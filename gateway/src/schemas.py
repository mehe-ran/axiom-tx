from pydantic import BaseModel, Field, validator
import uuid

class TransactionPayload(BaseModel):
    transaction_id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    sender_id: str = Field(..., min_length=4)
    receiver_id: str = Field(..., min_length=4)
    amount: float = Field(..., gt=0, description="Amount must be strictly positive")
    currency: str = Field(default="USD", max_length=3)

    @validator('receiver_id')
    def accounts_must_differ(cls, v, values):
        # sender and receiver cannot be identical
        if 'sender_id' in values and v == values['sender_id']:
            raise ValueError('Sender and receiver cannot be the same')
        return v