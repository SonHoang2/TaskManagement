import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Counter } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const rateLimitHits = new Counter('rate_limit_hits');

// Test configuration
// Mục tiêu: gửi vượt replenishRate + burstCapacity để buộc RequestRateLimiter
// trả 429. Dùng 'constant-arrival-rate' thay vì ramping VU để kiểm soát
// chính xác số request/giây bất kể response time — quan trọng vì rate
// limiter tính theo request/giây thực tế đến Gateway, không phải theo VU.
export const options = {
  scenarios: {
    burst_test: {
      executor: 'constant-arrival-rate',
      rate: 10,              // 10 req/giây — vượt xa replenishRate=2 (nếu đã sửa theo 100 req/phút)
      timeUnit: '1s',
      duration: '3s',
      preAllocatedVUs: 20,
      maxVUs: 50,
    },
  },
  thresholds: {
    // Không set 'errors < X%' vì 429 CHÍNH LÀ kết quả mong đợi, không phải lỗi thật
    http_req_duration: ['p(95)<500'],
  },
};

const BASE_URL = __ENV.API_GATEWAY_URL || __ENV.BASE_URL || 'http://localhost:8765';
const BEARER_TOKEN = __ENV.BEARER_TOKEN || '';

// Chỉ log 1 mẫu lỗi đầu tiên cho mỗi status code để tránh spam log
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

  // status ngoài 200/429 (404, 500, 503, 0...) mới tính là lỗi thật sự
  const isRealError = res.status !== 200 && res.status !== 429;
  errorRate.add(isRealError);

  if (res.status === 429) {
    rateLimitHits.add(1);
  }

  if (isRealError && !loggedStatuses.has(res.status)) {
    loggedStatuses.add(res.status);
    console.error(`Unexpected status=${res.status} endpoint=${endpoint} body=${res.body ? res.body.slice(0, 300) : ''}`);
  }

  // Không sleep hoặc sleep rất ngắn để duy trì áp lực đủ vượt bucket
  sleep(0.02);
}