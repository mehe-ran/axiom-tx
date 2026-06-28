# AxiomTx: Distributed Settlement Engine

AxiomTx is an event-driven microservices architecture designed to handle high-throughput financial transactions. It validates, enriches, and settles payments asynchronously using an idempotent ledger.

## Architecture Highlights
* **Edge Gateway:** FastAPI-based ingestion point. Validates schemas using Pydantic.
* **Event Bus:** Confluent Kafka cluster for decoupled message brokering.
* **Stream Processors:** Isolated Python nodes (FX Enrichment, Fraud Detection, Settlement).
* **Ledger:** PostgreSQL database utilizing `transaction_id` unique constraints for strict idempotency.

## Quickstart
Ensure Docker is installed, then run the entire cluster:
```bash
make up