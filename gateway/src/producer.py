from confluent_kafka import Producer
import json

producer = Producer({'bootstrap.servers': 'kafka:9092'})

def produce_event(topic, data):
    producer.produce(topic, json.dumps(data).encode('utf-8'))
    producer.flush()