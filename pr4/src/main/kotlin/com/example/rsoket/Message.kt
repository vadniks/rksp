
package com.example.rsoket

data class Message(
    val stream: Boolean,
    val payload: String,
    val index: Int = 0
)
