from typing import Optional


DICT_TABLE_MAP = {
    "brands": "brands",
    "categories": "categories",
    "segments": "segments",
    "volumes": "volumes",
    "procedureCategories": "procedure_categories",
    "contentCategories": "content_categories",
    "userRoles": "user_roles",
    "skin_types": "skin_types",
    "product_types": "product_types",
    "for_whom": "for_whom",
    "purposes": "purposes",
    "application_times": "application_times",
    "areas": "areas",
    "countries": "countries",
    "methodTypes": "procedure_method_types",
    "procedureDurations": "procedure_durations",
    "procedureEquipment": "procedure_equipment",
    "procedureZones": "procedure_zones",
    "procedureEffects": "procedure_effects",
    "procedureProblems": "procedure_problems",
}


def resolve_dict_table(key: str) -> Optional[str]:
    return DICT_TABLE_MAP.get(key)
