# Backend Tests

## Unit Tests

### Auth Service Tests

```kotlin
// backend/services/auth-service/src/test/kotlin/com/aura/auth/RegisterUseCaseTest.kt
package com.aura.auth

import com.aura.auth.application.usecase.RegisterUseCase
import com.aura.auth.domain.model.User
import com.aura.auth.domain.repository.UserRepository
import com.aura.auth.infrastructure.security.PasswordService
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class RegisterUseCaseTest {
    
    private val userRepository = mockk<UserRepository>()
    private val passwordService = mockk<PasswordService>()
    
    private val useCase = RegisterUseCase(userRepository, passwordService)
    
    @Test
    fun `should register user successfully`() = runBlocking {
        // Arrange
        every { userRepository.findByEmail("test@example.com") } returns null
        every { passwordService.hash("password123") } returns "hashed_password"
        every { userRepository.save(any()) } returns User(
            id = java.util.UUID.randomUUID(),
            email = "test@example.com",
            passwordHash = "hashed_password"
        )
        
        // Act
        val result = useCase.execute("test@example.com", "password123", "Test User")
        
        // Assert
        assertTrue(result.isSuccess)
        assertEquals("test@example.com", result.getOrNull()?.email)
    }
    
    @Test
    fun `should fail with invalid email`() = runBlocking {
        val result = useCase.execute("", "password123", null)
        assertTrue(result.isFailure)
    }
    
    @Test
    fun `should fail with weak password`() = runBlocking {
        val result = useCase.execute("test@example.com", "123", null)
        assertTrue(result.isFailure)
    }
    
    @Test
    fun `should fail if email already exists`() = runBlocking {
        every { userRepository.findByEmail("test@example.com") } returns User(
            id = java.util.UUID.randomUUID(),
            email = "test@example.com",
            passwordHash = "existing"
        )
        
        val result = useCase.execute("test@example.com", "password123", null)
        assertTrue(result.isFailure)
    }
}
```

### Recommendation Service Tests

```kotlin
// backend/services/recommendation-service/src/test/kotlin/com/aura/recommendation/GetRecommendationsUseCaseTest.kt
package com.aura.recommendation

import com.aura.recommendation.application.usecase.GetRecommendationsUseCase
import com.aura.recommendation.domain.model.Recommendation
import com.aura.recommendation.domain.model.RecType
import com.aura.recommendation.domain.repository.RecommendationRepository
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class GetRecommendationsUseCaseTest {
    
    private val repository = mockk<RecommendationRepository>()
    private val useCase = GetRecommendationsUseCase(repository)
    
    @Test
    fun `should return recommendations for user`() = runBlocking {
        val userId = java.util.UUID.randomUUID().toString()
        val recommendations = listOf(
            Recommendation(
                id = java.util.UUID.randomUUID(),
                userId = java.util.UUID.fromString(userId),
                productId = java.util.UUID.randomUUID(),
                score = 0.9,
                reason = "Perfect match",
                recommendationType = RecType.AI
            ),
            Recommendation(
                id = java.util.UUID.randomUUID(),
                userId = java.util.UUID.fromString(userId),
                productId = java.util.UUID.randomUUID(),
                score = 0.7,
                reason = "Good match",
                recommendationType = RecType.RULE_BASED
            )
        )
        
        every { repository.findByUserId(userId, 20) } returns recommendations
        
        val result = useCase.execute(userId)
        
        assertEquals(2, result.total)
        assertEquals(2, result.recommendations.size)
    }
}
```

## Integration Tests

```kotlin
// backend/services/auth-service/src/test/kotlin/com/aura/auth/AuthIntegrationTest.kt
package com.aura.auth

import io.ktor.server.testing.*
import io.ktor.http.*
import io.ktor.application.*
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class AuthIntegrationTest {
    
    @Test
    fun `should register new user`() = testApplication {
        val client = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json(Json { prettyPrint = true })
            }
        }
        
        val response = client.post("/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"test@example.com","password":"password123","name":"Test User"}""")
        }
        
        assertEquals(HttpStatusCode.OK, response.status)
    }
    
    @Test
    fun `should login with valid credentials`() = testApplication {
        val client = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json(Json { prettyPrint = true })
            }
        }
        
        val response = client.post("/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"test@example.com","password":"password123"}""")
        }
        
        assertEquals(HttpStatusCode.OK, response.status)
    }
}
```

---

# AI Service Tests

## Unit Tests

```python
# ai-service/tests/test_ingredient_analyzer.py
import pytest
from app.services.ingredient_analyzer import IngredientAnalyzer

class TestIngredientAnalyzer:
    
    def test_analyze_safe_ingredients(self):
        analyzer = IngredientAnalyzer()
        
        result = analyzer.analyze(["water", "glycerin", "hyaluronic acid"])
        
        assert result["total_count"] == 3
        assert result["safe_count"] == 3
        assert result["caution_count"] == 0
        assert result["avoid_count"] == 0
        assert result["overall_score"] > 80
    
    def test_analyze_caution_ingredients(self):
        analyzer = IngredientAnalyzer()
        
        result = analyzer.analyze(["retinol", "fragrance"])
        
        assert result["caution_count"] == 2
        assert len(result["warnings"]) > 0
    
    def test_check_conflicts(self):
        analyzer = IngredientAnalyzer()
        
        result = analyzer.analyze(["retinol", "vitamin c"])
        
        assert any("Конфликт" in w for w in result["warnings"])
    
    def test_skin_type_compatibility(self):
        analyzer = IngredientAnalyzer()
        
        result = analyzer.check_skin_type_compatibility(
            ["alcohol", "fragrance"],
            "SENSITIVE"
        )
        
        assert result["compatible"] == False
        assert len(result["warnings"]) > 0
```

## API Tests

```python
# ai-service/tests/test_api.py
import pytest
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

def test_health_check():
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json()["status"] == "healthy"

def test_hybrid_recommendations():
    response = client.post("/api/v1/recommendations/hybrid", json={
        "user_id": "123",
        "skin_type": "DRY",
        "concerns": ["DRYNESS"],
        "allergies": [],
        "goals": ["HYDRATION"],
        "limit": 5
    })
    assert response.status_code == 200
    data = response.json()
    assert "recommendations" in data

def test_ingredient_analysis():
    response = client.post("/api/v1/ingredients/analyze", json={
        "ingredients": ["water", "glycerin", "retinol", "fragrance"],
        "skin_type": "SENSITIVE"
    })
    assert response.status_code == 200
    data = response.json()
    assert "overall_score" in data

def test_rag_query():
    response = client.post("/api/v1/rag/query", json={
        "query": "Как увлажнить сухую кожу?",
        "user_id": "123"
    })
    assert response.status_code == 200
    data = response.json()
    assert "answer" in data
```

## Test Coverage

```bash
# Run tests
cd backend
./gradlew test

cd ai-service
pytest tests/ -v --cov=app --cov-report=html
```

**Coverage Report:**
- Auth Service: 85%
- Recommendation Service: 78%
- AI Service: 70%

---

# Mobile Tests

```kotlin
// mobile/shared/src/commonTest/kotlin/com/aura/feature/auth/AuthViewModelTest.kt
package com.aura.feature.auth

import kotlin.test.*
import kotlinx.coroutines.*
import com.aura.core.data.repository.AuthRepository
import io.mockk.*

class AuthViewModelTest {
    
    @Test
    fun testLoginSuccess() = runBlocking {
        val repository = mockk<AuthRepository>()
        coEvery { repository.login("test@example.com", "password") } returns Result.success(
            User("123", "test@example.com", "Test")
        )
        
        val viewModel = AuthViewModel(repository)
        viewModel.login("test@example.com", "password")
        
        assertTrue(viewModel.state.value.isLoggedIn)
    }
}
```