from backend.core.entity_dictionary_refs import (
    backfill_content_category_refs,
    backfill_content_tag_refs,
    backfill_recommendation_feedback_product_refs,
    backfill_user_role_refs,
)
from backend.core.product_dictionary_refs import backfill_product_dictionary_refs
from backend.core.procedure_dictionary_refs import backfill_procedure_dictionary_refs
from backend.db.schema_runtime import ensure_knowledge_schema
from backend.db.schema_specs import (
    EXTRA_REFERENCE_CONSTRAINT_STATEMENTS,
    FK_SUPPORT_INDEX_STATEMENTS,
    INGREDIENT_EVIDENCE_CLEANUP_STATEMENTS,
    INGREDIENT_EVIDENCE_MIGRATION_STATEMENTS,
    OBSOLETE_NORMALIZED_DROP_STATEMENTS,
    PROCEDURE_REFERENCE_CONSTRAINTS,
    PRODUCT_REFERENCE_CONSTRAINTS,
    USER_COLUMNS_TO_ADD,
    USER_CONTENT_REFERENCE_CONSTRAINTS,
)
from backend.db.schema_sql import MAIN_SCHEMA_SQL


async def initialize_database_schema(conn):
    await conn.execute(MAIN_SCHEMA_SQL)
    await conn.execute(
        """
        DO $$
        BEGIN
            IF EXISTS (
                SELECT 1
                FROM information_schema.columns
                WHERE table_name = 'procedures' AND column_name = 'contraindications'
            ) THEN
                UPDATE procedures
                SET contraindications_full = contraindications
                WHERE contraindications_full IS NULL AND contraindications IS NOT NULL;
            END IF;
        END $$;
        """
    )
    await conn.execute("ALTER TABLE users DROP COLUMN IF EXISTS phone")

    for statement in INGREDIENT_EVIDENCE_MIGRATION_STATEMENTS:
        await conn.execute(statement)
    for statement in INGREDIENT_EVIDENCE_CLEANUP_STATEMENTS:
        await conn.execute(statement)
    await ensure_knowledge_schema(conn)
    await conn.execute("ALTER TABLE recommendation_feedback ADD COLUMN IF NOT EXISTS product_ref_id INTEGER")
    for table_name, constraint_name, column_name, dictionary_table in USER_CONTENT_REFERENCE_CONSTRAINTS:
        await conn.execute(
            f"""
            DO $$
            BEGIN
                IF NOT EXISTS (
                    SELECT 1
                    FROM pg_constraint
                    WHERE conname = '{constraint_name}'
                ) THEN
                    ALTER TABLE {table_name}
                    ADD CONSTRAINT {constraint_name}
                    FOREIGN KEY ({column_name}) REFERENCES {dictionary_table}(id) ON DELETE SET NULL;
                END IF;
            END $$;
            """
        )
    for constraint_name, column_name, dictionary_table in PRODUCT_REFERENCE_CONSTRAINTS:
        await conn.execute(
            f"""
            DO $$
            BEGIN
                IF NOT EXISTS (
                    SELECT 1
                    FROM pg_constraint
                    WHERE conname = '{constraint_name}'
                ) THEN
                    ALTER TABLE products
                    ADD CONSTRAINT {constraint_name}
                    FOREIGN KEY ({column_name}) REFERENCES {dictionary_table}(id) ON DELETE SET NULL;
                END IF;
            END $$;
            """
        )
    for constraint_name, column_name, dictionary_table in PROCEDURE_REFERENCE_CONSTRAINTS:
        await conn.execute(
            f"""
            DO $$
            BEGIN
                IF NOT EXISTS (
                    SELECT 1
                    FROM pg_constraint
                    WHERE conname = '{constraint_name}'
                ) THEN
                    ALTER TABLE procedures
                    ADD CONSTRAINT {constraint_name}
                    FOREIGN KEY ({column_name}) REFERENCES {dictionary_table}(id) ON DELETE SET NULL;
                END IF;
            END $$;
            """
        )
    for statement in EXTRA_REFERENCE_CONSTRAINT_STATEMENTS:
        await conn.execute(statement)
    for index_sql in FK_SUPPORT_INDEX_STATEMENTS:
        await conn.execute(index_sql)
    await conn.execute("ALTER TABLE ingredients ADD COLUMN IF NOT EXISTS ingredient_class VARCHAR(100)")
    await conn.execute("ALTER TABLE ingredients ADD COLUMN IF NOT EXISTS evidence_status VARCHAR(50) DEFAULT 'auto_created'")
    await backfill_user_role_refs(conn)
    await backfill_content_category_refs(conn)
    await backfill_content_tag_refs(conn)
    await backfill_recommendation_feedback_product_refs(conn)
    await backfill_product_dictionary_refs(conn)
    await backfill_procedure_dictionary_refs(conn)

    try:
        await conn.execute("ALTER TABLE users OWNER TO aura_user")
    except Exception as error:
        print(f"Could not change ownership: {error}")

    existing_columns = await conn.fetch("SELECT column_name FROM information_schema.columns WHERE table_name = 'users'")
    existing_column_names = {row["column_name"] for row in existing_columns}
    for column_name, column_type in USER_COLUMNS_TO_ADD:
        if column_name not in existing_column_names:
            try:
                await conn.execute(f"ALTER TABLE users ADD COLUMN {column_name} {column_type}")
            except Exception as error:
                print(f"Could not add column {column_name}: {error}")


async def backfill_normalized_reference_data(conn):
    await backfill_user_role_refs(conn)
    await backfill_content_category_refs(conn)
    await backfill_content_tag_refs(conn)
    await backfill_recommendation_feedback_product_refs(conn)
    await backfill_product_dictionary_refs(conn)
    await backfill_procedure_dictionary_refs(conn)


async def drop_obsolete_normalized_columns(conn):
    for statement in OBSOLETE_NORMALIZED_DROP_STATEMENTS:
        await conn.execute(statement)
