import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const errorRate = new Rate('errors');

export const options = {
  stages: [
    { duration: '30s', target: 20 },   // Ramp up to 20 users over 30s
    { duration: '1m', target: 20 },    // Stay at 20 users for 1m
    { duration: '30s', target: 50 },   // Ramp up to 50 users over 30s
    { duration: '1m', target: 50 },    // Stay at 50 users for 1m
    { duration: '30s', target: 100 },  // Ramp up to 100 users over 30s
    { duration: '1m', target: 100 },   // Stay at 100 users for 1m
    { duration: '30s', target: 0 },    // Ramp down to 0 users
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'], // 95% of requests should be below 2s
    errors: ['rate<0.1'],               // Error rate should be less than 10%
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';

export default function () {
  const params = {
    headers: { 'Content-Type': 'application/json' },
    tags: { name: 'Hash Calculation' },
  };

  const res = http.get(`${BASE_URL}/api/load-test/cpu/hash?iterations=1000`, params);

  const success = check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 2000ms': (r) => r.timings.duration < 2000,
  });

  errorRate.add(!success);

  sleep(1); // Wait 1s between requests
}