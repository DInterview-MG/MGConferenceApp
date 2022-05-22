package org.mg.conferenceapp

// Ensures that the contained value is only accessed on the original thread.
class SingleThreadVar<E>(initialValue: E) {

    private val thread = Thread.currentThread()
    private var value: E = initialValue

    fun get(): E {
        checkThread()
        return value
    }

    fun set(newValue: E) {
        checkThread()
        value = newValue
    }

    private fun checkThread() {
        if(thread != Thread.currentThread()) {
            throw RuntimeException("Accessing variable from wrong thread")
        }
    }
}