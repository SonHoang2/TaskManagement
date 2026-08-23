import http from 'k6/http';
import { check } from 'k6';
import { Rate, Counter } from 'k6/metrics';

const errorRate = new Rate('errors');
const rateLimitHits = new Counter('rate_limit_hits');

export const options = {
  scenarios: {
    burst_test: {
      executor: 'constant-arrival-rate',
      rate: 50,
      timeUnit: '1s',
      duration: '10s',
      preAllocatedVUs: 10,
      maxVUs: 20,
    },
  },

  thresholds: {
    http_req_duration: ['p(95)<500'],
  },
};

const BASE_URL =
  __ENV.API_GATEWAY_URL ||
  __ENV.BASE_URL ||
  'http://localhost:8765';

const BEARER_TOKEN = __ENV.BEARER_TOKEN || '';

export default function () {
  const endpoint = '/project-service/api/v1/projects';

  const params = {
    headers: {
      'Content-Type': 'application/json',
      ...(BEARER_TOKEN && {
        Authorization: `Bearer ${BEARER_TOKEN}`,
      }),
    },
  };

  const res = http.get(`${BASE_URL}${endpoint}`, params);

  check(res, {
    'status is 200 or 429': (r) =>
      r.status === 200 || r.status === 429,
  });

  const isRealError =
    res.status !== 200 &&
    res.status !== 429;

  errorRate.add(isRealError);

  if (res.status === 429) {
    rateLimitHits.add(1);
  }

  if (isRealError) {
    console.error(
      `status=${res.status} error=${res.error} body=${
        res.body ? res.body.slice(0, 300) : ''
      }`
    );
  }
}