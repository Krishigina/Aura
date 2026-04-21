package com.aura.core.data.api.client

import com.aura.core.domain.model.Product
import com.aura.core.domain.model.ProductDetailResponse
import com.aura.core.domain.model.ProductMatch
import com.aura.core.domain.model.ProductMatchesResponse
import com.aura.core.domain.model.ProductPhoto
import com.aura.core.domain.model.RoutineProductOption
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.util.encodeBase64
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

internal class ProductNetworkClient(
    private val baseUrl: String,
    private val httpClient: HttpClient,
    private val json: Json,
    private val extractErrorMessage: (String, HttpStatusCode) -> String,
) {
    suspend fun getProducts(token: String? = null, hydratePhotos: Boolean = false): List<Product> {
        return try {
            val response = httpClient.get("$baseUrl/api/products")
            val responseText = response.bodyAsText()
            val baseProducts = try {
                json.decodeFromString<List<Product>>(responseText)
            } catch (e: Exception) {
                parseProductsManually(responseText)
            }

            val compatibilityByProductId = if (token.isNullOrBlank()) emptyMap() else getProductMatches(token, 0)
            baseProducts.map { product ->
                val match = compatibilityByProductId[product.id]
                val productWithCompatibility = product.copy(
                    compatibilityPercent = match?.compatibilityPercent,
                    decision = match?.decision,
                    explanations = match?.explanations.orEmpty(),
                )
                if (!hydratePhotos || product.id <= 0) {
                    productWithCompatibility
                } else {
                    val photos = runCatching { getProductPhotos(product.id) }.getOrDefault(emptyList())
                    if (photos.isEmpty()) productWithCompatibility else productWithCompatibility.copy(photos = photos)
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getProductDetail(token: String, productId: Int): ProductDetailResponse {
        val response = httpClient.get("$baseUrl/api/products/$productId") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        return hydrateProductDetailPhotos(json.decodeOrDefault(response.bodyTextOrThrow(extractErrorMessage), ::ProductDetailResponse))
    }

    suspend fun searchProductsForRoutine(token: String, query: String): List<RoutineProductOption> {
        val response = httpClient.get("$baseUrl/api/products/search") {
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("q", query)
            parameter("limit", 20)
        }
        val responseText = response.bodyTextOrThrow(extractErrorMessage)
        if (responseText.isBlank()) return emptyList()
        return parseRoutineProductOptions(responseText)
    }

    suspend fun getProductPhotos(productId: Int): List<ProductPhoto> {
        return try {
            val response = httpClient.get("$baseUrl/api/products/$productId/photos")
            if (!response.status.isSuccess()) return emptyList()
            val responseText = response.bodyAsText()
            if (responseText.isBlank()) return emptyList()
            runCatching { json.decodeFromString<List<ProductPhoto>>(responseText) }
                .getOrDefault(emptyList())
                .map { photo -> hydratePhotoDataFromUrl(photo) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun loadImageDataFromUrl(url: String): String? {
        if (url.isBlank()) return null
        val absoluteUrl = if (url.startsWith("http")) url else "$baseUrl$url"
        return runCatching { httpClient.get(absoluteUrl).body<ByteArray>().encodeBase64() }.getOrNull()
    }

    private suspend fun getProductMatches(token: String, minCompatibilityPercent: Int): Map<Int, ProductMatch> {
        return try {
            val response = httpClient.post("$baseUrl/api/matching/products") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("limit", 500)
                        put("min_compatibility_percent", minCompatibilityPercent)
                    },
                )
            }
            if (!response.status.isSuccess()) return emptyMap()
            val responseText = response.bodyAsText()
            if (responseText.isBlank()) return emptyMap()
            json.decodeFromString<ProductMatchesResponse>(responseText).items.associateBy { it.productId }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private suspend fun hydratePhotoDataFromUrl(photo: ProductPhoto): ProductPhoto {
        if (photo.data.isNotBlank() || photo.url.isNullOrBlank()) return photo
        val data = loadImageDataFromUrl(photo.url) ?: return photo
        return photo.copy(data = data)
    }

    private suspend fun hydrateProductDetailPhotos(detail: ProductDetailResponse): ProductDetailResponse {
        val photos = detail.product.photos.orEmpty().map { photo -> hydratePhotoDataFromUrl(photo) }
        return detail.copy(product = detail.product.copy(photos = photos))
    }

    private fun parseRoutineProductOptions(responseText: String): List<RoutineProductOption> {
        return runCatching { json.decodeFromString<List<RoutineProductOption>>(responseText) }.getOrElse {
            runCatching {
                val element = json.parseToJsonElement(responseText)
                (element as? JsonArray).orEmpty().mapNotNull { item ->
                    runCatching parseItem@{
                        val obj = item as? JsonObject ?: return@parseItem null
                        val id = obj["id"].primitiveContentOrNull()?.toIntOrNull() ?: return@parseItem null
                        RoutineProductOption(id = id, brand = obj["brand"].primitiveContentOrNull(), name = obj["name"].primitiveContentOrNull())
                    }.getOrNull()
                }
            }.getOrDefault(emptyList())
        }
    }

    private fun JsonElement?.primitiveContentOrNull(): String? {
        return runCatching { this?.jsonPrimitive?.contentOrNull }.getOrNull()
    }

    private fun parseProductsManually(text: String): List<Product> {
        return try {
            val element = json.parseToJsonElement(text)
            if (element is JsonArray) {
                element.mapNotNull { item ->
                    try {
                        val obj = item as? JsonObject ?: return@mapNotNull null
                        Product(
                            id = obj["id"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                            name = obj["name"]?.jsonPrimitive?.content,
                            brand = obj["brand"]?.jsonPrimitive?.content,
                            what_is_it = obj["what_is_it"]?.jsonPrimitive?.content,
                            product_type = obj["product_type"]?.jsonPrimitive?.content,
                            for_whom = obj["for_whom"]?.jsonPrimitive?.content,
                            purpose = obj["purpose"]?.let { if (it is JsonArray) it.mapNotNull { p -> p.jsonPrimitive.content } else null },
                            skin_type = obj["skin_type"]?.let { if (it is JsonArray) it.mapNotNull { p -> p.jsonPrimitive.content } else null },
                            application_time = obj["application_time"]?.jsonPrimitive?.content,
                            area = obj["area"]?.jsonPrimitive?.content,
                            active_ingredient = obj["active_ingredient"]?.jsonPrimitive?.content,
                            volume = obj["volume"]?.jsonPrimitive?.content,
                            segment = obj["segment"]?.jsonPrimitive?.content,
                            composition = obj["composition"]?.jsonPrimitive?.content,
                            application_info = obj["application_info"]?.jsonPrimitive?.content,
                            country = obj["country"]?.jsonPrimitive?.content,
                            country_origin = obj["country_origin"]?.jsonPrimitive?.content,
                            manufacturer = obj["manufacturer"]?.jsonPrimitive?.content,
                            description = obj["description"]?.jsonPrimitive?.content,
                            photos = obj["photos"]?.let {
                                runCatching {
                                    it.jsonArray.mapNotNull photos@{ photo ->
                                        val photoObj = photo as? JsonObject ?: return@photos null
                                        ProductPhoto(
                                            id = photoObj["id"]?.jsonPrimitive?.contentOrNull ?: "",
                                            filename = photoObj["filename"]?.jsonPrimitive?.contentOrNull ?: "",
                                            data = photoObj["data"]?.jsonPrimitive?.contentOrNull ?: "",
                                            content_type = photoObj["content_type"]?.jsonPrimitive?.contentOrNull ?: "",
                                        )
                                    }
                                }.getOrNull()
                            },
                            imageUrl = obj["imageUrl"]?.jsonPrimitive?.content,
                            thumbnailUrl = obj["thumbnail_url"]?.jsonPrimitive?.contentOrNull,
                            price = obj["price"]?.jsonPrimitive?.content,
                            currency = obj["currency"]?.jsonPrimitive?.content,
                            category = obj["category"]?.jsonPrimitive?.content,
                            desc = obj["desc"]?.jsonPrimitive?.content,
                            hasVideo = obj["has_video"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false,
                            compatibilityPercent = obj["compatibility_percent"]?.jsonPrimitive?.content?.toIntOrNull(),
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
