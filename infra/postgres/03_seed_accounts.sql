-- accounts used by scripts/SeedData.java, so seeded transfers pass the fk check
INSERT INTO accounts (account_id, balance)
SELECT 'acc_' || n, 10000.00
FROM generate_series(1000, 9999) AS n
ON CONFLICT (account_id) DO NOTHING;
