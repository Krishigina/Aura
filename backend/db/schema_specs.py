KNOWLEDGE_SCHEMA_ALTERS = [
    "ALTER TABLE knowledge_sources ADD COLUMN IF NOT EXISTS filename VARCHAR(255)",
    "ALTER TABLE knowledge_sources ADD COLUMN IF NOT EXISTS content TEXT DEFAULT ''",
    "ALTER TABLE knowledge_sources ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT NOW()",
    "ALTER TABLE ingredients ADD COLUMN IF NOT EXISTS ingredient_class VARCHAR(100)",
    "ALTER TABLE ingredients ADD COLUMN IF NOT EXISTS evidence_status VARCHAR(50) DEFAULT 'auto_created'",
    "ALTER TABLE ingredient_evidence ADD COLUMN IF NOT EXISTS matching_effect VARCHAR(50)",
    "ALTER TABLE ingredient_evidence ADD COLUMN IF NOT EXISTS matching_weight_delta DOUBLE PRECISION",
    "ALTER TABLE ingredient_evidence ADD COLUMN IF NOT EXISTS matching_condition_type VARCHAR(100)",
    "ALTER TABLE ingredient_evidence ALTER COLUMN direction DROP NOT NULL",
    "ALTER TABLE ingredient_evidence ALTER COLUMN strength DROP NOT NULL",
    "ALTER TABLE ingredient_evidence ALTER COLUMN summary DROP NOT NULL",
]


INGREDIENT_EVIDENCE_MIGRATION_STATEMENTS = [
    "ALTER TABLE brands ADD COLUMN IF NOT EXISTS description TEXT",
    "ALTER TABLE brands ADD COLUMN IF NOT EXISTS country VARCHAR(100)",
    "ALTER TABLE brands ADD COLUMN IF NOT EXISTS country_origin VARCHAR(100)",
    "ALTER TABLE brands ADD COLUMN IF NOT EXISTS manufacturer TEXT",
    "ALTER TABLE ingredients ADD COLUMN IF NOT EXISTS evidence_status VARCHAR(50) DEFAULT 'draft'",
    "ALTER TABLE ingredient_evidence ADD COLUMN IF NOT EXISTS ingredient_id INTEGER",
    "ALTER TABLE ingredient_evidence ADD COLUMN IF NOT EXISTS ingredient_key VARCHAR(255)",
    "ALTER TABLE ingredient_evidence ADD COLUMN IF NOT EXISTS fact_type VARCHAR(50)",
    "ALTER TABLE ingredient_evidence ADD COLUMN IF NOT EXISTS effect_key VARCHAR(100)",
    "ALTER TABLE ingredient_evidence ADD COLUMN IF NOT EXISTS condition_type VARCHAR(100)",
    "ALTER TABLE ingredient_evidence ADD COLUMN IF NOT EXISTS condition_value VARCHAR(255)",
    "ALTER TABLE ingredient_evidence ADD COLUMN IF NOT EXISTS matching_effect VARCHAR(50)",
    "ALTER TABLE ingredient_evidence ADD COLUMN IF NOT EXISTS matching_weight_delta DOUBLE PRECISION DEFAULT 0",
    "ALTER TABLE ingredient_evidence ADD COLUMN IF NOT EXISTS evidence_status VARCHAR(50) DEFAULT 'draft'",
    "ALTER TABLE ingredient_evidence ADD COLUMN IF NOT EXISTS confidence DOUBLE PRECISION DEFAULT 0",
    "ALTER TABLE ingredient_evidence ALTER COLUMN direction DROP NOT NULL",
    "ALTER TABLE ingredient_evidence ALTER COLUMN strength DROP NOT NULL",
    "ALTER TABLE ingredient_evidence ALTER COLUMN summary DROP NOT NULL",
]


INGREDIENT_EVIDENCE_CLEANUP_STATEMENTS = [
    """
    UPDATE ingredient_evidence
    SET effect_key=COALESCE(effect_key, ''),
        condition_type=COALESCE(condition_type, ''),
        condition_value=COALESCE(condition_value, ''),
        evidence_quote=COALESCE(evidence_quote, '')
    """,
    """
    DELETE FROM ingredient_evidence
    WHERE ctid IN (
        SELECT ctid
        FROM (
            SELECT ctid,
                   ROW_NUMBER() OVER (
                       PARTITION BY ingredient_id, source_id, effect_key, condition_type, condition_value, evidence_quote
                       ORDER BY id
                   ) AS row_number
            FROM ingredient_evidence
            WHERE ingredient_id IS NOT NULL AND source_id IS NOT NULL
        ) duplicates
        WHERE row_number > 1
    )
    """,
    """
    CREATE UNIQUE INDEX IF NOT EXISTS idx_ingredient_evidence_extracted_identity
    ON ingredient_evidence(ingredient_id, source_id, effect_key, condition_type, condition_value, evidence_quote)
    """,
]


USER_CONTENT_REFERENCE_CONSTRAINTS = [
    ("users", "users_role_id_fkey", "role_id", "user_roles"),
    ("content", "content_category_id_fkey", "category_id", "content_categories"),
]


PRODUCT_REFERENCE_CONSTRAINTS = [
    ("products_brand_id_fkey", "brand_id", "brands"),
    ("products_category_id_fkey", "category_id", "categories"),
    ("products_segment_id_fkey", "segment_id", "segments"),
    ("products_volume_id_fkey", "volume_id", "volumes"),
    ("products_product_type_id_fkey", "product_type_id", "product_types"),
    ("products_for_whom_id_fkey", "for_whom_id", "for_whom"),
    ("products_application_time_id_fkey", "application_time_id", "application_times"),
    ("products_area_id_fkey", "area_id", "areas"),
    ("products_country_id_fkey", "country_id", "countries"),
]


PROCEDURE_REFERENCE_CONSTRAINTS = [
    ("procedures_direction_id_fkey", "direction_id", "procedure_categories"),
    ("procedures_method_type_id_fkey", "method_type_id", "procedure_method_types"),
    ("procedures_duration_id_fkey", "duration_id", "procedure_durations"),
    ("procedures_equipment_id_fkey", "equipment_id", "procedure_equipment"),
    ("procedures_for_whom_id_fkey", "for_whom_id", "for_whom"),
]


EXTRA_REFERENCE_CONSTRAINT_STATEMENTS = [
    """
    DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'ingredient_evidence_ingredient_id_fkey'
        ) THEN
            ALTER TABLE ingredient_evidence
            ADD CONSTRAINT ingredient_evidence_ingredient_id_fkey
            FOREIGN KEY (ingredient_id) REFERENCES ingredients(id) ON DELETE CASCADE;
        END IF;
    END $$;
    """,
    """
    DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'passport_update_suggestions_source_message_id_fkey'
        ) THEN
            ALTER TABLE passport_update_suggestions
            ADD CONSTRAINT passport_update_suggestions_source_message_id_fkey
            FOREIGN KEY (source_message_id) REFERENCES chat_messages(id) ON DELETE SET NULL;
        END IF;
    END $$;
    """,
    """
    DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'recommendation_feedback_product_ref_id_fkey'
        ) THEN
            ALTER TABLE recommendation_feedback
            ADD CONSTRAINT recommendation_feedback_product_ref_id_fkey
            FOREIGN KEY (product_ref_id) REFERENCES products(id) ON DELETE SET NULL;
        END IF;
    END $$;
    """,
]


FK_SUPPORT_INDEX_STATEMENTS = [
    "CREATE INDEX IF NOT EXISTS idx_users_role_id ON users(role_id)",
    "CREATE INDEX IF NOT EXISTS idx_content_category_id ON content(category_id)",
    "CREATE INDEX IF NOT EXISTS idx_content_tag_links_tag_id ON content_tag_links(tag_id)",
    "CREATE INDEX IF NOT EXISTS idx_recommendation_feedback_product_ref_id ON recommendation_feedback(product_ref_id)",
    "CREATE INDEX IF NOT EXISTS idx_products_brand_id ON products(brand_id)",
    "CREATE INDEX IF NOT EXISTS idx_products_category_id ON products(category_id)",
    "CREATE INDEX IF NOT EXISTS idx_products_segment_id ON products(segment_id)",
    "CREATE INDEX IF NOT EXISTS idx_products_volume_id ON products(volume_id)",
    "CREATE INDEX IF NOT EXISTS idx_products_product_type_id ON products(product_type_id)",
    "CREATE INDEX IF NOT EXISTS idx_products_for_whom_id ON products(for_whom_id)",
    "CREATE INDEX IF NOT EXISTS idx_products_application_time_id ON products(application_time_id)",
    "CREATE INDEX IF NOT EXISTS idx_products_area_id ON products(area_id)",
    "CREATE INDEX IF NOT EXISTS idx_products_country_id ON products(country_id)",
    "CREATE INDEX IF NOT EXISTS idx_procedures_direction_id ON procedures(direction_id)",
    "CREATE INDEX IF NOT EXISTS idx_procedures_method_type_id ON procedures(method_type_id)",
    "CREATE INDEX IF NOT EXISTS idx_procedures_duration_id ON procedures(duration_id)",
    "CREATE INDEX IF NOT EXISTS idx_procedures_equipment_id ON procedures(equipment_id)",
    "CREATE INDEX IF NOT EXISTS idx_procedures_for_whom_id ON procedures(for_whom_id)",
    "CREATE INDEX IF NOT EXISTS idx_product_purpose_links_purpose_id ON product_purpose_links(purpose_id)",
    "CREATE INDEX IF NOT EXISTS idx_product_skin_type_links_skin_type_id ON product_skin_type_links(skin_type_id)",
    "CREATE INDEX IF NOT EXISTS idx_procedure_zone_links_zone_id ON procedure_zone_links(zone_id)",
    "CREATE INDEX IF NOT EXISTS idx_procedure_effect_links_effect_id ON procedure_effect_links(effect_id)",
    "CREATE INDEX IF NOT EXISTS idx_procedure_problem_links_problem_id ON procedure_problem_links(problem_id)",
]


USER_COLUMNS_TO_ADD = [
    ("nickname", "VARCHAR(255)"),
    ("phone", "VARCHAR(50)"),
    ("password_hash", "VARCHAR(255)"),
]


OBSOLETE_NORMALIZED_DROP_STATEMENTS = [
    "ALTER TABLE users DROP COLUMN IF EXISTS role",
    "ALTER TABLE content DROP COLUMN IF EXISTS category",
    "ALTER TABLE content DROP COLUMN IF EXISTS tags",
    "ALTER TABLE products DROP COLUMN IF EXISTS brand",
    "ALTER TABLE products DROP COLUMN IF EXISTS category",
    "ALTER TABLE products DROP COLUMN IF EXISTS segment",
    "ALTER TABLE products DROP COLUMN IF EXISTS volume",
    "ALTER TABLE products DROP COLUMN IF EXISTS product_type",
    "ALTER TABLE products DROP COLUMN IF EXISTS for_whom",
    "ALTER TABLE products DROP COLUMN IF EXISTS purpose",
    "ALTER TABLE products DROP COLUMN IF EXISTS skin_type",
    "ALTER TABLE products DROP COLUMN IF EXISTS application_time",
    "ALTER TABLE products DROP COLUMN IF EXISTS area",
    "ALTER TABLE products DROP COLUMN IF EXISTS country",
    "ALTER TABLE procedures DROP COLUMN IF EXISTS direction",
    "ALTER TABLE procedures DROP COLUMN IF EXISTS method_type",
    "ALTER TABLE procedures DROP COLUMN IF EXISTS duration",
    "ALTER TABLE procedures DROP COLUMN IF EXISTS equipment",
    "ALTER TABLE procedures DROP COLUMN IF EXISTS zones",
    "ALTER TABLE procedures DROP COLUMN IF EXISTS effects",
    "ALTER TABLE procedures DROP COLUMN IF EXISTS problems",
    "ALTER TABLE procedures DROP COLUMN IF EXISTS for_whom",
    "ALTER TABLE procedures DROP COLUMN IF EXISTS contraindications",
]
