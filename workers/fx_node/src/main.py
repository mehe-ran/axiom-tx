import logging
from confluent_kafka import Consumer
from shared.schemas import TransactionEvent

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)
logger.info("FX Service started...")

consumer = Consumer({'bootstrap.servers': 'kafka:9092', 'group.id': 'fx-group'})
consumer.subscribe(['tx_pending'])

while True:
    msg = consumer.poll(1.0)
    if msg is None: continue
    try:
        event = TransactionEvent.model_validate_json(msg.value())
        logger.info(f"Processing transaction {event.transaction_id}: {event.amount} {event.currency}")
    except Exception as e:
        logger.error(f"Failed to parse message as TransactionEvent: {e}")