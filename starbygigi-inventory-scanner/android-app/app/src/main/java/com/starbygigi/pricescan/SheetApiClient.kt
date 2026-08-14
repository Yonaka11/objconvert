package com.starbygigi.pricescan

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Thrown when the Apps Script backend returns an "error" field, or the app isn't configured yet. */
class ApiException(message: String) : Exception(message)

data class LookupResult(
    val found: Boolean,
    val barcode: String,
    val name: String,
    val price: Double?
)

/**
 * Talks to the Google Apps Script Web App described in apps-script/Code.gs.
 *
 * Apps Script Web Apps always answer with an HTTP 302 pointing at a one-time
 * `script.googleusercontent.com` URL that actually holds the response body. Automatic
 * redirect-following (OkHttp's default) resends the original request's method/headers on that
 * follow-up hop, which is fine for GET but breaks POST: Google's content server rejects it and
 * returns an unrelated "Page Not Found" page — even though the script already ran and saved the
 * data. Redirects are therefore followed manually here with a clean GET, for both GET and POST
 * calls, so responses are parsed consistently either way.
 *
 * All calls are blocking (OkHttp `execute()`) and must be run off the main thread —
 * every public function here already hops to [Dispatchers.IO] for that reason.
 */
class SheetApiClient(private val prefs: AppPrefs) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    suspend fun lookup(barcode: String): LookupResult = withContext(Dispatchers.IO) {
        val url = requireBaseUrl().toHttpUrl().newBuilder()
            .addQueryParameter("action", "lookup")
            .addQueryParameter("token", prefs.token)
            .addQueryParameter("barcode", barcode)
            .build()

        val json = parseJson(executeFollowingRedirect(Request.Builder().url(url).get().build()))
        LookupResult(
            found = json.optBoolean("found", false),
            barcode = json.optString("barcode", barcode),
            name = json.optString("name", ""),
            price = if (json.has("price") && !json.isNull("price")) json.optDouble("price") else null
        )
    }

    suspend fun createItem(barcode: String, name: String, price: Double) = withContext(Dispatchers.IO) {
        postAction(
            JSONObject().apply {
                put("action", "create")
                put("token", prefs.token)
                put("barcode", barcode)
                put("name", name)
                put("price", price)
            }
        )
    }

    suspend fun updateItem(barcode: String, price: Double) = withContext(Dispatchers.IO) {
        postAction(
            JSONObject().apply {
                put("action", "update")
                put("token", prefs.token)
                put("barcode", barcode)
                put("price", price)
            }
        )
    }

    private fun postAction(payload: JSONObject) {
        val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(requireBaseUrl()).post(body).build()

        val json = parseJson(executeFollowingRedirect(request))
        if (!json.optBoolean("success", false)) {
            throw ApiException("Server did not confirm the save. Please try again.")
        }
    }

    /** Executes [request]; if the server answers with a redirect, follows it with a clean GET. */
    private fun executeFollowingRedirect(request: Request): String {
        client.newCall(request).execute().use { response ->
            val location = response.header("Location")
            if (response.code in REDIRECT_CODES && location != null) {
                val redirected = Request.Builder().url(location).get().build()
                client.newCall(redirected).execute().use { redirectedResponse ->
                    return redirectedResponse.body?.string().orEmpty()
                }
            }
            return response.body?.string().orEmpty()
        }
    }

    private fun parseJson(body: String?): JSONObject {
        if (body.isNullOrBlank()) throw ApiException("Empty response from the server.")
        val json = try {
            JSONObject(body)
        } catch (e: Exception) {
            throw ApiException("Unexpected response from the server.")
        }
        if (json.has("error")) throw ApiException(json.optString("error"))
        return json
    }

    private fun requireBaseUrl(): String {
        val url = prefs.webAppUrl
        if (url.isNullOrBlank()) {
            throw ApiException("Set the Apps Script Web App URL in Settings first.")
        }
        return url
    }

    companion object {
        private val REDIRECT_CODES = intArrayOf(301, 302, 303, 307, 308)
    }
}
