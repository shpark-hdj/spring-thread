package com.hdjunction.springthread.controller

import com.hdjunction.springthread.domain.TestData
import com.hdjunction.springthread.repository.TestDataRepository
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestTemplate
import java.security.MessageDigest
import kotlin.random.Random

@RestController
@RequestMapping("/api/load-test")
class LoadTestController(
    private val testDataRepository: TestDataRepository,
    private val restTemplate: RestTemplate
) {

    /**
     * IO 집약적 작업 - DB 조회
     * DB에서 데이터를 읽어오는 작업으로 IO 대기 시간이 발생
     */
    @GetMapping("/io/db-read")
    fun ioDbRead(): Map<String, Any> {
        val data = testDataRepository.findTop100ByOrderByIdDesc()
        return mapOf(
            "type" to "IO",
            "operation" to "DB Read",
            "count" to data.size,
            "sum" to data.sumOf { it.value }
        )
    }

    /**
     * IO 집약적 작업 - DB 쓰기
     * DB에 데이터를 저장하는 작업
     */
    @PostMapping("/io/db-write")
    fun ioDbWrite(): Map<String, Any> {
        val entities = (1..10).map {
            TestData(
                data = "test-${System.currentTimeMillis()}-$it",
                value = Random.nextInt(1000)
            )
        }
        val saved = testDataRepository.saveAll(entities)
        return mapOf(
            "type" to "IO",
            "operation" to "DB Write",
            "saved" to saved.size
        )
    }

    /**
     * IO 집약적 작업 - 외부 HTTP 호출 (httpbin)
     * 실제 네트워크 IO 대기가 발생하는 작업
     */
    @GetMapping("/io/http-call")
    fun ioHttpCall(@RequestParam(defaultValue = "1") delaySeconds: Int): Map<String, Any> {
        val startTime = System.currentTimeMillis()

        // httpbin.org의 delay 엔드포인트 호출 (실제 네트워크 IO)
        val url = "https://httpbin.org/delay/$delaySeconds"
        val response = try {
            restTemplate.getForObject(url, String::class.java)
            "success"
        } catch (e: Exception) {
            "error: ${e.message}"
        }

        val endTime = System.currentTimeMillis()

        return mapOf(
            "type" to "IO",
            "operation" to "HTTP Call (httpbin)",
            "url" to url,
            "delaySeconds" to delaySeconds,
            "actualMs" to (endTime - startTime),
            "response" to response,
            "thread" to Thread.currentThread().name
        )
    }

    /**
     * CPU 집약적 작업 - 해시 계산
     * 많은 CPU 사이클을 소모하는 작업
     */
    @GetMapping("/cpu/hash")
    fun cpuHash(@RequestParam(defaultValue = "1000") iterations: Int): Map<String, Any> {
        val startTime = System.currentTimeMillis()
        val messageDigest = MessageDigest.getInstance("SHA-256")
        var result = "initial-data"

        repeat(iterations) {
            result = messageDigest.digest(result.toByteArray())
                .joinToString("") { "%02x".format(it) }
        }

        val endTime = System.currentTimeMillis()

        return mapOf(
            "type" to "CPU",
            "operation" to "Hash Calculation",
            "iterations" to iterations,
            "durationMs" to (endTime - startTime),
            "resultLength" to result.length,
            "thread" to Thread.currentThread().name
        )
    }

    /**
     * 혼합 작업 - IO + CPU
     * DB 조회 후 CPU 집약적 처리
     */
    @GetMapping("/mixed")
    fun mixed(): Map<String, Any> {
        val startTime = System.currentTimeMillis()

        // IO 작업: DB 조회
        val data = testDataRepository.findTop100ByOrderByIdDesc()

        // CPU 작업: 데이터 처리
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val processed = data.map { item ->
            val hash = messageDigest.digest(item.data.toByteArray())
                .joinToString("") { "%02x".format(it) }
            hash.take(16)
        }

        val endTime = System.currentTimeMillis()

        return mapOf(
            "type" to "MIXED",
            "operation" to "DB Read + Hash",
            "dbCount" to data.size,
            "processedCount" to processed.size,
            "durationMs" to (endTime - startTime),
            "thread" to Thread.currentThread().name
        )
    }

    /**
     * 헬스체크 엔드포인트
     */
    @GetMapping("/health")
    fun health(): Map<String, Any> {
        return mapOf(
            "status" to "OK",
            "timestamp" to System.currentTimeMillis(),
            "thread" to Thread.currentThread().name
        )
    }
}
