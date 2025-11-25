-- 테스트 데이터 테이블 생성
CREATE TABLE IF NOT EXISTS test_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    data VARCHAR(255) NOT NULL,
    value INT NOT NULL
);

-- 테스트용 데이터 삽입 (1000개)
INSERT INTO test_data (data, value)
SELECT
    CONCAT('test-data-', seq),
    FLOOR(RAND() * 10000)
FROM (
    SELECT @row := @row + 1 AS seq
    FROM information_schema.TABLES t1,
         information_schema.TABLES t2,
         (SELECT @row := 0) r
    LIMIT 1000
) AS numbers;