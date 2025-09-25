package com.example.finazas.ui.profile

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import org.json.JSONObject
import java.util.Base64

data class GoogleProfile(
    val sub: String,
    val givenName: String?,
    val familyName: String?,
    val email: String,
    val picture: String?
)

@RequiresApi(Build.VERSION_CODES.O)
suspend fun getGoogleProfileViaCredentialManager(context: Context): GoogleProfile? {
    val serverClientId = context.getString(com.example.finazas.R.string.default_web_client_id)

    val googleIdOption = GetGoogleIdOption.Builder()
        .setServerClientId(serverClientId)
        .setFilterByAuthorizedAccounts(false)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    return try {
        val credentialManager = CredentialManager.create(context)
        val result = credentialManager.getCredential(context, request)
        val cred = GoogleIdTokenCredential.createFrom(result.credential.data)
        // Parseamos el ID Token JWT para obtener campos (si no tienes backend)
        val payload = cred.idToken.split(".").getOrNull(1) ?: return null
        val json = String(Base64.getUrlDecoder().decode(payload))
        val obj = JSONObject(json)

        GoogleProfile(
            sub = obj.optString("sub"),
            givenName = obj.optString("given_name", null),
            familyName = obj.optString("family_name", null),
            email = obj.optString("email"),
            picture = obj.optString("picture", null)
        )
    } catch (e: GetCredentialException) {
        null
    } catch (_: Exception) {
        null
    }
}
