import http from 'k6/http';
import crypto from 'k6/crypto';
import encoding from 'k6/encoding';
import { check, sleep } from 'k6';

// Baseline for GET /projects, the hottest endpoint, before adding the Redis cache.
// Every project on a page triggers several Feign calls: owner + 5 members to user-service, stats to
// task-service. The script measures client-side latency and, via Prometheus, the number of internal calls per
// request: that is the number the caching steps have to bring down.
//
// Before running: load the data from fake-data/, start the stack with docker-compose.loadtest.yml (relaxes the
// gateway's per-IP rate limit), and pass the same JWT_SECRET as api-gateway. See load-tests/README.md.

const BASE_URL = __ENV.API_GATEWAY_URL || __ENV.BASE_URL || 'http://localhost:8765';
const PROMETHEUS_URL = __ENV.PROMETHEUS_URL || 'http://localhost:9090';
const JWT_SECRET = __ENV.JWT_SECRET || '';
// Uses fake-data users 0..USERS-1; must not exceed N_USERS used when generating the data
const USERS = parseInt(__ENV.USERS || '1000', 10);
const RATE = parseInt(__ENV.RATE || '20', 10);
const DURATION = __ENV.DURATION || '1m';
// Prometheus scrapes every 15s: wait past one scrape so the counters include the last requests
const SCRAPE_WAIT_SECONDS = parseInt(__ENV.SCRAPE_WAIT_SECONDS || '20', 10);

const PROJECTS_URL = `${BASE_URL}/project-service/api/v1/projects`;

export const options = {
  scenarios: {
    // The first requests hit a cold JVM and connection pool: keep them separate so they don't skew the numbers
    warmup: {
      executor: 'constant-arrival-rate',
      rate: Math.max(1, Math.floor(RATE / 4)),
      timeUnit: '1s',
      duration: '15s',
      preAllocatedVUs: 10,
      maxVUs: 50,
    },
    // Fix requests/second rather than the number of VUs: the system takes the same load before and after caching,
    // so any latency difference comes from the cache. dropped_iterations > 0 means the system can't keep up with RATE.
    measure: {
      executor: 'constant-arrival-rate',
      rate: RATE,
      timeUnit: '1s',
      duration: DURATION,
      startTime: '15s',
      preAllocatedVUs: 50,
      maxVUs: 300,
    },
  },
  // Only the measure scenario counts; these lines show up separately in the summary for comparing runs
  thresholds: {
    'http_req_duration{scenario:measure}': ['p(95)<2000'],
    'http_req_failed{scenario:measure}': ['rate<0.01'],
  },
  setupTimeout: '30s',
  teardownTimeout: `${SCRAPE_WAIT_SECONDS + 30}s`,
};

// Cumulative request counters for each internal endpoint. [{]id[}] instead of \{id\} to avoid double escaping.
const COUNTERS = {
  projectList: 'sum(http_server_requests_seconds_count{service="project-service", method="GET", uri=~".*/projects"})',
  userLookup: 'sum(http_server_requests_seconds_count{service="user-service", method="GET", uri=~".*/users/[{]id[}]"})',
  taskStats: 'sum(http_server_requests_seconds_count{service="task-service", method="GET", uri=~".*/stats"})',
  membershipLookup:
    'sum(http_server_requests_seconds_count{service="project-service", method="GET", uri=~".*/memberships/me"})',
};

function b64url(obj) {
  return encoding.b64encode(JSON.stringify(obj), 'rawurl');
}

// Same format as user-service's JwtService (HS256, sub = email, userId claim).
// Signed locally instead of calling /auth/signup or /auth/login: those endpoints allow only 5 requests/minute per IP.
function signToken(userId, email) {
  const now = Math.floor(Date.now() / 1000);
  const unsigned = `${b64url({ alg: 'HS256', typ: 'JWT' })}.${b64url({ sub: email, userId, iat: now, exp: now + 3600 })}`;
  return `${unsigned}.${crypto.hmac('sha256', JWT_SECRET, unsigned, 'base64rawurl')}`;
}

// Same as user_id(i) in fake-data/generate_fake_data.py: UUID from md5('fake-user-<i>')
function fakeUserId(n) {
  const h = crypto.md5(`fake-user-${n}`, 'hex');
  return `${h.slice(0, 8)}-${h.slice(8, 12)}-${h.slice(12, 16)}-${h.slice(16, 20)}-${h.slice(20)}`;
}

// Tokens are signed on demand and cached per VU, so a large USERS doesn't bloat setup()
const tokens = {};

function tokenFor(n) {
  if (!tokens[n]) {
    tokens[n] = signToken(fakeUserId(n), `user_${n}@example.com`);
  }
  return tokens[n];
}

function authParams(token) {
  return { headers: { Authorization: `Bearer ${token}` } };
}

// null when Prometheus can't be read; the internal call count is then skipped
function readCounters() {
  const counters = {};
  for (const [name, query] of Object.entries(COUNTERS)) {
    const res = http.get(`${PROMETHEUS_URL}/api/v1/query?query=${encodeURIComponent(query)}`, {
      tags: { name: 'prometheus' },
    });
    if (res.status !== 200) {
      return null;
    }
    const result = res.json('data.result');
    counters[name] = result.length ? parseFloat(result[0].value[1]) : 0;
  }
  return counters;
}

export function setup() {
  if (!JWT_SECRET) {
    throw new Error('Thiếu JWT_SECRET: lấy JWT_SECRET trong Backend/.env (phải giống secret của api-gateway)');
  }

  // Read the counters before the probe request, so the probe is in both the numerator and the denominator
  const before = readCounters();

  // Wrong secret, missing data or rate limit still on: stop now instead of running a full minute of errors
  const probe = http.get(PROJECTS_URL, authParams(tokenFor(0)));
  if (probe.status !== 200) {
    throw new Error(`Request thử trả về ${probe.status}: ${probe.body}`);
  }
  if (probe.json('data.totalElements') === 0) {
    throw new Error('User 0 không có project nào: nạp dữ liệu từ fake-data/ trước (xem load-tests/README.md)');
  }

  return { before };
}

export default function () {
  // Each request picks a random user: a per-user cache only hits when that same user comes back
  const res = http.get(PROJECTS_URL, authParams(tokenFor(Math.floor(Math.random() * USERS))));

  check(res, {
    'status 200': (r) => r.status === 200,
    'trang có project': (r) => r.status === 200 && r.json('data.content.#') > 0,
  });
}

export function teardown(data) {
  if (!data.before) {
    console.warn(`Không đọc được Prometheus ở ${PROMETHEUS_URL}: bỏ qua phần đếm lời gọi nội bộ`);
    return;
  }

  sleep(SCRAPE_WAIT_SECONDS);
  const after = readCounters();
  if (!after) {
    console.warn(`Không đọc được Prometheus ở ${PROMETHEUS_URL} sau khi chạy`);
    return;
  }

  const requests = after.projectList - data.before.projectList;
  if (requests <= 0) {
    console.warn('Prometheus chưa ghi nhận request GET /projects nào; tăng SCRAPE_WAIT_SECONDS rồi chạy lại');
    return;
  }
  const perRequest = (name) => ((after[name] - data.before[name]) / requests).toFixed(2);

  console.log(`Lời gọi nội bộ trên mỗi GET /projects (${requests} request, theo Prometheus):`);
  console.log(`  user-service    GET /users/{id}        ${perRequest('userLookup')}`);
  console.log(`  task-service    GET .../stats          ${perRequest('taskStats')}`);
  console.log(`  project-service GET /memberships/me    ${perRequest('membershipLookup')}`);
}
