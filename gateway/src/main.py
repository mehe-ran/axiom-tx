import logging
import os
from fastapi import FastAPI, HTTPException
from confluent_kafka import Producer
from .schemas import TransactionPayload
from shared.schemas import TransactionEvent

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="AxiomTx Gateway")

# resolve Kafka broker URI (defaults to local development port)
KAFKA_BROKER = os.getenv('KAFKA_BROKER_URL', 'localhost:29092')
producer = Producer({'bootstrap.servers': KAFKA_BROKER})


@app.post("/transfer")
async def create_transfer(payload: TransactionPayload):
    try:
        event = TransactionEvent(
            transaction_id=payload.transaction_id,
            sender_id=payload.sender_id,
            receiver_id=payload.receiver_id,
            amount=payload.amount,
            currency=payload.currency,
        )
        producer.produce(
            'tx_pending',
            key=event.transaction_id,
            value=event.model_dump_json().encode('utf-8')
        )
        producer.flush()

        logger.info(f"Ingested and routed transaction {event.transaction_id}")
        return {"status": "accepted", "transaction_id": event.transaction_id}

    except Exception as e:
        logger.error(f"Failed to route transaction: {e}")
        raise HTTPException(status_code=500, detail="Internal routing failure")