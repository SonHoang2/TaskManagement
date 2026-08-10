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
