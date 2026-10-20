package com.ibnluqman.vidgrab

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Resolves public Instagram posts into an ordered list of direct media URLs before Snatch queues
 * any downloads. This avoids guessing a carousel item's type from nearby HTML text.
 */
internal object InstagramPostResolver {
    data class Media(val url: String, val type: String)

    private const val tag = "Snatch"
    private const val graphQlUrl = "https://www.instagram.com/api/graphql"
    private const val documentId = "10015901848480474"
    private const val userAgent = "Mozilla/5.0 (Linux; Android 15; Pixel 8) AppleWebKit/537.36 Chrome/124.0 Mobile Safari/537.36"

    fun resolve(postUrl: String, cookieHeader: String): List<Media> {
        val shortcode = shortcodeFrom(postUrl) ?: return emptyList()
        return try {
            val variables = JSONObject().apply {
                put("shortcode", shortcode)
                put("fetch_comment_count", 0)
                put("fetch_tagged_user_count", 0)
            }
            val body = "doc_id=${URLEncoder.encode(documentId, "UTF-8")}" +
                "&variables=${URLEncoder.encode(variables.toString(), "UTF-8")}" +
                "&lsd=snatch"
            val connection = (URL(graphQlUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("User-Agent", userAgent)
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                setRequestProperty("X-FB-LSD", "snatch")
                setRequestProperty("X-IG-App-ID", "936619743392459")
                setRequestProperty("Origin", "https://www.instagram.com")
                setRequestProperty("Referer", "https://www.instagram.com/")
                if (cookieHeader.isNotBlank()) setRequestProperty("Cookie", cookieHeader)
                connectTimeout = 15_000
                readTimeout = 15_000
            }
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(body) }
            val response = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            connection.disconnect()
            if (response.isBlank()) return emptyList()
            val media = JSONObject(response).optJSONObject("data")
                ?.optJSONObject("xdt_shortcode_media") ?: return emptyList()
            parse(media)
        } catch (error: Exception) {
            Log.w(tag, "Instagram structured resolver failed; trying compatibility fallback", error)
            emptyList()
        }
    }

    private fun shortcodeFrom(url: String): String? =
        Regex("instagram\\.com/(?:p|reel|tv)/([^/?#]+)", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.getOrNull(1)

    private fun parse(post: JSONObject): List<Media> {
        val children = post.optJSONObject("edge_sidecar_to_children")?.optJSONArray("edges")
        if (children != null && children.length() > 0) {
            return (0 until children.length()).mapNotNull { index ->
                children.optJSONObject(index)?.optJSONObject("node")?.let(::mediaFrom)
            }
        }
        return mediaFrom(post)?.let(::listOf).orEmpty()
    }

    private fun mediaFrom(item: JSONObject): Media? {
        val video = item.optBoolean("is_video", false)
        val url = if (video) item.optString("video_url") else item.optString("display_url")
        return url.takeIf { it.isNotBlank() }?.let { Media(it, if (video) "VIDEO" else "PHOTO") }
    }
}
