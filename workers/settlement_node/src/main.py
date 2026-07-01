import psycopg2
import logging
import json
import time
from confluent_kafka import Consumer, Producer, KafkaError

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
    while True:
        try:
            conn = psycopg2.connect("dbname=axiom_tx user=postgres password=secret host=postgres")
            logger.info("Connected to postgres successfully.")
            return conn
        except psycopg2.OperationalError:
            logger.warning("Database not ready. Retrying in 2 seconds...")
            time.sleep(2)

def settle_transaction(conn, data):
    cursor = None
    try:
        cursor = conn.cursor()
        cursor.execute(
            "INSERT INTO transactions (transaction_id, sender_id, receiver_id, amount) VALUES (%s, %s, %s, %s)",
            (data['transaction_id'], data['sender_id'], data['receiver_id'], data['amount'])
        )
        conn.commit()
        logger.info(f"Transaction {data['transaction_id']} settled.")
    except psycopg2.errors.UniqueViolation:
        conn.rollback()
        logger.warning(f"Duplicate transaction {data['transaction_id']} skipped.")
    except psycopg2.errors.ForeignKeyViolation:
        conn.rollback()
        logger.warning(f"Foreign Key missing for {data['transaction_id']} - check accounts table.")
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
                data = json.loads(msg.value().decode('utf-8'))
                settle_transaction(conn, data)
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