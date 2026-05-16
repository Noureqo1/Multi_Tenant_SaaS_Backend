import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  stages: [
    { duration: "10s", target: 5 },  // Ramp-up
    { duration: "20s", target: 10 }, // Steady load
    { duration: "10s", target: 0 },  // Ramp-down
  ],
  thresholds: {
    http_req_failed: ["rate<0.05"],
    http_req_duration: ["p(95)<1000"],
  },
};

const BASE_URL = "http://host.docker.internal:8082";

export default function () {
  // Step 1: Login
  const loginRes = http.post(
    `${BASE_URL}/auth/login`,
    JSON.stringify({
      email: "admin@tenant-a.com",
      password: "password",
    }),
    {
      headers: {
        "Content-Type": "application/json",
      },
    }
  );

  check(loginRes, {
    "Login returned HTTP 200": (r) => r.status === 200,
    "Login returned a token": (r) => {
      const body = r.json();
      return body && body.token !== undefined && body.token !== null;
    },
  });

  const token = loginRes.json("token");

  if (!token) {
    return;
  }

  const authHeaders = {
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
  };

  sleep(0.5);

  // Step 2: Get Projects
  const getProjectsRes = http.get(`${BASE_URL}/projects`, authHeaders);

  check(getProjectsRes, {
    "Get projects returned HTTP 200": (r) => r.status === 200,
  });

  sleep(0.5);

  // Step 3: Create a Project
  const createProjectRes = http.post(
    `${BASE_URL}/projects`,
    JSON.stringify({
      name: `Load Test Project ${Date.now()}`,
      description: "Created during k6 load test",
    }),
    authHeaders
  );

  check(createProjectRes, {
    "Create project returned HTTP 201": (r) => r.status === 201,
    "Create project returned an ID": (r) => {
      const body = r.json();
      return body && body.id !== undefined && body.id !== null;
    },
  });

  const projectId = createProjectRes.json("id");

  if (!projectId) {
    return;
  }

  sleep(0.5);

  // Step 4: Generate Report
  const generateReportRes = http.post(
    `${BASE_URL}/projects/${projectId}/generate-report`,
    null,
    authHeaders
  );

  check(generateReportRes, {
    "Generate report returned HTTP 202": (r) => r.status === 202,
    "Generate report returned a job ID": (r) => {
      const body = r.json();
      return body && body.jobId !== undefined && body.jobId !== null;
    },
  });

  sleep(1);
}