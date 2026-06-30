import json
import logging
from confluent_kafka import Consumer

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


def check_velocity(tx_data):
    # placeholder for geographical velocity check
    return True


def detect_fraud(transaction_data):
    amount = float(transaction_data.get('amount', 0))
    sender = transaction_data.get('sender_id', '')

    # rule 1: hard limits
    if amount > 10000.00:
        return True, "Amount exceeds 10k threshold"

    # rule 2: blocklisted accounts
    blocklist = ["acc_9999", "acc_0000", "acc_6666"]
    if sender in blocklist:
        return True, "Sender is on high-risk blocklist"

    return False, "Clear"


def main():
    consumer = Consumer({
        'bootstrap.servers': 'kafka:9092',
        'group.id': 'fraud-group',
        'auto.offset.reset': 'earliest'
    })

    # updated to listen to the same ingestion stream as the settlement node
    consumer.subscribe(['tx_pending'])
    logger.info("Fraud Detection Node initialized. Listening for events...")

    while True:
        msg = consumer.poll(1.0)
        if msg is None:
            continue

        try:
            # parse the incoming kafka message
            data = json.loads(msg.value().decode('utf-8'))

            # run the checks
            velocity_pass = check_velocity(data)
            is_fraud, reason = detect_fraud(data)

            if is_fraud:
                logger.critical(f"FRAUD BLOCKED: {data.get('transaction_id')} | Reason: {reason}")
            else:
                logger.info(f"Transaction cleared: {data.get('transaction_id')}")

        except Exception as e:
            logger.error(f"Failed to process message: {e}")


if __name__ == "__main__":
    main()