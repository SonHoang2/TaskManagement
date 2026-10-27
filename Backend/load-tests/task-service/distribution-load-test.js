import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Counter } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const rateLimitHits = new Counter('rate_limit_hits');

// Test configuration
// Goal: send more than replenishRate + burstCapacity to force RequestRateLimiter
// to return 429. Uses 'constant-arrival-rate' instead of ramping VUs to control
// requests/second exactly regardless of response time — this matters because the
// rate limiter counts actual requests/second reaching the Gateway, not VUs.
export const options = {
  scenarios: {
    burst_test: {
      executor: 'constant-arrival-rate',
      rate: 10,              // 10 req/s — far above replenishRate=2 (if set to 100 req/minute)
      timeUnit: '1s',
      duration: '3s',
      preAllocatedVUs: 20,
      maxVUs: 50,
    },
  },
  thresholds: {
    // No 'errors < X%' threshold because 429 IS the expected result, not a real error
    http_req_duration: ['p(95)<500'],
  },
};

const BASE_URL = __ENV.API_GATEWAY_URL || __ENV.BASE_URL || 'http://localhost:8765';
const BEARER_TOKEN = __ENV.BEARER_TOKEN || '';

// Log only the first error sample per status code to avoid spamming the log
const loggedStatuses = new Set();

const endpoints = [
  '/task-service/api/v1/tasks/distribution',
];

export default function () {
  const endpoint = endpoints[Math.floor(Math.random() * endpoints.length)];

  const params = {
    headers: {
      'Content-Type': 'application/json',
      ...(BEARER_TOKEN && { Authorization: `Bearer ${BEARER_TOKEN}` }),
    },
  };

  const res = http.get(`${BASE_URL}${endpoint}`, params);

  const ok = check(res, {
    'status is 200 or 429': (r) => r.status === 200 || r.status === 429,
    'rate limit triggered (429)': (r) => r.status === 429,
    'request allowed (200)': (r) => r.status === 200,
  });

  // Only statuses other than 200/429 (404, 500, 503, 0...) count as real errors
  const isRealError = res.status !== 200 && res.status !== 429;
  errorRate.add(isRealError);

  if (res.status === 429) {
    rateLimitHits.add(1);
  }

  if (isRealError && !loggedStatuses.has(res.status)) {
    loggedStatuses.add(res.status);
    console.error(`Unexpected status=${res.status} endpoint=${endpoint} body=${res.body ? res.body.slice(0, 300) : ''}`);
  }

  // No sleep, or a very short one, to keep enough pressure to overflow the bucket
  sleep(0.02);
}