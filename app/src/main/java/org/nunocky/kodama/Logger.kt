package org.nunocky.kodama

class Logger {
    companion object {
        var isEnabled = true

        fun d(tag: String, message: String) {
            if (isEnabled) {
                android.util.Log.d(tag, message)
            }
        }

        fun e(tag: String, message: String, throwable: Throwable? = null) {
            if (isEnabled) {
                android.util.Log.e(tag, message, throwable)
            }
        }

        fun i(tag: String, message: String) {
            if (isEnabled) {
                android.util.Log.i(tag, message)
            }
        }

        fun w(tag: String, message: String) {
            if (isEnabled) {
                android.util.Log.w(tag, message)
            }
        }
    }
}