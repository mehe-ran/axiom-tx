# AxiomTx: Distributed Settlement Engine

AxiomTx is an event-driven settlement engine. An HTTP gateway validates transfers and publishes them to Kafka. Independent workers consume the stream, and PostgreSQL is the idempotent ledger of record.

Built with Java 21, Spring Boot 3 and Spring for Apache Kafka, as a Maven multi-module project.

> The original Python version (FastAPI, confluent-kafka, psycopg2) lives on the [`python` branch](../../tree/python).

## Architecture

```
POST /transfer ──► gateway ──► kafka: tx_pending ──┬──► fx_node          (fx-group, logs only)
                                                   ├──► fraud_node       (fraud-group, logs verdicts)
                                                   └──► settlement_node  (settlement-group) ──► postgres
                                                              │
                                                              └── on failure ──► kafka: tx_dead_letter
```

The three workers subscribe to `tx_pending` with separate consumer groups. Each one gets every event, so this is a parallel fan-out, not a pipeline.

| Module | What it does |
| --- | --- |
| `shared` | `TransactionEvent` record, the JSON contract on `tx_pending` (snake_case keys, `BigDecimal` amount) |
| `gateway` | `POST /transfer` on port 8000. Validates the request, publishes to `tx_pending` synchronously and returns 200 once the broker acks |
| `workers/settlement_node` | The only writer to Postgres. Inserts each event into `transactions` |
| `workers/fraud_node` | Flags amounts over 10,000 and blocklisted senders (`acc_9999`, `acc_0000`, `acc_6666`) and logs the verdict |
| `workers/fx_node` | Logs the amount and currency of each event (stub) |
| `jobs/reconciliation` | One-off audit: counts transactions that aren't `processed` and sums settled volume |

### Gateway validation

Invalid requests return `422` with a FastAPI-style body (`{"detail": [{"loc": ["body", "<field>"], "msg": ..., "type": ...}]}`):

- `sender_id` and `receiver_id` are required, at least 4 characters, and must differ
- `amount` is required and must be greater than 0
- `currency` is at most 3 characters and defaults to `USD`
- `transaction_id` defaults to a random UUID

If the send fails or Kafka doesn't ack within 10 seconds, the gateway returns `500 {"detail": "Internal routing failure"}`.

### Idempotent settlement

`transactions.transaction_id` is the primary key. If Kafka redelivers an event, or a client retries with the same `transaction_id`, the insert hits a duplicate-key violation. The settlement node logs it as a warning and skips it, so a transaction is never settled twice and a settled row is never overwritten. A foreign-key violation (unknown sender or receiver account) is also logged and skipped.

Any other failure goes to `tx_dead_letter` as `{"original_data": "<raw message>", "error": "<exception>"}`. That covers malformed JSON, missing fields and other database errors.

These paths are covered by `SettlementListenerTest`, which runs the real `infra/postgres/init.sql` against H2 in PostgreSQL mode.

### Known limitations

- **Fraud does not gate settlement.** `fraud_node` only logs its verdict. A flagged transaction still settles, because `settlement_node` consumes `tx_pending` directly.
- **FX is a stub.** No conversion happens. Amounts are stored as sent, whatever the currency.
- **Nothing consumes `tx_dead_letter`.** Dead letters are not retried or alerted on.
- **Balances are not moved.** Settlement records the transfer but doesn't debit or credit `accounts.balance`.

## Quickstart

Requires Docker. The scripts and local builds also need JDK 21.

```bash
make up            # build every module image and start postgres, kafka and all services
make seed          # POST 50 random transfers between acc_1000..acc_9999
make edge-cases    # POST known-bad payloads, expect 422 for each
make reconcile     # run the reconciliation job against the compose postgres
make logs          # tail all containers
make down          # stop the stack (make wipe also drops volumes)
```

Check what landed in the ledger:

```bash
docker compose exec postgres psql -U postgres -d axiom_tx -c "select count(*), sum(amount) from transactions;"
```

Postgres runs three init scripts on first start: `init.sql` (schema), `02_indexes.sql`, and `03_seed_accounts.sql`, which creates accounts `acc_1000` to `acc_9999` with a 10,000.00 balance so seeded transfers pass the FK check.

## Build and test

```bash
./mvnw verify      # compile and run the unit tests for every module
```

The root `Dockerfile` builds any module with `--build-arg MODULE=<path>`, for example `docker build --build-arg MODULE=workers/settlement_node .`. It has a Maven build stage and a JRE runtime stage.

### Running a service outside Docker

Every service reads `KAFKA_BROKER_URL` (default `localhost:29092`, the host listener compose exposes). The Postgres services read `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER` and `POSTGRES_PASSWORD`.

```bash
./mvnw -pl gateway -am package -DskipTests
java -jar gateway/target/gateway.jar

./mvnw -pl jobs/reconciliation -am package -DskipTests
java -jar jobs/reconciliation/target/reconciliation.jar   # postgres on localhost by default
```

The reconciliation job exits with `0` on a completed audit and `1` if it can't run its queries.

## Layout

```
shared/                   message contract
gateway/                  http ingestion
workers/fx_node/          fx stub
workers/fraud_node/       fraud rules
workers/settlement_node/  ledger writer
jobs/reconciliation/      audit job
scripts/                  single-file java scripts (java scripts/SeedData.java)
infra/postgres/           init scripts mounted by docker-compose
```
