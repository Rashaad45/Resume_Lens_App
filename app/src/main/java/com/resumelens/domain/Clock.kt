package com.resumelens.domain

/** Abstracts "now" so timestamps are deterministic in tests. */
fun interface Clock {
    fun nowMillis(): Long
}
