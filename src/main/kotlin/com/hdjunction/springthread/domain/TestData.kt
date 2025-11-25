package com.hdjunction.springthread.domain

import javax.persistence.*

@Entity
@Table(name = "test_data")
data class TestData(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val data: String,

    @Column(nullable = false)
    val value: Int
)