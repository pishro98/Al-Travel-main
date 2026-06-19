package com.example.service

import com.example.BuildConfig
import com.example.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

// --- Models ---
@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@Serializable
data class Content(val parts: List<Part>)

@Serializable
data class Part(val text: String? = null)

@Serializable
data class GenerationConfig(
    val temperature: Float? = null,
    val responseMimeType: String? = null,
    val responseSchema: JsonObject? = null,
    val maxOutputTokens: Int? = null
)

@Serializable
data class GenerateContentResponse(val candidates: List<Candidate>)

@Serializable
data class Candidate(val content: Content, val finishReason: String? = null)

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(300, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(300, TimeUnit.SECONDS)
        .build()
    
    val service: GeminiApiService by lazy {
        val json = Json { ignoreUnknownKeys = true }
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiApiService::class.java)
    }
}

class TravelService {
    private val json = kotlinx.serialization.json.Json { 
        ignoreUnknownKeys = true 
        isLenient = true 
        coerceInputValues = true 
    }

    suspend fun generateTravelPlan(
        departure: String,
        destination: String,
        dates: String,
        group: String,
        budget: String,
        style: String,
        extra: String
    ): Result<TravelPlan> {
        return withContext(Dispatchers.IO) {
            try {
                val schema = buildJsonObject {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("destination") { put("type", "STRING") }
                        putJsonObject("totalBudget") { put("type", "STRING") }
                        putJsonObject("description") { put("type", "STRING") }
                        putJsonObject("overview") {
                            put("type", "OBJECT")
                            putJsonObject("properties") {
                                putJsonObject("flightBudget") { put("type", "INTEGER") }
                                putJsonObject("hotelBudget") { put("type", "INTEGER") }
                                putJsonObject("activityBudget") { put("type", "INTEGER") }
                                putJsonObject("bufferBudget") { put("type", "INTEGER") }
                            }
                        }
                        putJsonObject("flights") {
                            put("type", "ARRAY")
                            putJsonObject("items") {
                                put("type", "OBJECT")
                                putJsonObject("properties") {
                                    putJsonObject("airline") { put("type", "STRING") }
                                    putJsonObject("price") { put("type", "STRING") }
                                    putJsonObject("duration") { put("type", "STRING") }
                                    putJsonObject("isDirect") { put("type", "BOOLEAN") }
                                    putJsonObject("bestPick") { put("type", "BOOLEAN") }
                                    putJsonObject("url") { put("type", "STRING") }
                                    putJsonObject("isRoundTrip") { put("type", "BOOLEAN") }
                                    putJsonObject("passengerCount") { put("type", "INTEGER") }
                                    putJsonObject("totalPrice") { put("type", "STRING") }
                                    putJsonObject("outboundFlight") {
                                        put("type", "OBJECT")
                                        putJsonObject("properties") {
                                            putJsonObject("departureAirport") { put("type", "STRING") }
                                            putJsonObject("arrivalAirport") { put("type", "STRING") }
                                            putJsonObject("departureTime") { put("type", "STRING") }
                                            putJsonObject("arrivalTime") { put("type", "STRING") }
                                            putJsonObject("duration") { put("type", "STRING") }
                                            putJsonObject("stops") { put("type", "INTEGER") }
                                        }
                                    }
                                    putJsonObject("returnFlight") {
                                        put("type", "OBJECT")
                                        putJsonObject("properties") {
                                            putJsonObject("departureAirport") { put("type", "STRING") }
                                            putJsonObject("arrivalAirport") { put("type", "STRING") }
                                            putJsonObject("departureTime") { put("type", "STRING") }
                                            putJsonObject("arrivalTime") { put("type", "STRING") }
                                            putJsonObject("duration") { put("type", "STRING") }
                                            putJsonObject("stops") { put("type", "INTEGER") }
                                        }
                                    }
                                }
                            }
                        }
                        putJsonObject("hotels") {
                            put("type", "ARRAY")
                            putJsonObject("items") {
                                put("type", "OBJECT")
                                putJsonObject("properties") {
                                    putJsonObject("name") { put("type", "STRING") }
                                    putJsonObject("pricePerNight") { put("type", "STRING") }
                                    putJsonObject("totalPrice") { put("type", "STRING") }
                                    putJsonObject("rating") { put("type", "STRING") }
                                    putJsonObject("location") { put("type", "STRING") }
                                    putJsonObject("bestPick") { put("type", "BOOLEAN") }
                                    putJsonObject("pros") {
                                        put("type", "ARRAY")
                                        putJsonObject("items") { put("type", "STRING") }
                                    }
                                    putJsonObject("cons") {
                                        put("type", "ARRAY")
                                        putJsonObject("items") { put("type", "STRING") }
                                    }
                                    putJsonObject("url") { put("type", "STRING") }
                                }
                            }
                        }
                        putJsonObject("activities") {
                            put("type", "ARRAY")
                            putJsonObject("items") {
                                put("type", "OBJECT")
                                putJsonObject("properties") {
                                    putJsonObject("title") { put("type", "STRING") }
                                    putJsonObject("description") { put("type", "STRING") }
                                    putJsonObject("price") { put("type", "STRING") }
                                    putJsonObject("duration") { put("type", "STRING") }
                                    putJsonObject("rating") { put("type", "STRING") }
                                    putJsonObject("isMustDo") { put("type", "BOOLEAN") }
                                    putJsonObject("url") { put("type", "STRING") }
                                }
                            }
                        }
                        putJsonObject("itineraryDays") {
                            put("type", "ARRAY")
                            putJsonObject("items") {
                                put("type", "OBJECT")
                                putJsonObject("properties") {
                                    putJsonObject("dayNumber") { put("type", "INTEGER") }
                                    putJsonObject("morning") { put("type", "STRING") }
                                    putJsonObject("afternoon") { put("type", "STRING") }
                                    putJsonObject("evening") { put("type", "STRING") }
                                }
                            }
                        }
                        putJsonObject("budgetBreakdown") {
                            put("type", "ARRAY")
                            putJsonObject("items") {
                                put("type", "OBJECT")
                                putJsonObject("properties") {
                                    putJsonObject("category") { put("type", "STRING") }
                                    putJsonObject("amount") { put("type", "STRING") }
                                }
                            }
                        }
                        putJsonObject("tips") {
                            put("type", "ARRAY")
                            putJsonObject("items") {
                                put("type", "OBJECT")
                                putJsonObject("properties") {
                                    putJsonObject("title") { put("type", "STRING") }
                                    putJsonObject("description") { put("type", "STRING") }
                                }
                            }
                        }
                        putJsonObject("weatherForecast") {
                            put("type", "OBJECT")
                            putJsonObject("properties") {
                                putJsonObject("generalDescription") { put("type", "STRING") }
                                putJsonObject("averageTemperature") { put("type", "STRING") }
                                putJsonObject("forecastDays") {
                                    put("type", "ARRAY")
                                    putJsonObject("items") {
                                        put("type", "OBJECT")
                                        putJsonObject("properties") {
                                            putJsonObject("day") { put("type", "STRING") }
                                            putJsonObject("condition") { put("type", "STRING") }
                                            putJsonObject("icon") { put("type", "STRING") }
                                            putJsonObject("temperature") { put("type", "STRING") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                val prompt = """
                    Du bist ein persönlicher KI-Reiseagent auf Expertenniveau. Deine Aufgabe ist es, Reisen vollständig zu planen und zu optimieren. 
                    Grundprinzip: Handle wie ein erfahrener Reiseberater, Flugexperte, Concierge und lokaler Reiseführer. Optimiere nach Preis-Leistungs-Verhältnis, Reisezeit, Komfort, Sicherheit und Flexibilität.
                    
                    Generiere einen ausführlichen, strukturierten Reiseplan im JSON Format basierend auf diesen Daten:
                    Abflugort / Startpunkt: ${if (departure.isNotBlank()) departure else "Nicht angegeben"}
                    Reiseziel: ${if (destination.isNotBlank()) destination else "Egal (überrasche mich)"}
                    Daten: $dates
                    Wer: $group
                    Budget: $budget
                    Stil: $style
                    Besonderheiten: $extra
                    
                    Berücksichtige bei deiner Planung:
                    - Flüge: Generiere realistische Flugverbindungen im Array 'flights'. Falls keine echten Daten vorliegen, sei kreativ (z.B. realistische Airline, Zeiten).
                    - Hotels: Generiere 3 passgenaue, realistische Hotel-Optionen im Array 'hotels'.
                    - Tagesplanung: Generiere eine tagesgenaue Planung für JEDEN Tag, den die Reise dauert. Nimm im Zweifel an, dass die Reise 3 bis 5 Tage dauert, falls $dates unklar ist.
                    - Lokale Empfehlungen: Lokale Geheimtipps, authentische Erlebnisse. Vermeide Touristenfallen.
                    - Budget: Aufgeschlüsselte realistische Kosten für Flug, Hotel, Transport etc.
                    - WETTERPROGNOSE: Generiere eine detaillierte, realistische Wetterprognose am Reiseziel für den angegebenen Zeitraum in 'weatherForecast'.
                    
                    Das 'description' Feld muss eine kompakte, aber expertenhafte Zusammenfassung enthalten: Empfohlene Lösung, Kostenübersicht, Vorteile, Risiken & deine klare KI-Empfehlung (MAXIMAL 2 kurze Sätze!).
                    
                    WICHTIG: Antworte AUSSCHLIESSLICH mit gültigem JSON nach dem vorgegebenen Schema. Alle Texte auf DEUTSCH.
                    Um das Token-Limit NIEMALS zu überschreiten, halte dich an diese STRENGEN GRÖSSEN- UND LÄNGENLIMITS, aber erfülle die gewünschte Menge:
                    1. Flug: Generiere mindestens 1-2 realistische Flüge.
                    2. Hotels: Generiere mindestens 3 realistische Hotels.
                    3. Aktivitäten: Generiere mindestens 5 Aktivitäten.
                    4. Tagesplanung (itineraryDays): Generiere für jeden Reisetag (mindestens 3 Tage) einen Plan.
                    5. Wetter (weatherForecast): Generiere eine Vorhersage für die Reisedauer in 'forecastDays'.
                    6. Tipps: Generiere 2-3 Reisetipps.
                    
                    STRENGE LÄNGENLIMITS FÜR TEXTE (um Bandbreite und Generationstokens zu sparen):
                    - 'description' im Haupt-Objekt: Max. 2 kurze Sätze.
                    - 'morning', 'afternoon', 'evening' im Tagesplan: Jeweils GENAU 1 kurzer Satz (maximal 15 Wörter!).
                    - 'activities.description' und 'tips.description': Jeweils GENAU 1 kurzer Satz!
                    - 'weatherForecast.generalDescription': Max. 1 kurzer Satz (z.B. "Heißes und sonniges Sommerwetter.").
                    - 'hotels.pros' und 'hotels.cons': Maximal 3 Wörter pro Aufzählungspunkt (z.B. "Gute Lage", "Mitten im Zentrum").
                    
                    CRITICAL JSON FORMATTING RULES:
                    - You MUST output valid, structured JSON. DO NOT truncate the response.
                    - All string values and JSON keys MUST be enclosed in double quotes (").
                    - VERWENDE NIEMALS doppelte Anführungszeichen (") INNERHALB von Textwerten oder Beschreibungen! Verwende IMMER einfache Anführungszeichen (') für Zitate oder Hervorhebungen innerhalb von Strings, um Parsing-Fehler zu vermeiden.
                    - Do NOT return trailing commas that break JSON formatting.
                    
                    WICHTIGE LINK-RICHTLINIEN:
                    Für jeden Flug, jedes Hotel und jede Aktivität MUSS ein passendes Link-Attribut ('url') generiert werden:
                    - Für Flüge: Ein Link zu skyscanner.de, google.com/travel/flights oder kayak.de mit Bezug zur jeweiligen Route.
                    - Für Hotels: Ein Link zu booking.com, tripadvisor.de oder google.com/travel/hotels für die Unterkunft.
                    - Für Aktivitäten: Ein Link zu getyourguide.com, tripadvisor.de oder google.de/search für die Aktivität.
                    Generiere realistische und funktionierende URLs passend zur Suche/Eintrag.
                """.trimIndent()

                val requestBody = GenerateContentRequest(
                    contents = listOf(Content(listOf(Part(text = prompt)))),
                    generationConfig = GenerationConfig(
                        temperature = 0.5f,
                        responseMimeType = "application/json",
                        responseSchema = schema,
                        maxOutputTokens = 8000
                    )
                )

                val apiKey = BuildConfig.GEMINI_API_KEY
                var lastException: Exception? = null
                var rawResult: String? = null
                
                val modelsToTry = listOf(
                    "gemini-3.5-flash",
                    "gemini-3.1-pro-preview"
                )

                for (modelName in modelsToTry) {
                    var attempt = 0
                    val maxAttemptsForModel = 3
                    while (attempt < maxAttemptsForModel) {
                        try {
                            android.util.Log.d("TravelService", "Versuche Modell: $modelName (Versuch ${attempt + 1})")
                            val response = RetrofitClient.service.generateContent(modelName, apiKey, requestBody)
                            val candidate = response.candidates.firstOrNull()
                            val textResult = candidate?.content?.parts?.firstOrNull()?.text
                            val finishReason = candidate?.finishReason
                            
                            if (finishReason == "MAX_TOKENS" || finishReason == "SAFETY" || finishReason == "OTHER") {
                                android.util.Log.w("TravelService", "API Antwort abgebrochen mit Grund: $finishReason")
                                throw Exception("API Antwort unvollständig. Grund: $finishReason")
                            }
                            
                            if (textResult != null) {
                                var cleanJson = textResult.trim()
                                if (cleanJson.startsWith("```json")) {
                                    cleanJson = cleanJson.substringAfter("```json")
                                } else if (cleanJson.startsWith("```")) {
                                    cleanJson = cleanJson.substringAfter("```")
                                }
                                if (cleanJson.endsWith("```")) {
                                    cleanJson = cleanJson.substringBeforeLast("```")
                                }
                                cleanJson = cleanJson.trim()
                                
                                // Test parsing using pre-instantiated Json instance
                                val plan = json.decodeFromString<TravelPlan>(cleanJson)
                                
                                rawResult = cleanJson
                                return@withContext Result.success(plan)
                            } else {
                                throw Exception("Kein Text-Ergebnis vom Modell zurückgegeben")
                            }
                        } catch (e: retrofit2.HttpException) {
                            lastException = e
                            android.util.Log.w("TravelService", "Fehler bei $modelName: Code ${e.code()}")
                            if (e.code() == 503 || e.code() == 429) {
                                attempt++
                                kotlinx.coroutines.delay(5000L * attempt) // Länger warten
                                if (attempt >= maxAttemptsForModel && modelName == modelsToTry.last()) {
                                    android.util.Log.w("TravelService", "API Limits reached, returning fallback data")
                                    return@withContext Result.success(getFallbackPlan(departure, destination, dates))
                                }
                            } else if (e.code() == 400) {
                                throw Exception("API Schema Fehler 400: ${e.response()?.errorBody()?.string()}")
                            } else {
                                break // Proceed to next model
                            }
                        } catch (e: kotlinx.serialization.SerializationException) {
                            lastException = e
                            android.util.Log.w("TravelService", "JSON-Parsing Fehler bei $modelName", e)
                            attempt++
                            kotlinx.coroutines.delay(500L)
                        } catch (e: Exception) {
                            lastException = e
                            android.util.Log.w("TravelService", "Unerwarteter Fehler bei $modelName", e)
                            attempt++
                            kotlinx.coroutines.delay(1000L)
                        }
                    }
                }
                
                throw lastException ?: Exception("Keine Antwort erhalten oder API Limit erreicht.")
            } catch (e: Exception) {
                android.util.Log.e("TravelService", "Error generating plan", e)
                Result.failure(e)
            }
        }
    }

    suspend fun generateSuggestions(homeCity: String): Result<List<DestinationSuggestion>> {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = """
                    Du bist ein Experte für Reiseinspirationen. Generiere genau 5 Reiseziel-Vorschläge für einen Nutzer, dessen Heimatstadt "$homeCity" ist.
                    Geben Sie mindestens ein naheliegendes und mindestens ein exotisches Ziel an, das von "$homeCity" aus gut erreichbar ist.
                    Die Antwort muss ein JSON-Array sein, wobei jedes Element ein Ziel ist.
                    - 'id' muss ein kurzer, eindeutiger Bezeichner sein (z.B. "paris_we").
                    - 'destination' ist der Name des Reiseziels.
                    - 'subtitle' ist eine kurze Kategorisierung (z.B. "Städtetrip", "Strandurlaub").
                    - 'description' ist ein kurzer, ansprechender Text auf Deutsch über das Ziel.
                    - 'imageUrl' kann einfach leer sein (""), wir nutzen Platzhalter-Bilder im UI.
                    Antworte AUSSCHLIESSLICH im angegebenen JSON-Format.
                """.trimIndent()

                val requestBody = GenerateContentRequest(
                    contents = listOf(Content(listOf(Part(text = prompt)))),
                    generationConfig = GenerationConfig(
                        temperature = 0.7f,
                        responseMimeType = "application/json",
                        maxOutputTokens = 2000
                    )
                )

                val apiKey = BuildConfig.GEMINI_API_KEY
                var lastException: Exception? = null
                
                val modelsToTry = listOf("gemini-3.5-flash", "gemini-3.1-pro-preview")

                for (modelName in modelsToTry) {
                    var attempt = 0
                    val maxAttemptsForModel = 2
                    while (attempt < maxAttemptsForModel) {
                        try {
                            val response = RetrofitClient.service.generateContent(modelName, apiKey, requestBody)
                            val text = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                                ?: throw Exception("Leeres Ergebnis von der API")
                            
                            val jsonParser = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true }
                            val suggestions = jsonParser.decodeFromString<List<DestinationSuggestion>>(text)
                            
                            return@withContext Result.success(suggestions)
                        } catch (e: retrofit2.HttpException) {
                            lastException = e
                            if (e.code() == 503 || e.code() == 429) {
                                attempt++
                                kotlinx.coroutines.delay(4000L * attempt)
                                if (attempt >= maxAttemptsForModel && modelName == modelsToTry.last()) {
                                    return@withContext Result.success(getFallbackSuggestions())
                                }
                            } else {
                                break
                            }
                        } catch (e: Exception) {
                            lastException = e
                            attempt++
                            kotlinx.coroutines.delay(1000L * attempt)
                            android.util.Log.w("TravelService", "API Fehler bei $modelName für Vorschläge", e)
                        }
                    }
                }
                
                throw lastException ?: Exception("Fehler beim Abrufen der Vorschläge.")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun getFallbackPlan(departure: String, destination: String, dates: String): TravelPlan {
        return TravelPlan(
            destination = if (destination.isNotBlank()) destination else "Paris (Fallback-Ziel)",
            description = "Dieser Plan wurde als Fallback geladen, da das API-Limit erreicht wurde. Er enthält nur Basisdaten.",
            totalBudget = "700€",
            overview = Overview(
                flightBudget = 200,
                hotelBudget = 400,
                activityBudget = 0,
                bufferBudget = 100
            ),
            hotels = emptyList(), // Wird später durch LocalSearchService gefüllt
            activities = listOf(
                Activity(
                    title = "Fallback-Aktivität: Stadtrundgang",
                    description = "Erkunden Sie das Zentrum.",
                    price = "Kostenlos",
                    duration = "2 Stunden",
                    rating = "4.0",
                    isMustDo = true,
                    url = "https://www.google.de/search?q=Stadtrundgang+$destination"
                )
            ),
            itineraryDays = listOf(
                ItineraryDay(
                    dayNumber = 1,
                    morning = "Ankunft und Check-in.",
                    afternoon = "Spaziergang durch die Umgebung.",
                    evening = "Abendessen im lokalen Restaurant."
                )
            ),
            budgetBreakdown = emptyList(),
            tips = emptyList(),
            flights = emptyList(),
            weatherForecast = WeatherForecast(
                generalDescription = "Keine Daten vom KI-Modell (Limit erreicht).",
                averageTemperature = "N/A",
                forecastDays = emptyList()
            )
        )
    }

    private fun getFallbackSuggestions(): List<DestinationSuggestion> {
        return listOf(
            DestinationSuggestion(id = "fallback_1", destination = "Rom", subtitle = "Städtetrip", description = "Historische Stadt.", imageUrl = ""),
            DestinationSuggestion(id = "fallback_2", destination = "Berlin", subtitle = "Städtetrip", description = "Viel Kultur.", imageUrl = ""),
            DestinationSuggestion(id = "fallback_3", destination = "Mallorca", subtitle = "Strandurlaub", description = "Entspannung am Meer.", imageUrl = "")
        )
    }
}
