# Gateway

The edge entry point: a Spring Boot web service on port 8000.

`POST /transfer` validates the payload (see the root README for the rules) and returns `422` with a FastAPI-style `detail` body if it's invalid. Valid transfers become a `TransactionEvent`, which is published to the `tx_pending` Kafka topic keyed by `transaction_id`. The request waits for the broker ack, so a `200 {"status": "accepted", "transaction_id": ...}` means the event is on the topic. A failed or timed-out send returns `500`.

Tests: `TransferControllerTest` (`./mvnw -pl gateway -am test`).
