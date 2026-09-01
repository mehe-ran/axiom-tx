import os
import psycopg2
import logging
import json
import time
from confluent_kafka import Consumer, Producer, KafkaError
from shared.schemas import TransactionEvent

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

dlq_producer = Producer({'bootstrap.servers': 'kafka:9092'})

def route_to_dlq(transaction_data, error_reason):
    try:
        payload = {
            "original_data": transaction_data,
            "error": str(error_reason)
        }
        dlq_producer.produce(
            'tx_dead_letter',
            value=json.dumps(payload).encode('utf-8')
        )
        dlq_producer.flush()
        logger.warning("Routed failed transaction to DLQ.")
    except Exception as e:
        logger.error(f"DLQ Routing failed: {e}")

def get_db_connection():
    dsn = (
        f"dbname={os.getenv('POSTGRES_DB', 'axiom_tx')} "
        f"user={os.getenv('POSTGRES_USER', 'postgres')} "
        f"password={os.getenv('POSTGRES_PASSWORD', 'secret')} "
        f"host={os.getenv('POSTGRES_HOST', 'postgres')}"
    )
    while True:
        try:
            conn = psycopg2.connect(dsn)
            logger.info("Connected to postgres successfully.")
            return conn
        except psycopg2.OperationalError:
            logger.warning("Database not ready. Retrying in 2 seconds...")
            time.sleep(2)

def settle_transaction(conn, event: TransactionEvent):
    cursor = None
    try:
        cursor = conn.cursor()
        cursor.execute(
            "INSERT INTO transactions (transaction_id, sender_id, receiver_id, amount) VALUES (%s, %s, %s, %s)",
            (event.transaction_id, event.sender_id, event.receiver_id, event.amount)
        )
        conn.commit()
        logger.info(f"Transaction {event.transaction_id} settled.")
    except psycopg2.errors.UniqueViolation:
        conn.rollback()
        logger.warning(f"Duplicate transaction {event.transaction_id} skipped.")
    except psycopg2.errors.ForeignKeyViolation:
        conn.rollback()
        logger.warning(f"Foreign Key missing for {event.transaction_id} - check accounts table.")
    finally:
        if cursor:
            cursor.close()

def main():
    conf = {
        'bootstrap.servers': 'kafka:9092',
        'group.id': 'settlement-group',
        'auto.offset.reset': 'earliest'
    }

    consumer = Consumer(conf)
    consumer.subscribe(['tx_pending'])

    logger.info("Settlement Node started. Listening for events...")

    conn = get_db_connection()

    try:
        while True:
            msg = consumer.poll(timeout=1.0)

            if msg is None:
                continue
            if msg.error():
                if msg.error().code() == KafkaError._PARTITION_EOF:
                    continue
                else:
                    logger.error(f"Kafka Error: {msg.error()}")
                    break

            try:
                event = TransactionEvent.model_validate_json(msg.value())
                settle_transaction(conn, event)
            except Exception as e:
                logger.error(f"Failed to process message: {e}")
                raw_data = msg.value().decode('utf-8') if msg.value() else None
                route_to_dlq(raw_data, e)

    except KeyboardInterrupt:
        logger.info("Shutting down gracefully...")
    finally:
        consumer.close()
        if conn:
            conn.close()

if __name__ == "__main__":
    main()