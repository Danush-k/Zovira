.PHONY: help infra infra-down api web install test test-api test-web lint build

help:
	@echo "Zovira developer commands"
	@echo "  make infra       Start Postgres, Redis, object storage and Mailpit"
	@echo "  make infra-down  Stop local infrastructure"
	@echo "  make install     Install web dependencies"
	@echo "  make api         Run the Spring Boot API (dev profile, seeds an empty database)"
	@echo "  make web         Run the Vite dev server on http://localhost:5173"
	@echo "  make test        Run API and web test suites"
	@echo "  make lint        Lint and type-check the web app"
	@echo "  make build       Build API jar and web bundle"

infra:
	docker compose up -d

infra-down:
	docker compose down

install:
	cd frontend && npm ci

api:
	cd backend && ./mvnw spring-boot:run

web:
	cd frontend && npm run dev

test: test-api test-web

test-api:
	cd backend && ./mvnw -B verify

test-web:
	cd frontend && npm test

lint:
	cd frontend && npm run lint && npm run type-check

build:
	cd backend && ./mvnw -B -DskipTests package
	cd frontend && npm run build
