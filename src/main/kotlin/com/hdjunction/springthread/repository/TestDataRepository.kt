package com.hdjunction.springthread.repository

import com.hdjunction.springthread.domain.TestData
import org.springframework.data.jpa.repository.JpaRepository

interface TestDataRepository : JpaRepository<TestData, Long> {

    fun findTop100ByOrderByIdDesc(): List<TestData>
}
