from backend.db.schema_bootstrap import (
    backfill_normalized_reference_data,
    drop_obsolete_normalized_columns,
    initialize_database_schema,
)
from backend.db.schema_runtime import ensure_knowledge_schema
from backend.db.schema_sql import MAIN_SCHEMA_SQL
