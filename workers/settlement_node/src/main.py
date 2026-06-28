import psycopg2
import logging
import json
import time
from confluent_kafka import Consumer, KafkaError

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# create a persistent database connection with a retry loop for safe startup
def get_db_connection():
    while True:
        try:
            conn = psycopg2.connect("dbname=axiom_tx user=postgres password=secret host=postgres")
            logger.info("connected to postgres successfully.")
            return conn
        except psycopg2.OperationalError:
            logger.warning("database not ready. retrying in 2 seconds...")
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
    # connect to the internal docker kafka broker
    conf = {
        'bootstrap.servers': 'kafka:9092',
        'group.id': 'settlement-group',
        'auto.offset.reset': 'earliest'
    }

    consumer = Consumer(conf)

    # updated topic to match the gateway and fx_node
    consumer.subscribe(['tx_pending'])

    logger.info("Settlement Node started. Listening for events...")

    # initialize the persistent connection before entering the consumer loop
    conn = get_db_connection()

    try:
        while True:
            # poll for new messages
            msg = consumer.poll(timeout=1.0)

            if msg is None:
                continue
            if msg.error():
                if msg.error().code() == KafkaError._PARTITION_EOF:
                    continue
                else:
                    logger.error(f"Kafka Error: {msg.error()}")
                    break

            # parse the kafka event and pass it to postgres using the open connection
            try:
                data = json.loads(msg.value().decode('utf-8'))
                settle_transaction(conn, data)
            except Exception as e:
                logger.error(f"Failed to process message: {e}")

    except KeyboardInterrupt:
        logger.info("Shutting down gracefully...")
    finally:
        consumer.close()
        if conn:
            conn.close()


if __name__ == "__main__":
    main()