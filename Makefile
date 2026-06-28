# devops orchestration
.PHONY: up down restart logs wipe

up:
	docker-compose up -d --build

down:
	docker-compose down

restart:
	docker-compose down && docker-compose up -d --build

logs:
	docker-compose logs -f

wipe:
	docker-compose down -v
	rm -rf ./venv