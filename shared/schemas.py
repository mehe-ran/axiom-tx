from pydantic import BaseModel

# base transaction schema for the event stream
class TransactionEvent(BaseModel):
    transaction_id: str
    sender_id: str
    receiver_id: str
    amount: float
    currency: str
    status: str = "pending"