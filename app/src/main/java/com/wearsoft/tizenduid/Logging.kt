package com.wearsoft.tizenduid

private val ENABLED = "true" == System.getenv("DADB_LOGGING")

internal fun log(block: () -> String) {
    if (ENABLED) {
        println(block())
    }
}