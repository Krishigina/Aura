from typing import Any, Dict, List, Optional

from pydantic import BaseModel, Field


class SkinPassportUpdateRequest(BaseModel):
    answers: Dict[str, List[str]]
    completed_at_epoch_millis: Optional[int] = None


class SkinPassportResponse(BaseModel):
    completed_at_epoch_millis: Optional[int] = None
    answers: Dict[str, List[str]] = {}


class SkinJournalSettingsUpdate(BaseModel):
    has_sensor: Optional[bool] = None
    push_enabled: Optional[bool] = None
    sensor_reminder_schedule: Optional[str] = None


class ProcedureZoneAmount(BaseModel):
    zone: str
    amount: str


class SkinJournalProcedureCreate(BaseModel):
    catalog_procedure_id: int
    procedure_name: str
    performed_at: str
    zones: List[str]
    zone_amounts: List[ProcedureZoneAmount] = Field(default_factory=list)
    preparation_name: Optional[str] = None
    clinic_or_doctor: Optional[str] = None
    note: Optional[str] = None
    repeat_due_at: Optional[str] = None
    post_care_tasks: List[str] = Field(default_factory=list)
    photos: List[str] = Field(default_factory=list)


class SkinJournalSensorReadingCreate(BaseModel):
    measured_at: str
    zone: str
    percent_value: int = Field(ge=0, le=100)
    hydration: int = Field(ge=1, le=5)
    oiliness: int = Field(ge=1, le=5)
    softness: int = Field(ge=1, le=5)


class SkinJournalReminderAction(BaseModel):
    action: str
    rescheduled_due_at: Optional[str] = None


class PassportSuggestionCreate(BaseModel):
    suggestion_type: str
    target_field: Optional[str] = None
    old_value: Optional[Dict[str, Any]] = None
    proposed_value: Dict[str, Any]
    source_type: str
    source_message_id: Optional[int] = None
    evidence_text: str = ""
    confidence: float = 0
    conflict_status: str = "none"


class PassportSuggestionUpdate(BaseModel):
    status: str
