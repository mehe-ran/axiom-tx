from confluent_kafka import Consumer

consumer = Consumer({'bootstrap.servers': 'kafka:9092', 'group.id': 'fraud-group'})
consumer.subscribe(['tx_enriched'])

def check_velocity(tx_data):
    # placeholder for geographical velocity check
    return True

while True:
    msg = consumer.poll(1.0)
    if msg is None: continue
    check_velocity(msg.value())