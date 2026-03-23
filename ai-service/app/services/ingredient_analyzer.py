from typing import List, Dict, Any, Optional, Dict as TypeDict

class IngredientAnalyzer:
    """Analyze cosmetic ingredients for safety and compatibility"""
    
    # Safety database
    INGREDIENT_DB = {
        "water": {"name": "Water", "inci": "Aqua", "safety": "SAFE", "category": "SOLVENT", "benefits": ["base"], "risks": []},
        "glycerin": {"name": "Glycerin", "inci": "Glycerin", "safety": "SAFE", "category": "HUMECTANT", "benefits": ["увлажнение"], "risks": []},
        "niacinamide": {"name": "Niacinamide", "inci": "Niacinamide", "safety": "SAFE", "category": "VITAMIN", "benefits": ["осветление", "укрепление"], "risks": []},
        "hyaluronic acid": {"name": "Hyaluronic Acid", "inci": "Sodium Hyaluronate", "safety": "SAFE", "category": "HUMECTANT", "benefits": ["глубокое увлажнение"], "risks": []},
        "ceramides": {"name": "Ceramides", "inci": "Ceramide NP", "safety": "SAFE", "category": "LIPID", "benefits": ["восстановление барьера"], "risks": []},
        "panthenol": {"name": "Panthenol", "inci": "Panthenol", "safety": "SAFE", "category": "VITAMIN", "benefits": ["успокоение", "заживление"], "risks": []},
        "retinol": {"name": "Retinol", "inci": "Retinol", "safety": "CAUTION", "category": "VITAMIN", "benefits": ["анти-эйдж"], "risks": ["раздражение", "фоточувствительность", "не для беременных"]},
        "salicylic acid": {"name": "Salicylic Acid", "inci": "Salicylic Acid", "safety": "MODERATE", "category": "ACID", "benefits": ["отшелушивание", "акне"], "risks": ["сухость"]},
        "aha": {"name": "AHA", "inci": "Alpha Hydroxy Acid", "safety": "MODERATE", "category": "ACID", "benefits": ["отшелушивание"], "risks": ["раздражение"]},
        "benzoyl peroxide": {"name": "Benzoyl Peroxide", "inci": "Benzoyl Peroxide", "safety": "MODERATE", "category": "ANTIBACTERIAL", "benefits": ["лечение акне"], "risks": ["сухость", "отбеливание"]},
        "vitamin c": {"name": "Vitamin C", "inci": "Ascorbic Acid", "safety": "MODERATE", "category": "VITAMIN", "benefits": ["антиоксидант", "осветление"], "risks": ["нестабильный"]},
        "fragrance": {"name": "Fragrance", "inci": "Parfum", "safety": "CAUTION", "category": "FRAGRANCE", "benefits": ["аромат"], "risks": ["аллергия", "раздражение"]},
        "alcohol": {"name": "Alcohol", "inci": "Alcohol Denat", "safety": "CAUTION", "category": "SOLVENT", "benefits": ["быстрое высыхание"], "risks": ["сухость", "раздражение"]},
        "parabens": {"name": "Parabens", "inci": "Parabens", "safety": "AVOID", "category": "PRESERVATIVE", "benefits": ["консервант"], "risks": ["эндокринные нарушения"]},
        "synthetic colors": {"name": "Synthetic Colors", "inci": "CI 19140", "safety": "AVOID", "category": "COLOR", "benefits": ["цвет"], "risks": ["аллергия"]},
    }
    
    # Known conflicts
    CONFLICTS = [
        (["retinol", "vitamin c"], "Не использовать вместе - раздражает"),
        (["retinol", "aha"], "Может вызвать сильное раздражение"),
        (["benzoyl peroxide", "retinol"], "Снижает эффективность"),
        (["benzoyl peroxide", "vitamin c"], "Окисляет витамин C"),
    ]
    
    def analyze(self, ingredients: List[str]) -> Dict[str, Any]:
        """Analyze list of ingredients"""
        results = []
        safety_counts = {"SAFE": 0, "MODERATE": 0, "CAUTION": 0, "AVOID": 0}
        warnings = []
        
        for ing in ingredients:
            info = self._get_ingredient_info(ing)
            if info:
                results.append(info)
                safety_counts[info["safety"]] += 1
                
                if info["safety"] in ["CAUTION", "AVOID"]:
                    for risk in info["risks"][:1]:
                        warnings.append(f"{info['name']}: {risk}")
        
        # Check conflicts
        conflicts = self._check_conflicts(ingredients)
        if conflicts:
            warnings.extend(conflicts)
        
        # Calculate overall score
        total = len(results)
        if total > 0:
            score = (safety_counts["SAFE"] * 100 + safety_counts["MODERATE"] * 70 + 
                    safety_counts["CAUTION"] * 30 + safety_counts["AVOID"] * 0) / total
        else:
            score = 100
        
        return {
            "ingredients": results,
            "total_count": total,
            "safe_count": safety_counts["SAFE"],
            "moderate_count": safety_counts["MODERATE"],
            "caution_count": safety_counts["CAUTION"],
            "avoid_count": safety_counts["AVOID"],
            "overall_score": round(score, 1),
            "warnings": warnings
        }
    
    def _get_ingredient_info(self, name: str) -> Optional[Dict[str, Any]]:
        """Get ingredient info"""
        key = name.lower().strip()
        
        # Direct match
        if key in self.INGREDIENT_DB:
            return self.INGREDIENT_DB[key]
        
        # Partial match
        for db_key, info in self.INGREDIENT_DB.items():
            if db_key in key or key in db_key:
                return info
        
        # Default - unknown
        return {
            "name": name,
            "inci": name,
            "safety": "MODERATE", 
            "category": "UNKNOWN",
            "benefits": [],
            "risks": ["неизвестно"]
        }
    
    def _check_conflicts(self, ingredients: List[str]) -> List[str]:
        """Check for ingredient conflicts"""
        warnings = []
        ing_lower = [i.lower() for i in ingredients]
        
        for conflict_ings, reason in self.CONFLICTS:
            if all(c in ing_lower for c in conflict_ings):
                warnings.append(f"Конфликт: {', '.join(conflict_ings)} - {reason}")
        
        return warnings
    
    def check_skin_type_compatibility(self, ingredients: List[str], skin_type: str) -> Dict[str, Any]:
        """Check if ingredients are suitable for skin type"""
        warnings = []
        
        skin_type = skin_type.upper()
        caution_ingredients = {
            "SENSITIVE": ["alcohol", "fragrance", "aha", "salicylic acid", "retinol"],
            "DRY": ["alcohol", "salicylic acid"],
            "OILY": [],
        }
        
        if skin_type in caution_ingredients:
            for ing in ingredients:
                ing_lower = ing.lower()
                for caution in caution_ingredients[skin_type]:
                    if caution in ing_lower:
                        warnings.append(f"{ing} может быть слишком агрессивен для {skin_type.lower()} кожи")
        
        return {"compatible": len(warnings) == 0, "warnings": warnings}


# Singleton
_analyzer: Optional[IngredientAnalyzer] = None

def get_ingredient_analyzer() -> IngredientAnalyzer:
    global _analyzer
    if _analyzer is None:
        _analyzer = IngredientAnalyzer()
    return _analyzer
