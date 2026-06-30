import json
import logging
import os
from fastapi import FastAPI, HTTPException
from confluent_kafka import Producer
from .schemas import TransactionPayload

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="AxiomTx Gateway")

# resolve Kafka broker URI (defaults to local development port)
KAFKA_BROKER = os.getenv('KAFKA_BROKER_URL', 'localhost:29092')
producer = Producer({'bootstrap.servers': KAFKA_BROKER})


@app.post("/transfer")
async def create_transfer(payload: TransactionPayload):
    try:
        producer.produce(
            'tx_pending',
            key=payload.transaction_id,
            value=json.dumps(payload.dict()).encode('utf-8')
        )
        producer.flush()

        logger.info(f"Ingested and routed transaction {payload.transaction_id}")
        return {"status": "accepted", "transaction_id": payload.transaction_id}

    except Exception as e:
        logger.error(f"Failed to route transaction: {e}")
        raise HTTPException(status_code=500, detail="Internal routing failure")