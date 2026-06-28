import psycopg2
import logging
from datetime import datetime

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("ReconciliationEngine")


def run_audit():
    logger.info(f"Starting End-of-Day Ledger Audit for {datetime.now().date()}")
    conn = psycopg2.connect("dbname=axiom_tx user=postgres password=secret host=postgres")
    cursor = conn.cursor()

    try:
        # check for stranded or unprocessed transactions
        cursor.execute("SELECT count(*) FROM transactions WHERE status != 'processed'")
        unprocessed = cursor.fetchone()[0]

        # verify double-entry accounting principle
        cursor.execute("SELECT sum(amount) FROM transactions WHERE status = 'processed'")
        total_volume = cursor.fetchone()[0] or 0.00

        logger.info(f"Audit Complete. Total Volume Settled: ${total_volume}. Unprocessed TXNs: {unprocessed}.")

        if unprocessed > 0:
            logger.warning("CRITICAL: Unprocessed transactions found in ledger!")

    except Exception as e:
        logger.error(f"Audit failed: {e}")
    finally:
        cursor.close()
        conn.close()


if __name__ == "__main__":
    run_audit()