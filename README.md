# Spring Thread Load Test

Spring Boot 애플리케이션의 스레드 성능을 k6를 사용하여 부하 테스트하는 프로젝트입니다.
IO 집약적 작업과 CPU 집약적 작업에 대한 Tomcat 스레드 풀의 성능을 측정합니다.

## 프로젝트 구조

```
spring-thread/
├── src/main/kotlin/com/hdjunction/springthread/
│   ├── config/
│   │   └── RestTemplateConfig.kt     # RestTemplate 설정
│   ├── controller/
│   │   └── LoadTestController.kt     # 부하 테스트용 API 엔드포인트
│   ├── domain/
│   │   └── TestData.kt                # 테스트 데이터 엔티티
│   └── repository/
│       └── TestDataRepository.kt      # 데이터 접근 레이어
├── k6-scripts/
│   ├── io-test.js                     # IO 집약적 작업 테스트
│   ├── cpu-test.js                    # CPU 집약적 작업 테스트
│   └── mixed-test.js                  # IO + CPU 혼합 테스트
└── init-data.sql                      # DB 초기 데이터 스크립트
```

## 사전 요구사항

- Java 11+
- Docker & Docker Compose
- k6 설치

### k6 설치

**macOS:**
```bash
brew install k6
```

**Linux:**
```bash
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6
```

**Windows:**
```bash
choco install k6
```

## 실행 방법

### 1. MySQL 데이터베이스 시작

데이터베이스를 시작하면 init-data.sql이 자동으로 실행됩니다.

```bash
docker-compose up -d
```

### 2. Spring Boot 애플리케이션 실행

```bash
./gradlew bootRun
```

애플리케이션은 `http://localhost:8081`에서 실행됩니다.

### 3. 헬스체크

```bash
curl http://localhost:8081/api/load-test/health
```

## API 엔드포인트

### IO 집약적 작업

- `GET /api/load-test/io/db-read` - DB에서 100개 레코드 조회
- `POST /api/load-test/io/db-write` - DB에 10개 레코드 저장
- `GET /api/load-test/io/http-call?delaySeconds=1` - 외부 HTTP 호출 (httpbin.org 사용)

### CPU 집약적 작업

- `GET /api/load-test/cpu/hash?iterations=1000` - SHA-256 해시 반복 계산

### 혼합 작업

- `GET /api/load-test/mixed` - DB 조회 후 해시 계산

### 헬스체크

- `GET /api/load-test/health` - 상태 확인

## k6 부하 테스트 실행

### 1. IO 테스트

DB 조회/쓰기와 외부 HTTP 호출을 사용한 IO 집약적 작업 테스트

```bash
k6 run k6-scripts/io-test.js
```

**특징:**
- 50 → 100 → 200명의 가상 사용자로 단계적 증가
- IO 대기 시간이 많아 스레드가 블로킹됨
- 스레드 풀이 고갈될 수 있는 시나리오

### 2. CPU 테스트

SHA-256 해시 계산을 사용한 CPU 집약적 작업 테스트

```bash
k6 run k6-scripts/cpu-test.js
```

**특징:**
- 20 → 50 → 100명의 가상 사용자로 단계적 증가
- CPU를 많이 사용하여 응답 시간이 길어질 수 있음
- CPU 코어 수에 따라 성능이 달라짐

### 3. 혼합 테스트

IO와 CPU 작업이 혼합된 실제 시나리오

```bash
k6 run k6-scripts/mixed-test.js
```

**특징:**
- 30 → 60 → 100명의 가상 사용자로 단계적 증가
- 실제 애플리케이션과 유사한 워크로드

### 커스텀 설정으로 실행

```bash
# 다른 URL 지정
k6 run -e BASE_URL=http://192.168.1.100:8081 k6-scripts/io-test.js

# 더 많은 가상 사용자로 테스트
k6 run --vus 500 --duration 1m k6-scripts/io-test.js
```

## Tomcat 스레드 풀 설정

`src/main/resources/application.yml`에서 설정 가능:

```yaml
server:
  tomcat:
    threads:
      max: 200          # 최대 스레드 수
      min-spare: 10     # 최소 유지 스레드 수
    max-connections: 8192  # 최대 연결 수
    accept-count: 100      # 대기 큐 크기
```

### 스레드 풀 설정 변경 실험

다양한 설정으로 테스트하여 최적의 값을 찾을 수 있습니다:

**예시 1: 작은 스레드 풀**
```yaml
server:
  tomcat:
    threads:
      max: 50
      min-spare: 5
```

**예시 2: 큰 스레드 풀**
```yaml
server:
  tomcat:
    threads:
      max: 500
      min-spare: 50
```

설정 변경 후 애플리케이션을 재시작하고 동일한 k6 테스트를 실행하여 비교합니다.

## 결과 분석

k6는 테스트 완료 후 다음 메트릭을 제공합니다:

```
✓ status is 200
✓ response time < 500ms

checks.........................: 95.00% ✓ 9500  ✗ 500
data_received..................: 1.2 MB 20 kB/s
data_sent......................: 890 kB 15 kB/s
http_req_blocked...............: avg=1.5ms    min=0s      med=1ms     max=50ms    p(90)=2ms    p(95)=3ms
http_req_connecting............: avg=1ms      min=0s      med=0.5ms   max=40ms    p(90)=1.5ms  p(95)=2ms
http_req_duration..............: avg=150ms    min=10ms    med=100ms   max=2s      p(90)=300ms  p(95)=500ms
http_req_receiving.............: avg=1ms      min=0s      med=0.5ms   max=10ms    p(90)=2ms    p(95)=3ms
http_req_sending...............: avg=0.5ms    min=0s      med=0.2ms   max=5ms     p(90)=1ms    p(95)=1.5ms
http_req_waiting...............: avg=148.5ms  min=9ms     med=99ms    max=1.99s   p(90)=298ms  p(95)=498ms
http_reqs......................: 10000  166.666667/s
iteration_duration.............: avg=1.15s    min=1.01s   med=1.1s    max=3s      p(90)=1.3s   p(95)=1.5s
iterations.....................: 10000  166.666667/s
vus............................: 100    min=10  max=200
vus_max........................: 200    min=200 max=200
```

### 주요 메트릭 설명

- **http_req_duration**: 요청 처리 시간 (p95가 중요)
- **http_reqs**: 초당 처리된 요청 수 (처리량)
- **checks**: 성공률 (목표: 90% 이상)
- **vus**: 가상 사용자 수

## 모니터링

부하 테스트 중 다음을 모니터링하는 것이 좋습니다:

### JVM 메트릭
```bash
# JConsole 또는 VisualVM 사용
jconsole
```

### 스레드 덤프
```bash
# 실행 중인 스레드 확인
jstack <PID>
```

### 시스템 리소스
```bash
# CPU 사용률
top

# 메모리 사용량
free -h
```

## 실험 시나리오

### 실험 1: 스레드 풀 크기별 성능 비교

1. `max: 50`으로 설정 후 io-test.js 실행
2. `max: 200`으로 설정 후 io-test.js 실행
3. `max: 500`으로 설정 후 io-test.js 실행
4. p95 응답 시간과 처리량 비교

### 실험 2: IO vs CPU 작업 비교

1. io-test.js 실행 - IO 워크로드 결과 기록
2. cpu-test.js 실행 - CPU 워크로드 결과 기록
3. 동일한 VUS에서 응답 시간 비교
4. 스레드 블로킹 패턴 분석

## 트러블슈팅

### 연결 거부 에러
```
connection refused
```
- 애플리케이션이 실행 중인지 확인
- 포트 8081이 열려있는지 확인

### DB 연결 실패
```
Unable to acquire JDBC Connection
```
- MySQL 컨테이너가 실행 중인지 확인: `docker ps`
- 데이터베이스 초기화 완료 확인

### 높은 에러율
- 스레드 풀 크기 증가
- DB 커넥션 풀 설정 확인
- 시스템 리소스 확인 (CPU, 메모리)

## 참고 자료

- [k6 Documentation](https://k6.io/docs/)
- [Spring Boot Tomcat Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html#application-properties.server)
- [Load Testing Best Practices](https://k6.io/docs/testing-guides/test-types/)