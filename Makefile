# devops orchestration
.PHONY: up down restart logs wipe

up:
	docker-compose up -d --build

down:
	docker-compose down --remove-orphans

restart:
	docker-compose down --remove-orphans && docker-compose up -d --build

logs:
	docker-compose logs -f

wipe:
	docker-compose down -v --remove-orphans
	rm -rf ./venv