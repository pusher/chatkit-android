package com.pusher.chatkit

class TestFileReader(private val path: String) {

    fun readTestFile(filename: String): String = javaClass.getResource("$path/$filename").readText()
}
