
package com.example.rsocket2

data class Message(
    val stream: Boolean,
    val payload: String?,
    val index: Int = 0
)
