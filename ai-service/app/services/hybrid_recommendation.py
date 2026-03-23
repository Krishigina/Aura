from typing import List, Dict, Any, Optional
import numpy as np
from app.infrastructure.vector_store import get_vector_store
from app.infrastructure.embedder import get_embedder

class HybridRecommendationEngine:
    """Hybrid: Rule-based + AI Vector Search"""
    
    def __init__(self):
        self.vector_store = get_vector_store()
        self.embedder = get_embedder()
    
    def get_recommendations(
        self,
        user_profile: Dict[str, Any],
        products: List[Dict[str, Any]],
        limit: int = 10
    ) -> List[Dict[str, Any]]:
        
        # Step 1: Rule-based filtering
        filtered = self._rule_based_filter(user_profile, products)
        
        # Step 2: AI scoring with vector similarity
        scored = self._ai_scoring(user_profile, filtered)
        
        # Step 3: Combine and rank
        ranked = sorted(scored, key=lambda x: x['score'], reverse=True)
        
        return ranked[:limit]
    
    def _rule_based_filter(
        self,
        user_profile: Dict[str, Any],
        products: List[Dict[str, Any]]
    ) -> List[Dict[str, Any]]:
        """Rule-based filtering"""
        filtered = []
        
        skin_type = user_profile.get('skin_type', '').upper()
        allergies = [a.lower() for a in user_profile.get('allergies', [])]
        concerns = [c.upper() for c in user_profile.get('concerns', [])]
        
        for product in products:
            # Check suitability for skin type
            suitable_types = product.get('suitable_skin_types', [])
            if skin_type and skin_type not in suitable_types:
                continue
            
            # Check for allergens in ingredients
            ingredients = product.get('ingredients', '').lower()
            if any(allergy in ingredients for allergy in allergies):
                continue
            
            # Check matching concerns
            target_concerns = [c.upper() for c in product.get('target_concerns', [])]
            if concerns and not any(c in target_concerns for c in concerns):
                # Could still include but with lower score
                product['concern_match'] = False
            else:
                product['concern_match'] = True
            
            filtered.append(product)
        
        return filtered
    
    def _ai_scoring(
        self,
        user_profile: Dict[str, Any],
        products: List[Dict[str, Any]]
    ) -> List[Dict[str, Any]]:
        """AI-powered scoring with embeddings"""
        
        # Build user profile query
        profile_text = self._build_profile_text(user_profile)
        
        for product in products:
            # Get product text for embedding
            product_text = f"{product.get('name', '')} {product.get('description', '')}"
            
            # Calculate similarity
            profile_emb = self.embedder.embed(profile_text)
            product_emb = self.embedder.embed(product_text)
            
            similarity = self._cosine_similarity(profile_emb, product_emb)
            
            # Base score from similarity (40%)
            base_score = similarity * 0.4
            
            # Concern match bonus (30%)
            concern_bonus = 0.3 if product.get('concern_match', False) else 0.0
            
            # Skin type bonus (20%)
            skin_bonus = 0.2
            
            # Popularity/social proof bonus (10%)
            popularity_bonus = product.get('popularity', 0) * 0.1
            
            # Generate explanation
            product['score'] = min(base_score + concern_bonus + skin_bonus + popularity_bonus, 1.0)
            product['reason'] = self._generate_reason(user_profile, product)
        
        return products
    
    def _build_profile_text(self, profile: Dict[str, Any]) -> str:
        parts = []
        
        skin_type = profile.get('skin_type', '')
        if skin_type:
            skin_descriptions = {
                'DRY': 'сухая кожа needs hydration',
                'OILY': 'жирная кожа needs oil control',
                'COMBINATION': 'комбинированная кожа',
                'NORMAL': 'нормальная кожа',
                'SENSITIVE': 'чувствительная кожа gentle products'
            }
            parts.append(skin_descriptions.get(skin_type.upper(), skin_type))
        
        concerns = profile.get('concerns', [])
        if concerns:
            parts.append(f"concerns: {', '.join(concerns)}")
        
        goals = profile.get('goals', [])
        if goals:
            parts.append(f"goals: {', '.join(goals)}")
        
        return " | ".join(parts)
    
    def _cosine_similarity(self, vec1: List[float], vec2: List[float]) -> float:
        dot = sum(a * b for a, b in zip(vec1, vec2))
        norm1 = np.sqrt(sum(a * a for a in vec1))
        norm2 = np.sqrt(sum(b * b for b in vec2))
        
        if norm1 == 0 or norm2 == 0:
            return 0.0
        
        return dot / (norm1 * norm2)
    
    def _generate_reason(self, profile: Dict[str, Any], product: Dict[str, Any]) -> str:
        reasons = []
        
        # Skin type match
        if product.get('concern_match', False):
            concerns = profile.get('concerns', [])
            if concerns:
                reasons.append(f"помогает с {', '.join(concerns[:2])}")
        
        # Goals match
        goals = profile.get('goals', [])
        goal_reasons = {
            'HYDRATION': 'глубоко увлажняет',
            'ANTI_AGING': 'борьба с возрастными изменениями',
            'ACNE_TREATMENT': 'лечение акне',
            'BRIGHTENING': 'осветление и сияние',
            'SOOTHING': 'успокаивает раздражения'
        }
        
        for goal in goals:
            if goal in goal_reasons:
                reasons.append(goal_reasons[goal])
                break
        
        if not reasons:
            reasons.append("подходит для вашего типа кожи")
        
        return f"{product.get('name', '')}: {', '.join(reasons[:2])}."


# Singleton
_engine: Optional[HybridRecommendationEngine] = None

def get_recommendation_engine() -> HybridRecommendationEngine:
    global _engine
    if _engine is None:
        _engine = HybridRecommendationEngine()
    return _engine
