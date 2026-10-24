# devops orchestration
.PHONY: up down restart logs wipe build test reconcile seed edge-cases

up:
	docker compose up -d --build

down:
	docker compose down --remove-orphans

restart:
	docker compose down --remove-orphans && docker compose up -d --build

logs:
	docker compose logs -f

wipe:
	docker compose down -v --remove-orphans
	./mvnw -q clean

build:
	./mvnw -B verify

test:
	./mvnw -B test

reconcile:
	docker compose run --rm --build reconciliation

seed:
	java scripts/SeedData.java

edge-cases:
	java scripts/TestEdgeCases.java
