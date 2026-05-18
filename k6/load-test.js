import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

// Custom metrics
const readLatency = new Trend('read_latency');
const writeLatency = new Trend('write_latency');
const asyncLatency = new Trend('async_latency');
const errorRate = new Rate('error_rate');

// SLO Definitions & Thresholds
export const options = {
  scenarios: {
    // 1. Sustained Read Load (Authentication & Fetch Projects)
    sustained_read: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 50 }, // Ramp up
        { duration: '1m', target: 50 },  // Sustain
        { duration: '30s', target: 0 },  // Ramp down
      ],
      exec: 'readScenario',
    },
    // 2. Stress Write Load (Concurrency via bulk task creation)
    stress_write: {
      executor: 'constant-arrival-rate',
      rate: 20, // 20 iterations per second
      timeUnit: '1s',
      duration: '1m',
      preAllocatedVUs: 50,
      maxVUs: 100,
      exec: 'writeScenario',
      startTime: '30s', // Start after reads have ramped up
    },
    // 3. Spike Load (Async Job Processing via RabbitMQ)
    spike_async: {
      executor: 'ramping-arrival-rate',
      startRate: 5,
      timeUnit: '1s',
      preAllocatedVUs: 50,
      maxVUs: 200,
      stages: [
        { duration: '10s', target: 100 }, // Huge spike
        { duration: '20s', target: 100 }, // Hold spike
        { duration: '10s', target: 0 },   // Cool down
      ],
      exec: 'asyncScenario',
      startTime: '1m', // Hit during peak sustained load
    },
  },
  thresholds: {
    // Latency: p(95) < 200ms for standard read operations
    'read_latency': ['p(95)<200'],
    
    // Overall Error Rate: < 1% of all total requests resulting in server errors (5xx)
    'error_rate': ['rate<0.01'],
    
    // Additional thresholds
    'http_req_failed': ['rate<0.01'], // Global HTTP failure rate
    'write_latency': ['p(95)<500'],   // Writes can be slightly slower
  },
};

const BASE_URL = __ENV.TARGET_URL || 'http://localhost:8080';

const TENANTS = [
  { email: 'admin@tenant1.com', password: '123456', tenantId: '1' },
  { email: 'admin@tenant2.com', password: '123456', tenantId: '2' },
];

function getTenant() {
  return TENANTS[Math.floor(Math.random() * TENANTS.length)];
}

function authenticate(tenant) {
  const payload = JSON.stringify({
    email: tenant.email,
    password: tenant.password,
  });

  const params = {
    headers: {
      'Host': 'api.workhub.local',
      'Content-Type': 'application/json',
      'X-Tenant-ID': tenant.tenantId, // Custom header for multi-tenancy
    },
  };

  // Mocking auth endpoint
  const res = http.post(`${BASE_URL}/auth/login`, payload, params);
  
  // Track error rate (5xx)
  if (res.status >= 500) {
    errorRate.add(1);
  }

  // Assuming response contains JWT token
  return res.json('token') || 'mock-jwt-token';
}

export function readScenario() {
  const tenant = getTenant();
  const token = authenticate(tenant);

  group('Fetch Projects', () => {
    const params = {
      headers: {
        'Host': 'api.workhub.local',
        'Authorization': `Bearer ${token}`,
        'X-Tenant-ID': tenant.tenantId,
      },
    };

    const res = http.get(`${BASE_URL}/projects`, params);
    readLatency.add(res.timings.duration);
    
    if (res.status >= 500) errorRate.add(1);

    check(res, {
      'is status 200': (r) => r.status === 200,
    });
  });

  sleep(1);
}

export function writeScenario() {
  const tenant = getTenant();
  const token = authenticate(tenant);
  
  // First get projects or use a known Project ID
  const projectId = Math.floor(Math.random() * 10) + 1; // Mock project ID

  group('Create Tasks', () => {
    const payload = JSON.stringify({
      title: `Task-${__VU}-${__ITER}`,
      description: 'Stress test task creation to verify DB locks',
      status: 'TODO'
    });

    const params = {
      headers: {
        'Host': 'api.workhub.local',
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
        'X-Tenant-ID': tenant.tenantId,
      },
    };

    const res = http.post(`${BASE_URL}/projects/${projectId}/tasks`, payload, params);
    writeLatency.add(res.timings.duration);
    
    if (res.status >= 500) errorRate.add(1);

    check(res, {
      'is status 201': (r) => r.status === 201,
    });
  });
}

export function asyncScenario() {
  const tenant = getTenant();
  const token = authenticate(tenant);
  const projectId = Math.floor(Math.random() * 10) + 1;

  group('Trigger Async Report', () => {
    const params = {
      headers: {
        'Host': 'api.workhub.local',
        'Authorization': `Bearer ${token}`,
        'X-Tenant-ID': tenant.tenantId,
      },
    };

    const res = http.post(`${BASE_URL}/projects/${projectId}/generate-report`, null, params);
    asyncLatency.add(res.timings.duration);
    
    if (res.status >= 500) errorRate.add(1);

    check(res, {
      'is status 202': (r) => r.status === 202, // 202 Accepted for async
    });
  });
}
