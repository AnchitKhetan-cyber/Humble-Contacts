package com.humblesolutions.humblecontacts.utils

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object GeminiError {

    fun getMessage(exception: Exception): String {

        return when (exception) {

            is UnknownHostException ->
                "No internet connection. Please connect to the internet and try again."

            is SocketTimeoutException ->
                "Request timed out. Please try again."

            is IOException ->
                "Network error. Please check your internet connection."

            else -> {

                val message = exception.message.orEmpty()

                when {

                    message.contains("Unable to resolve host", true) ||
                            message.contains("No address associated with hostname", true) ||
                            message.contains("Failed to connect", true) ->
                        "No internet connection. Please connect to the internet and try again."

                    message.contains("timeout", true) ->
                        "Request timed out. Please try again."

                    message.contains("429", true) ->
                        "Too many requests. Please wait a few moments."

                    message.contains("503", true) ->
                        "Gemini service is temporarily unavailable."

                    message.contains("quota", true) ->
                        "Gemini API quota exceeded."

                    message.contains("SSL", true) ->
                        "Secure connection failed. Check your internet."

                    else ->
                        "Couldn't extract details from the business card."
                }
            }
        }
    }
}