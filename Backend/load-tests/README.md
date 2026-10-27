# Load Tests

This directory contains k6 load tests for TaskManagementAPI services.

## Prerequisites

Install k6: https://k6.io/docs/getting-started/installation/

## Task Service Load Tests

### Create Task Load Test

Tests the task creation endpoint with 1000 continuous requests.

**Location:** `task-service/create-task-load-test.js`

**Configuration:**
- Rate: 100 requests/second
- Duration: 10 seconds
- Total requests: 1000
- Thresholds: 95% of requests < 500ms, error rate < 10%

**Configuration:**
Default values are hardcoded in the test file:
- `BASE_URL`: `http://localhost:8765`
- `PROJECT_ID`: `00000000-0000-0000-0000-000000000001`
- `USER_ID`: `00000000-0000-0000-0000-000000000001`

**Environment Variables (override defaults):**
- `BASE_URL`: Override API Gateway URL
- `PROJECT_ID`: Override project UUID
- `USER_ID`: Override user UUID

**Run the test:**
```bash
# With default settings
k6 run load-tests/task-service/create-task-load-test.js

# Override with environment variables
k6 run --env BEARER_TOKEN=your_token_here --env BASE_URL=http://localhost:8765 --env PROJECT_ID=your-project-id --env USER_ID=your-user-id load-tests/task-service/create-task-load-test.js

# With output file
k6 run --out json=results.json task-service/create-task-load-test.js
```

**View results:**
The test will output real-time statistics to the console. For detailed analysis, use the `--out json=results.json` flag and analyze the JSON output.

## Project Service Load Tests

### List Projects Load Test

Baseline for `GET /projects`, the hottest read path: every project on the page triggers Feign calls (owner and
5 members to user-service, stats to task-service). Run it before and after each caching change and compare.

**Location:** `project-service/list-projects-load-test.js`

**What it reports:**
- Client-side latency and error rate for the `measure` scenario (a 15s warm-up runs first and is reported separately)
- Internal calls per request, read from Prometheus: `user-service GET /users/{id}`, `task-service .../stats`,
  `project-service /memberships/me`. This is the number caching should bring down.
- `dropped_iterations` > 0 means the system could not keep up with `RATE`

**Setup (once):**
1. Start the stack with the gateway's rate limit relaxed. It is per IP and every virtual user comes from one IP, so it
   would answer 429 almost immediately. The services' `@RateLimit` (150 requests/minute per user) stays on: with
   1,000 users it only kicks in above ~2,500 requests/second.
   ```bash
   docker compose -f docker-compose.yml -f load-tests/docker-compose.loadtest.yml up -d --build
   ```
2. Generate the fake data (50,000 users in teams, ~16,000 projects, ~560,000 tasks, all linked) and load it. The
   loader truncates the tables first. See the docstring of `fake-data/generate_fake_data.py` for the data model.
   ```bash
   (cd fake-data && python3 generate_fake_data.py)
   ```
   ```bash
   bash fake-data/output/load_fake_data.sh
   ```

The script signs JWTs for fake-data users itself (user `i` has id `md5('fake-user-<i>')` and email
`user_<i>@example.com`), because signup/login allow only 5 requests per minute. It needs the gateway's `JWT_SECRET`
from `Backend/.env`.

**Environment Variables:**
- `JWT_SECRET` (required): same secret as api-gateway
- `BASE_URL`: API Gateway URL (default `http://localhost:8765`)
- `PROMETHEUS_URL`: default `http://localhost:9090`; internal call counts are skipped if unreachable
- `RATE`: requests per second in the `measure` scenario (default 20)
- `DURATION`: length of the `measure` scenario (default `1m`)
- `USERS`: requests pick a random user among the first `USERS` fake-data users (default 1000, at most `N_USERS`)

**Run the test:**
```bash
k6 run --env JWT_SECRET="$(grep ^JWT_SECRET= .env | cut -d= -f2-)" --summary-export=load-tests/results/projects-baseline.json load-tests/project-service/list-projects-load-test.js
```

Keep the app otherwise idle while it runs: the Prometheus counters include every request, not only k6's.
