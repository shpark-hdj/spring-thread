import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const errorRate = new Rate('errors');

export const options = {
  stages: [
    { duration: '30s', target: 50 },   // Ramp up to 50 users over 30s
    { duration: '1m', target: 50 },    // Stay at 50 users for 1m
    { duration: '30s', target: 100 },  // Ramp up to 100 users over 30s
    { duration: '1m', target: 100 },   // Stay at 100 users for 1m
    { duration: '30s', target: 200 },  // Ramp up to 200 users over 30s
    { duration: '1m', target: 200 },   // Stay at 200 users for 1m
    { duration: '30s', target: 0 },    // Ramp down to 0 users
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'], // 95% of requests should be below 2s (httpbin 호출 포함)
    errors: ['rate<0.1'],               // Error rate should be less than 10%
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';

export default function () {
  const scenarios = [
    { name: 'DB Read', endpoint: '/api/load-test/io/db-read' },
    { name: 'DB Write', endpoint: '/api/load-test/io/db-write', method: 'POST' },
    { name: 'HTTP Call', endpoint: '/api/load-test/io/http-call?delaySeconds=1' },
  ];

  // Randomly select a scenario
  const scenario = scenarios[Math.floor(Math.random() * scenarios.length)];

  const params = {
    headers: { 'Content-Type': 'application/json' },
    tags: { name: scenario.name },
  };

  let res;
  if (scenario.method === 'POST') {
    res = http.post(`${BASE_URL}${scenario.endpoint}`, '', params);
  } else {
    res = http.get(`${BASE_URL}${scenario.endpoint}`, params);
  }

  const success = check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 2000ms': (r) => r.timings.duration < 2000,
  });

  errorRate.add(!success);

  sleep(1); // Wait 1s between requests
}