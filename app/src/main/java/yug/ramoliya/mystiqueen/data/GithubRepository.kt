package yug.ramoliya.daymaker.data

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import yug.ramoliya.mystiqueen.constants.Constants
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import android.webkit.MimeTypeMap

class GithubRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /**
     * Get file extension from MIME type
     */
    private fun getExtensionFromMimeType(type: String): String {
        return when (type) {
            // Images
            Constants.TYPE_IMAGE -> "jpg"
            // Videos
            Constants.TYPE_VIDEO -> "mp4"
            // Audio
            Constants.TYPE_AUDIO -> "mp3"
            else -> "bin"
        }
    }

    /**
     * Get file extension from file object
     */
    private fun getFileExtension(file: File): String {
        val name = file.name
        val lastDot = name.lastIndexOf(".")
        return if (lastDot > 0) {
            name.substring(lastDot + 1).lowercase()
        } else {
            ""
        }
    }

    /**
     * Upload file to GitHub and return raw file URL
     */
    suspend fun uploadFile(
        file: File,
        type: String // image / video / audio
    ): Result<String> = withContext(Dispatchers.IO) {

        try {
            // Validate file exists and size
            if (!file.exists()) {
                return@withContext Result.failure(Exception("File does not exist"))
            }

            val fileSizeKB = file.length() / 1024
            val maxSizeKB = when (type) {
                Constants.TYPE_IMAGE -> Constants.MAX_IMAGE_SIZE_MB * 1024
                Constants.TYPE_VIDEO -> Constants.MAX_VIDEO_SIZE_MB * 1024
                Constants.TYPE_AUDIO -> Constants.MAX_AUDIO_SIZE_MB * 1024
                else -> Constants.MAX_IMAGE_SIZE_MB * 1024
            }

            if (fileSizeKB > maxSizeKB) {
                return@withContext Result.failure(
                    Exception("File size exceeds limit: ${fileSizeKB}KB > ${maxSizeKB}KB")
                )
            }

            val folder = when (type) {
                Constants.TYPE_IMAGE -> Constants.IMAGE_FOLDER
                Constants.TYPE_VIDEO -> Constants.VIDEO_FOLDER
                Constants.TYPE_AUDIO -> Constants.AUDIO_FOLDER
                else -> Constants.IMAGE_FOLDER
            }

            // Get extension - first try file extension, then fallback to MIME type
            val extension = getFileExtension(file).ifEmpty { getExtensionFromMimeType(type) }
            val fileName = "${UUID.randomUUID()}.$extension"
            val path = "$folder$fileName"

            // Convert file to Base64
            val bytes = file.readBytes()
            val base64Content = Base64.encodeToString(bytes, Base64.NO_WRAP)

            // GitHub API URL
            val url =
                "${Constants.GITHUB_BASE_URL}repos/" +
                        "${Constants.GITHUB_OWNER}/" +
                        "${Constants.GITHUB_REPO}/" +
                        "contents/$path"

            // JSON Body
            val json = JSONObject().apply {
                put("message", "Upload $type message")
                put("content", base64Content)
                put("branch", Constants.GITHUB_BRANCH)
            }

            val body = json.toString()
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(url)
                .put(body)
                .addHeader("Authorization", "Bearer ${Constants.GITHUB_TOKEN}")
                .addHeader("Accept", "application/vnd.github+json")
                .addHeader("X-GitHub-Media-Type", "github.v3")
                .build()

            val response = client.newCall(request).execute()

            return@withContext if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                response.close()
                Result.failure(
                    Exception("GitHub upload failed (${response.code}): $errorBody")
                )
            } else {
                response.close()

                // Raw file URL
                val rawUrl =
                    "https://raw.githubusercontent.com/" +
                            "${Constants.GITHUB_OWNER}/" +
                            "${Constants.GITHUB_REPO}/" +
                            "${Constants.GITHUB_BRANCH}/$path"

                Result.success(rawUrl)
            }

        } catch (e: Exception) {
            Result.failure(Exception("Upload error: ${e.message}", e))
        }
    }
}
