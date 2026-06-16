from fastapi import FastAPI, BackgroundTasks
from pydantic import BaseModel
from .producer import produce_event
import uuid

app = FastAPI()

class TransferRequest(BaseModel):
    sender_id: str
    receiver_id: str
    amount: float
    currency: str

@app.post("/transfer")
async def create_transfer(request: TransferRequest):
    tx_id = str(uuid.uuid4())
    payload = {"transaction_id": tx_id, **request.dict()}
    produce_event("tx_pending", payload)
    return {"status": "accepted", "transaction_id": tx_id}