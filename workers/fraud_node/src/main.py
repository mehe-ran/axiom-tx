import logging
from confluent_kafka import Consumer
from shared.schemas import TransactionEvent

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


def check_velocity(event: TransactionEvent):
    # placeholder for geographical velocity check
    return True


def detect_fraud(event: TransactionEvent):
    amount = event.amount
    sender = event.sender_id

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
            # parse and validate the incoming kafka message against the shared contract
            event = TransactionEvent.model_validate_json(msg.value())

            # run the checks
            velocity_pass = check_velocity(event)
            is_fraud, reason = detect_fraud(event)

            if is_fraud:
                logger.critical(f"FRAUD BLOCKED: {event.transaction_id} | Reason: {reason}")
            else:
                logger.info(f"Transaction cleared: {event.transaction_id}")

        except Exception as e:
            logger.error(f"Failed to process message: {e}")


if __name__ == "__main__":
    main()