package com.example.data.network

import android.util.Log
import com.example.BuildConfig
import com.example.data.database.MediaItemEntity
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class Part(val text: String? = null)

@JsonClass(generateAdapter = true)
data class Content(val parts: List<Part>)

@JsonClass(generateAdapter = true)
data class ResponseFormatText(
    val mimeType: String,
    val schema: Map<String, Any>? = null
)

@JsonClass(generateAdapter = true)
data class ResponseFormat(
    val text: ResponseFormatText? = null
)

@JsonClass(generateAdapter = true)
data class ThinkingConfig(
    val thinkingLevel: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseFormat: ResponseFormat? = null,
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val thinkingConfig: ThinkingConfig? = null,
    val responseModalities: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(val content: Content)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(val candidates: List<Candidate>)

interface GeminiApiService {
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    suspend fun recommendMedia(userPrompt: String): List<MediaItemEntity> {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "API Key is empty or placeholder, using mock advisor instead")
            return getFallbackRecommendations(userPrompt)
        }

        // Build the Map schema for Moshi serialization
        @Suppress("UNCHECKED_CAST")
        val schemaMap = mapOf(
            "type" to "ARRAY",
            "items" to mapOf(
                "type" to "OBJECT",
                "properties" to mapOf(
                    "title" to mapOf("type" to "STRING", "description" to "Titolo del film o serie TV"),
                    "type" to mapOf("type" to "STRING", "description" to "Deve essere uno di: 'film', 'serie', 'kdrama'"),
                    "description" to mapOf("type" to "STRING", "description" to "Breve sinossi entusiasmante in italiano, max 3 frasi"),
                    "releaseYear" to mapOf("type" to "STRING", "description" to "Anno di uscita, es: '2024'"),
                    "genre" to mapOf("type" to "STRING", "description" to "Generi separati da virgole, es: 'Drammatico, Thriller'"),
                    "rating" to mapOf("type" to "NUMBER", "description" to "Voto medio immaginario o reale, tra 1.0 e 5.0"),
                    "platform" to mapOf("type" to "STRING", "description" to "Piattaforma in cui trovarlo, es: 'Netflix', 'Disney+', 'Prime Video'"),
                    "duration" to mapOf("type" to "STRING", "description" to "Durata o numero di episodi, es: '2h 15m' o '16 Episodi'"),
                    "cast" to mapOf("type" to "STRING", "description" to "Nomi del cast principale separati da virgole")
                ),
                "required" to listOf("title", "type", "description", "releaseYear", "genre", "rating", "platform", "duration", "cast")
            )
        ) as Map<String, Any>

        val systemInst = "Sei un esperto critico di cinema italiano. Consiglia massimo 3 film, serie TV o K-Drama in base alla richiesta dell'utente. Devi restituire ESCLUSIVAMENTE un array JSON che segue attentamente lo schema fornito. Non aggiungere prefazioni o postfazioni o blocchi di markdown. Rispondi solo in italiano."

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = "Consigliami per: $userPrompt")))),
            generationConfig = GenerationConfig(
                temperature = 0.8f,
                responseFormat = ResponseFormat(
                    text = ResponseFormatText(
                        mimeType = "application/json",
                        schema = schemaMap
                    )
                )
            ),
            systemInstruction = Content(parts = listOf(Part(text = systemInst)))
        )

        return try {
            val response = service.generateContent(apiKey, request)
            val jsonText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            Log.d(TAG, "Gemini JSON Response: $jsonText")
            parseGeminiJson(jsonText)
        } catch (e: Exception) {
            Log.e(TAG, "Error contacting Gemini API, using mock backup", e)
            getFallbackRecommendations(userPrompt)
        }
    }

    private fun parseGeminiJson(rawText: String): List<MediaItemEntity> {
        val results = mutableListOf<MediaItemEntity>()
        var cleaned = rawText.trim()
        
        // Remove potential markdown blocks
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replace(Regex("^```json\\s*"), "")
            cleaned = cleaned.replace(Regex("^```\\s*"), "")
            cleaned = cleaned.replace(Regex("\\s*```$"), "")
        }
        cleaned = cleaned.trim()
        
        // Parse and isolate JSON objects between { and }
        var index = 0
        val blocks = mutableListOf<String>()
        while (true) {
            val start = cleaned.indexOf("{", index)
            if (start == -1) break
            
            var braceCount = 1
            var current = start + 1
            while (current < cleaned.length && braceCount > 0) {
                val char = cleaned[current]
                if (char == '{') braceCount++
                else if (char == '}') braceCount--
                current++
            }
            
            if (current <= cleaned.length) {
                val block = cleaned.substring(start, current)
                blocks.add(block)
                index = current
            } else {
                break
            }
        }
        
        // Regex field extractors
        fun extractField(block: String, key: String, default: String): String {
            val regex = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"", RegexOption.IGNORE_CASE)
            val match = regex.find(block)
            return match?.groupValues?.get(1) ?: default
        }
        
        fun extractNumberField(block: String, key: String, default: Float): Float {
            val regex = Regex("\"$key\"\\s*:\\s*([0-9.]+)", RegexOption.IGNORE_CASE)
            val match = regex.find(block)
            return match?.groupValues?.get(1)?.toFloatOrNull() ?: default
        }
        
        for (block in blocks) {
            val title = extractField(block, "title", "Cinematic Hub Rec")
            val type = extractField(block, "type", "film").lowercase()
            val description = extractField(block, "description", "Disponibile per la riproduzione immediata senza limiti.")
            val releaseYear = extractField(block, "releaseYear", "2024")
            val genre = extractField(block, "genre", "Intrattenimento")
            val rating = extractNumberField(block, "rating", 4.4f)
            val platform = extractField(block, "platform", "Free")
            val duration = extractField(block, "duration", "2h 5m")
            val cast = extractField(block, "cast", "Attore Principale")
            
            val cleanId = "ai_${title.lowercase().replace(Regex("[^a-z0-9]"), "_")}"
            
            // Assign preview cinematic trailer
            val randomVideoUrl = when (type) {
                "kdrama" -> "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4"
                "serie" -> "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
                else -> "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
            }

            // High-quality background poster category match
            val randomImageUrl = when (type) {
                "kdrama" -> "https://images.unsplash.com/photo-1518156677180-95a2893f3e9f?w=500&auto=format&fit=crop&q=60"
                "serie" -> "https://images.unsplash.com/photo-1627856013091-fed6e4e30025?w=500&auto=format&fit=crop&q=60"
                else -> "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=500&auto=format&fit=crop&q=60"
            }
            
            results.add(
                MediaItemEntity(
                    id = cleanId,
                    title = title,
                    type = type,
                    description = description,
                    imageUrl = randomImageUrl,
                    releaseYear = releaseYear,
                    genre = genre,
                    rating = rating,
                    platform = platform,
                    duration = duration,
                    cast = cast,
                    videoUrl = randomVideoUrl
                )
            )
        }
        
        return results
    }

    private fun getFallbackRecommendations(prompt: String): List<MediaItemEntity> {
        val query = prompt.lowercase()
        return when {
            query.contains("k-drama") || query.contains("kdrama") || query.contains("corea") || query.contains("study") -> {
                listOf(
                    MediaItemEntity(
                        id = "ai_study_group_fall",
                        title = "Study Group (La Serie)",
                        type = "kdrama",
                        description = "Gamin Yoon, uno studente dall'aspetto mite ma formidabile nei combattimenti, cerca di formare un gruppo di studio nella peggiore scuola della città.",
                        imageUrl = "https://images.unsplash.com/photo-1518156677180-95a2893f3e9f?w=500&auto=format&fit=crop&q=60",
                        releaseYear = "2024",
                        genre = "Azione, K-Drama, Scolastico",
                        rating = 4.9f,
                        platform = "Netflix & CineHub",
                        duration = "12 Episodi",
                        cast = "Hwang Min-hyun, Han Ji-eun",
                        videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4"
                    ),
                    MediaItemEntity(
                        id = "ai_squid_game_2",
                        title = "Squid Game 2",
                        type = "kdrama",
                        description = "Il ritorno del fenomeno coreano globale su CineHub con nuove sfide mortali svelate senza login richiesto.",
                        imageUrl = "https://images.unsplash.com/photo-1627856013091-fed6e4e30025?w=500&auto=format&fit=crop&q=60",
                        releaseYear = "2024",
                        genre = "K-Drama, Thriller, Suspense",
                        rating = 4.8f,
                        platform = "Netflix",
                        duration = "9 Episodi",
                        cast = "Lee Jung-jae, Lee Byung-hun",
                        videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
                    )
                )
            }
            query.contains("prime") || query.contains("boys") -> {
                listOf(
                    MediaItemEntity(
                        id = "ai_the_boys_5",
                        title = "The Boys (Nuova Stagione)",
                        type = "serie",
                        description = "L'epico culmine dello scontro tra i Boys di Butcher e i Super corrotti guidati da Patriota, ad-free gratis.",
                        imageUrl = "https://images.unsplash.com/photo-1540224769541-7e6e9047f566?w=500&auto=format&fit=crop&q=60",
                        releaseYear = "2024",
                        genre = "Supereroi, Azione, Satira",
                        rating = 4.8f,
                        platform = "Prime Video",
                        duration = "8 Episodi",
                        cast = "Karl Urban, Antony Starr",
                        videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
                    )
                )
            }
            else -> {
                listOf(
                    MediaItemEntity(
                        id = "ai_gladiatore_2",
                        title = "Il Gladiatore II",
                        type = "film",
                        description = "Anni dopo aver assistito alla morte di Massimo, Lucius è costretto ad entrare nel Colosseo per ridare gloria a Roma.",
                        imageUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=500&auto=format&fit=crop&q=60",
                        releaseYear = "2024",
                        genre = "Storico, Azione, Drammatico",
                        rating = 4.7f,
                        platform = "Disney+ & Cinema",
                        duration = "2h 30m",
                        cast = "Paul Mescal, Pedro Pascal, Denzel Washington",
                        videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"
                    ),
                    MediaItemEntity(
                        id = "ai_deadpool_wolverine",
                        title = "Deadpool & Wolverine",
                        type = "film",
                        description = "Il mitico duo Marvel si unisce per salvare il multiverso in un uragano di azione e commedia vietata ai minori, qui gratis.",
                        imageUrl = "https://images.unsplash.com/photo-1509347528160-9a9e33742cdb?w=500&auto=format&fit=crop&q=60",
                        releaseYear = "2024",
                        genre = "Supereroi, Commedia, Azione",
                        rating = 4.8f,
                        platform = "Disney+",
                        duration = "2h 7m",
                        cast = "Ryan Reynolds, Hugh Jackman",
                        videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4"
                    )
                )
            }
        }
    }
}
