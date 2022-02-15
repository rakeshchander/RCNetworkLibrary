package io.github.rcnetworklibrary

class Greeting {
    fun greeting(): String {
        return "Hello, ${Platform().platform}!"
    }
}