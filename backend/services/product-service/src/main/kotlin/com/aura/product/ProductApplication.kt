package com.aura.product

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import org.jetbrains.exposed.sql.Database
import kotlinx.serialization.json.Json

data class ProductFilter(val categoryId: String? = null, val skinType: String? = null, val search: String? = null, val page: Int = 0, val size: Int = 20)

fun main() {
    val database = Database.connect("jdbc:postgresql://localhost:5432/aura", "aura_user", "aura_password")
    val repo = ProductRepositoryImpl(database)
    val getProducts = GetProductsUseCase(repo)
    val getDetails = GetProductDetailsUseCase(repo)

    embeddedServer(Netty, port = 8003) {
        install(ContentNegotiation) { json(Json { prettyPrint = true }) }
        routing {
            get("/products") {
                val filter = ProductFilter(
                    categoryId = call.request.queryParameters["categoryId"],
                    skinType = call.request.queryParameters["skinType"],
                    search = call.request.queryParameters["search"]
                )
                val result = getProducts.execute(filter)
                call.respond(result)
            }
            get("/products/{id}") {
                val id = call.parameters["id"]!!
                val details = getDetails.execute(id)
                call.respond(details!!)
            }
        }
    }.start(wait = true)
}

class GetProductsUseCase(private val repo: ProductRepository) {
    suspend fun execute(filter: ProductFilter) = ProductListResult(emptyList(), 0, 0, 0)
}
class GetProductDetailsUseCase(private val repo: ProductRepository) {
    suspend fun execute(id: String) = null
}
interface ProductRepository { suspend fun findAll(page: Int, size: Int): List<Product> }
class ProductRepositoryImpl(private val db: Database) : ProductRepository { suspend fun findAll(page: Int, size: Int) = emptyList() }
data class Product(val id: String, val name: String, val brand: String)
data class ProductListResult(val products: List<Product>, val total: Long, val page: Int, val totalPages: Int)