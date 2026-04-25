async def build_product_matching_runtime_dependencies(conn, *, matching_router_support_core, passport_updates_core, skin_passport_core, matching_helpers_core, skin_journal_core):
    return matching_router_support_core.build_matching_runtime_dependencies(
        conn,
        load_skin_passport_context=passport_updates_core.load_skin_passport_context,
        sanitize_skin_passport_answers=skin_passport_core.sanitize_skin_passport_answers,
        load_accepted_passport_insights=matching_helpers_core.load_accepted_passport_insights,
        load_user_skin_journal=skin_journal_core.load_user_skin_journal,
        load_product_function_signals=matching_helpers_core.load_product_function_signals,
        product_match_sort_key=matching_helpers_core.product_match_sort_key,
        filter_product_match_results=matching_helpers_core.filter_product_match_results,
    )


async def build_recommendation_runtime_dependencies(conn, *, matching_router_support_core, passport_updates_core, skin_passport_core, matching_helpers_core, skin_journal_core):
    return matching_router_support_core.build_matching_runtime_dependencies(
        conn,
        load_skin_passport_context=passport_updates_core.load_skin_passport_context,
        sanitize_skin_passport_answers=skin_passport_core.sanitize_skin_passport_answers,
        load_accepted_passport_insights=matching_helpers_core.load_accepted_passport_insights,
        load_user_skin_journal=skin_journal_core.load_user_skin_journal,
        load_product_function_signals=matching_helpers_core.load_product_function_signals,
        product_row_to_recommendation_input=matching_helpers_core.product_row_to_recommendation_input,
    )
