
package com.example.rsocket

data class Message(
    val stream: Boolean,
    val payload: String,
    val index: Int = 0
)
