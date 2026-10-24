# Settlement Node

The ledger writer, and the only service that writes to Postgres. It consumes `tx_pending` as `settlement-group` (`auto.offset.reset=earliest`) and inserts each event into `transactions`.

Idempotency is enforced by the database: `transaction_id` is the primary key. A redelivered or retried event hits a duplicate-key violation, which is logged and skipped, so a transaction is never settled twice. An unknown sender or receiver hits the foreign key into `accounts` and is also skipped. Any other failure is published to `tx_dead_letter` as `{"original_data", "error"}`. Nothing consumes that topic yet.

Tests: `SettlementListenerTest` runs the real `infra/postgres/init.sql` on H2 in PostgreSQL mode (`./mvnw -pl workers/settlement_node -am test`).
