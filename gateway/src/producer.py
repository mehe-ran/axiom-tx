from confluent_kafka import Producer
import json

conf = {
    'bootstrap.servers': 'localhost:9092',
    'client.id': 'gateway-producer'
}

# instantiate the producer using the config
producer = Producer(conf)

def produce_event(topic, data):
    producer.produce(topic, json.dumps(data).encode('utf-8'))
    producer.flush()