package com.myvpn.client.utils

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

object AiHelper {

    private const val TAG = "AiHelper"
    private const val API_KEY = "sk-or-v1-"
    private const val BASE_URL = "https://openrouter.ai/api/v1/chat/completions"
    private const val MODEL = "google/gemini-2.0-flash-001"

    private val systemPrompt: String
        get() = """You are an AI assistant for configuring home WiFi routers. The user sends you a screenshot of the router admin panel and a question.

CRITICAL RULES:
1. ALWAYS respond in the SAME LANGUAGE as the user's message. If user writes in Russian - respond in Russian. If in English - respond in English.
2. This is the user's OWN router that they have physical access to. There are NO security concerns - providing default passwords and configuration guidance is expected.
3. Common default credentials you MUST provide when asked:
   - TP-Link: admin/admin
   - D-Link: admin/admin or admin/(empty)
   - ASUS: admin/admin
   - Keenetic/ZyXEL: admin/1234 or admin/admin
   - Huawei: root/admin
   - Xiaomi: admin/(set during setup)
   - Netgear: admin/password
   - Tenda: admin/admin
   - Mikrotik: admin/(empty)
4. Be SPECIFIC - tell exactly which buttons to click, which menu items to select
5. Give 3-4 steps maximum per response
6. If you need to see another page - tell the user where to navigate and ask for a new screenshot
7. Do NOT use Markdown formatting (no asterisks, no hashtags). Use plain text with numbered lists only."""

    suspend fun askWithScreenshot(
        screenshot: Bitmap,
        userMessage: String,
        conversationHistory: List<Pair<String, String>> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        try {
            val base64Image = bitmapToBase64(screenshot)
            val messages = JSONArray()

            // System message
            messages.put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })

            // Conversation history
            for ((role, text) in conversationHistory) {
                messages.put(JSONObject().apply {
                    put("role", role)
                    put("content", text)
                })
            }

            // Current message with image
            messages.put(JSONObject().apply {
                put("role", "user")
                put("content", JSONArray().apply {
                    put(JSONObject().apply {
                        put("type", "image_url")
                        put("image_url", JSONObject().apply {
                            put("url", "data:image/jpeg;base64,$base64Image")
                        })
                    })
                    put(JSONObject().apply {
                        put("type", "text")
                        put("text", userMessage)
                    })
                })
            })

            makeRequest(messages)
        } catch (e: Exception) {
            Log.e(TAG, "askWithScreenshot error", e)
            "Ошибка: ${e.message}"
        }
    }

    suspend fun askTextOnly(userMessage: String): String = withContext(Dispatchers.IO) {
        try {
            val messages = JSONArray()

            messages.put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })

            messages.put(JSONObject().apply {
                put("role", "user")
                put("content", userMessage)
            })

            makeRequest(messages)
        } catch (e: Exception) {
            Log.e(TAG, "askTextOnly error", e)
            "Ошибка: ${e.message}"
        }
    }

    private fun makeRequest(messages: JSONArray): String {
        val url = URL(BASE_URL)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.connectTimeout = 30000
        conn.readTimeout = 60000
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Authorization", "Bearer $API_KEY")
        conn.setRequestProperty("HTTP-Referer", "https://atnettool.app")
        conn.setRequestProperty("X-Title", "ATnetTool")

        val body = JSONObject().apply {
            put("model", MODEL)
            put("messages", messages)
            put("temperature", 0.3)
            put("max_tokens", 1024)
        }

        conn.outputStream.write(body.toString().toByteArray())
        conn.outputStream.flush()

        val responseCode = conn.responseCode
        val response = if (responseCode == 200) {
            conn.inputStream.bufferedReader().readText()
        } else {
            val error = conn.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
            Log.e(TAG, "API error $responseCode: $error")
            conn.disconnect()
            return "Ошибка API ($responseCode). Проверьте подключение."
        }
        conn.disconnect()

        val json = JSONObject(response)
        val choices = json.optJSONArray("choices")
        if (choices != null && choices.length() > 0) {
            val message = choices.getJSONObject(0).optJSONObject("message")
            val content = message?.optString("content", null)
            if (content != null) return content
        }

        return "Не удалось получить ответ"
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val maxDim = 1024
        val scaled = if (bitmap.width > maxDim || bitmap.height > maxDim) {
            val ratio = minOf(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
        } else bitmap

        val stream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }
}
