@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.6.3")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.6.3")

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

@Serializable
data class BaseResponseDto<T>(
    val success: Boolean,
    val message: String,
    val data: T
)

@Serializable
data class AuthResponseDto(
    val accessToken: String
)

val jsonStr = """{"success":true,"message":"Request successful","data":{"accessToken":"eyJhb..."}}"""

val json = Json { ignoreUnknownKeys = true }
try {
    val response = json.decodeFromString<BaseResponseDto<AuthResponseDto>>(jsonStr)
    println("Parsed successfully: ${response.data.accessToken}")
} catch (e: Exception) {
    println("Parse failed: ${e.message}")
    e.printStackTrace()
}
