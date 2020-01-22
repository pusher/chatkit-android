package com.pusher.chatkit

class TestFileReader(private val path: String) {

    fun readTestFile(fileName: String): String = javaClass.getResource("$path/$fileName").readText()

}