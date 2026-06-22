import logging
from confluent_kafka import Consumer

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)
logger.info("FX Service started...")

consumer = Consumer({'bootstrap.servers': 'kafka:9092', 'group.id': 'fx-group'})
consumer.subscribe(['tx_pending'])

while True:
    msg = consumer.poll(1.0)
    if msg is None: continue
    logger.info(f"Processing message: {msg.value()}")