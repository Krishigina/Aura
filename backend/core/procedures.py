import json
from typing import Any, Dict, List


def coerce_list_field(value: Any) -> List[str]:
    if value is None:
        return []
    if isinstance(value, list):
        return [str(item) for item in value if str(item).strip()]
    if isinstance(value, str):
        try:
            parsed = json.loads(value)
            if isinstance(parsed, list):
                return [str(item) for item in parsed if str(item).strip()]
        except json.JSONDecodeError:
            pass
        return [item.strip() for item in value.split(",") if item.strip()]
    return [str(value)]


def normalize_procedure_response(row: Any) -> Dict[str, Any]:
    procedure = dict(row)
    procedure["zones"] = coerce_list_field(procedure.get("zones"))
    procedure["effects"] = coerce_list_field(procedure.get("effects"))
    procedure["problems"] = coerce_list_field(procedure.get("problems"))
    return procedure


def procedure_select_sql(alias: str = "p") -> str:
    return f"""
        SELECT
            {alias}.id,
            {alias}.name,
            direction_dict.value AS direction,
            method_type_dict.value AS method_type,
            duration_dict.value AS duration,
            equipment_dict.value AS equipment,
            (
                SELECT json_agg(zone_dict.value ORDER BY zone_dict.value)
                FROM procedure_zone_links zone_links
                JOIN procedure_zones zone_dict ON zone_dict.id = zone_links.zone_id
                WHERE zone_links.procedure_id = {alias}.id
            ) AS zones,
            (
                SELECT json_agg(effect_dict.value ORDER BY effect_dict.value)
                FROM procedure_effect_links effect_links
                JOIN procedure_effects effect_dict ON effect_dict.id = effect_links.effect_id
                WHERE effect_links.procedure_id = {alias}.id
            ) AS effects,
            (
                SELECT json_agg(problem_dict.value ORDER BY problem_dict.value)
                FROM procedure_problem_links problem_links
                JOIN procedure_problems problem_dict ON problem_dict.id = problem_links.problem_id
                WHERE problem_links.procedure_id = {alias}.id
            ) AS problems,
            {alias}.description,
            {alias}.procedure_about,
            {alias}.advantages,
            {alias}.indications,
            {alias}.principle,
            {alias}.how_it_goes,
            for_whom_dict.value AS for_whom,
            {alias}.problems_solved,
            {alias}.preparation,
            {alias}.recommended_course,
            {alias}.rehabilitation,
            {alias}.post_care,
            {alias}.side_effects,
            {alias}.created_at,
            {alias}.contraindications_full,
            {alias}.direction_id,
            {alias}.method_type_id,
            {alias}.duration_id,
            {alias}.equipment_id,
            {alias}.for_whom_id
        FROM procedures {alias}
        LEFT JOIN procedure_categories direction_dict ON direction_dict.id = {alias}.direction_id
        LEFT JOIN procedure_method_types method_type_dict ON method_type_dict.id = {alias}.method_type_id
        LEFT JOIN procedure_durations duration_dict ON duration_dict.id = {alias}.duration_id
        LEFT JOIN procedure_equipment equipment_dict ON equipment_dict.id = {alias}.equipment_id
        LEFT JOIN for_whom for_whom_dict ON for_whom_dict.id = {alias}.for_whom_id
    """
