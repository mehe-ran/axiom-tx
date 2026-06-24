import psycopg2
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

def settle_transaction(data):
    conn = None
    cursor = None
    try:
        conn = psycopg2.connect("dbname=axiom_tx user=postgres password=secret host=postgres")
        cursor = conn.cursor()
        cursor.execute(
            "INSERT INTO transactions (transaction_id, sender_id, receiver_id, amount) VALUES (%s, %s, %s, %s)",
            (data['transaction_id'], data['sender_id'], data['receiver_id'], data['amount'])
        )
        conn.commit()
        logger.info(f"Transaction {data['transaction_id']} settled.")
    except psycopg2.errors.UniqueViolation:
        if conn: conn.rollback()
        logger.warning(f"Duplicate transaction {data['transaction_id']} skipped.")
    finally:
        if cursor: cursor.close()
        if conn: conn.close()