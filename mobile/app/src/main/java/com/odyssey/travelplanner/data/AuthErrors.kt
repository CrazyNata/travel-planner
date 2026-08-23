package com.odyssey.travelplanner.data

import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import java.io.IOException

sealed class AuthFailure(open val cause: Throwable) {
    class InvalidCredentials(override val cause: Throwable) : AuthFailure(cause)
    class Network(override val cause: Throwable) : AuthFailure(cause)
    class Unknown(override val cause: Throwable) : AuthFailure(cause)
}

fun classifyAuthFailure(error: Throwable): AuthFailure {
    val chain = generateSequence(error) { it.cause }.toList()
    val authError = chain.filterIsInstance<AuthRestException>().firstOrNull()
    val normalizedMessage = chain.joinToString(" ") { it.message.orEmpty() }.lowercase()
    return when {
        authError?.errorCode == AuthErrorCode.InvalidCredentials ||
            normalizedMessage.contains("invalid login credentials") ||
            normalizedMessage.contains("invalid_credentials") -> AuthFailure.InvalidCredentials(error)

        authError?.errorCode == AuthErrorCode.RequestTimeout ||
            authError?.statusCode?.let { it in 500..599 } == true ||
            chain.any { it is IOException } -> AuthFailure.Network(error)
        else -> AuthFailure.Unknown(error)
    }
}
