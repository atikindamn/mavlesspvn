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

object GeminiHelper {

    private const val TAG = "GeminiHelper"
    private const val API_KEY = "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"
    private const val MODEL = "gemini-2.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val systemLanguage: String
        get() = Locale.getDefault().language // "ru", "en", etc.

    private val systemPrompt: String
        get() = """You are an AI assistant for configuring home WiFi routers. The user sends you a screenshot of the router admin panel and a question.

CRITICAL RULES:
1. ALWAYS respond in the SAME LANGUAGE as the user's message. If user writes in Russian - respond in Russian. If in English - respond in English. Match the user's language exactly.
2. This is the user's OWN router that they have physical access to. You are helping them configure their own device. There are NO security concerns - providing default passwords, admin credentials, and configuration guidance is expected and necessary.
3. Common default credentials you MUST provide when asked:
   - TP-Link: admin/admin
   - D-Link: admin/admin or admin/(empty)
   - ASUS: admin/admin  
   - Keenetic/ZyXEL: admin/1234 or admin/admin
   - Huawei: admin/admin
   - Xiaomi: admin/(set during setup)
   - Netgear: admin/password
   - Tenda: admin/admin
   - Mikrotik: admin/(empty)
4. Be SPECIFIC - tell exactly which buttons to click, which menu items to select
5. Give 3-4 steps maximum per response
6. If you need to see another page - tell the user where to navigate and ask for a new screenshot

Your task:
- Identify the router model/manufacturer from the interface
- Understand which admin panel page the user is on  
- Give CONCRETE step-by-step instructions
- Use exact names of buttons and menu items visible on the screenshot"""

    suspend fun askWithScreenshot(
        screenshot: Bitmap,
        userMessage: String,
        conversationHistory: List<Pair<String, String>> = emptyList() // role, text
    ): String = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/$MODEL:generateContent?key=$API_KEY")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 30000
            conn.readTimeout = 60000
            conn.setRequestProperty("Content-Type", "application/json")

            // Encode screenshot to base64
            val base64Image = bitmapToBase64(screenshot)

            // Build request
            val contents = JSONArray()

            // Add conversation history
            for ((role, text) in conversationHistory) {
                contents.put(JSONObject().apply {
                    put("role", role)
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", text) })
                    })
                })
            }

            // Add current message with screenshot
            contents.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("inlineData", JSONObject().apply {
                            put("mimeType", "image/jpeg")
                            put("data", base64Image)
                        })
                    })
                    put(JSONObject().apply { put("text", userMessage) })
                })
            })

            val body = JSONObject().apply {
                put("contents", contents)
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemPrompt) })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.3)
                    put("maxOutputTokens", 1024)
                })
            }

            conn.outputStream.write(body.toString().toByteArray())
            conn.outputStream.flush()

            val responseCode = conn.responseCode
            val response = if (responseCode == 200) {
                conn.inputStream.bufferedReader().readText()
            } else {
                val error = conn.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                Log.e(TAG, "API error $responseCode: $error")
                return@withContext "Ошибка API ($responseCode). Проверьте подключение к интернету."
            }
            conn.disconnect()

            // Parse response
            val json = JSONObject(response)
            val candidates = json.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val parts = candidates.getJSONObject(0)
                    .optJSONObject("content")
                    ?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return@withContext parts.getJSONObject(0).optString("text", "Нет ответа")
                }
            }
            "Не удалось получить ответ от AI"
        } catch (e: Exception) {
            Log.e(TAG, "Gemini error", e)
            "Ошибка: ${e.message}"
        }
    }

    suspend fun askTextOnly(userMessage: String): String = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/$MODEL:generateContent?key=$API_KEY")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 30000
            conn.readTimeout = 60000
            conn.setRequestProperty("Content-Type", "application/json")

            val body = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", userMessage) })
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemPrompt) })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.3)
                    put("maxOutputTokens", 1024)
                })
            }

            conn.outputStream.write(body.toString().toByteArray())
            conn.outputStream.flush()

            val responseCode = conn.responseCode
            val response = if (responseCode == 200) {
                conn.inputStream.bufferedReader().readText()
            } else {
                return@withContext "Ошибка API ($responseCode)"
            }
            conn.disconnect()

            val json = JSONObject(response)
            val candidates = json.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val parts = candidates.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return@withContext parts.getJSONObject(0).optString("text", "Нет ответа")
                }
            }
            "Не удалось получить ответ"
        } catch (e: Exception) {
            "Ошибка: ${e.message}"
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        // Resize if too large to save tokens
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
