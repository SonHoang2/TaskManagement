import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');

// Test configuration
export const options = {
    scenarios: {
        constant_load: {
            executor: 'constant-arrival-rate',
            rate: 1000, // 1000 requests per second
            timeUnit: '1s',
            duration: '10s', // Total 10000 requests (rate * duration)
            preAllocatedVUs: 50,
            maxVUs: 100,
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<500'], // 95% of requests must complete below 500ms
        errors: ['rate<0.1'], // Error rate must be less than 10%
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8765';
const PROJECT_ID = __ENV.PROJECT_ID || '00000000-0000-0000-0000-000000000001';
const USER_ID = __ENV.USER_ID || '00000000-0000-0000-0000-000000000001';
const BEARER_TOKEN = __ENV.BEARER_TOKEN || '';

const taskStatuses = ['TODO', 'IN_PROGRESS', 'DONE'];
const taskPriorities = ['LOW', 'MEDIUM', 'HIGH'];

function generateRandomTask() {
    const randomId = Math.floor(Math.random() * 1000000);
    return {
        projectId: PROJECT_ID,
        title: `Load Test Task ${randomId}`,
        description: `This is a load test task created at ${new Date().toISOString()}`,
        status: taskStatuses[Math.floor(Math.random() * taskStatuses.length)],
        priority: taskPriorities[Math.floor(Math.random() * taskPriorities.length)],
        assigneeId: null,
        reporterId: null,
        dueDate: null,
        startDate: null,
        parentTaskId: null,
    };
}

export default function () {
    const payload = JSON.stringify(generateRandomTask());
    const params = {
        headers: {
            'Content-Type': 'application/json',
            'X-User-Id': USER_ID,
            'Authorization': `Bearer ${BEARER_TOKEN}`,
        },
    };

    const res = http.post(`${BASE_URL}/task-service/tasks`, payload, params);

    const success = check(res, {
        'status is 201': (r) => r.status === 201,
        'has task response': (r) => r.json('data.task') !== undefined,
        'response time < 500ms': (r) => r.timings.duration < 500,
    });

    errorRate.add(!success);

    if (!success) {
        console.error(`Request failed with status ${res.status}: ${res.body}`);
    }

    sleep(Math.random() * 0.1); // Small random delay between requests
}
