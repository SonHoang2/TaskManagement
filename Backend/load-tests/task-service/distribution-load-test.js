import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');

// Test configuration
// Dashboard endpoint -> không cần tải cực đoan, chỉ cần đủ để thấy xu hướng
// và xác nhận cải thiện trước/sau tối ưu (query + pool + index).
export const options = {
  scenarios: {
    ramp: {
      executor: 'ramping-arrival-rate',
      startRate: 5,
      timeUnit: '1s',
      preAllocatedVUs: 20,
      maxVUs: 60,
      stages: [
        { target: 10, duration: '20s' }, // baseline nhẹ
        { target: 20, duration: '20s' }, // tải vừa - mức thực tế dashboard hay gặp
        { target: 40, duration: '20s' }, // tải cao hơn bình thường, kiểm tra biên an toàn
        { target: 0,  duration: '10s' }, // hạ tải, quan sát hồi phục
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<500'],
    errors: ['rate<0.1'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8765';
const BEARER_TOKEN = __ENV.BEARER_TOKEN || '';

// Chỉ log 1 mẫu lỗi đầu tiên cho mỗi status code để tránh spam log
// (log toàn bộ res.body cho mỗi lỗi từng làm phình log lên hàng chục GB
// khi error rate cao trong lúc tìm breaking point).
const loggedStatuses = new Set();

export default function () {
  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${BEARER_TOKEN}`,
    },
  };

  const res = http.get(`${BASE_URL}/task-service/tasks/distribution`, params);

  const success = check(res, {
    'status is 200': (r) => r.status === 200,
    'has distribution response': (r) => r.json('data.distribution') !== undefined,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });

  errorRate.add(!success);

  if (!success && !loggedStatuses.has(res.status)) {
    loggedStatuses.add(res.status);
    // Chỉ log 300 ký tự đầu của body để tránh dump toàn bộ response dài
    console.error(`Sample failure status=${res.status} body=${res.body ? res.body.slice(0, 300) : ''}`);
  }

  sleep(Math.random() * 0.1); // Small random delay between requests
}