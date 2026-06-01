package com.example.data.network

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com"
    private const val MODEL_NAME = "gemini-3.5-flash"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Calls Gemini API to generate a summary or smart tags of the note.
     * Fallbacks to offline local parsing if key is default or missing.
     */
    suspend fun generateSummary(noteContent: String, title: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        Log.d(TAG, "Using API key placeholder state: ${apiKey != "MY_GEMINI_API_KEY"}")

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            // Intelligent local simulated summary based on paragraphs for flawless offline experience
            return@withContext generateLocalMockSummary(noteContent, title)
        }

        try {
            val url = "$BASE_URL/v1beta/models/$MODEL_NAME:generateContent?key=$apiKey"

            val prompt = """
                You are a smart note summarization assistant for Smart Notes Pro. 
                Generate a concise, elegant bullet-point summary (maximum 3 bullet points) 
                representing the core ideas of the note below. 
                Do not add any chatty preamble or structural headings.
                
                Note Title: $title
                Note Content:
                $noteContent
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        })
                    })
                })
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (response.isSuccessful && responseString.isNotEmpty()) {
                val jsonObject = JSONObject(responseString)
                val candidates = jsonObject.optJSONArray("candidates")
                val contentObj = candidates?.optJSONObject(0)?.optJSONObject("content")
                val parts = contentObj?.optJSONArray("parts")
                val txt = parts?.optJSONObject(0)?.optString("text")

                if (!txt.isNullOrEmpty()) {
                    return@withContext txt.trim()
                }
            }
            Log.e(TAG, "Network response error code: ${response.code}, response: $responseString")
            return@withContext generateLocalMockSummary(noteContent, title)
        } catch (e: Exception) {
            Log.e(TAG, "Error executing Gemini API call, using backup summarizer: ", e)
            return@withContext generateLocalMockSummary(noteContent, title)
        }
    }

    /**
     * Smart local summarizer based on markdown structure if offline or API key is unconfigured.
     */
    private fun generateLocalMockSummary(noteContent: String, title: String): String {
        val lines = noteContent.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return "Empty note. Add some content to summarize."

        val bullets = mutableListOf<String>()
        
        // Try searching for prominent headers
        val headers = lines.filter { it.startsWith("#") }.take(2)
        if (headers.isNotEmpty()) {
            bullets.add("Focuses on sections: " + headers.map { it.replace("#", "").trim() }.joinToString(", "))
        }

        // Add first sentence or two as summary item
        val bodyLine = lines.firstOrNull { !it.startsWith("#") && !it.startsWith("*") && !it.startsWith(">") }
        if (bodyLine != null) {
            val shortText = if (bodyLine.length > 80) bodyLine.substring(0, 77) + "..." else bodyLine
            bullets.add("Core context: $shortText")
        }

        // Quick meta tag line
        bullets.add("Locally generated preview of '$title'. Enter your real Gemini API Key in the AI Studio Secrets Panel to unlock full neural network indexing.")

        return bullets.joinToString("\n") { "• $it" }
    }
}
