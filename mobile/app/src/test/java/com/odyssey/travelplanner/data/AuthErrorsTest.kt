package com.odyssey.travelplanner.data

import io.github.jan.supabase.auth.exception.AuthRestException
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertIs

class AuthErrorsTest {
    @Test
    fun invalidCredentialsAreClassifiedFromSupabaseAuthError() {
        val error = AuthRestException("Invalid login credentials", "invalid_credentials", 400)

        assertIs<AuthFailure.InvalidCredentials>(classifyAuthFailure(error))
    }

    @Test
    fun networkErrorsAreClassifiedSeparately() {
        assertIs<AuthFailure.Network>(classifyAuthFailure(IOException("offline")))
    }
}
