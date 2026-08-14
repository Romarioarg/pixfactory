# PixFactory

Gestão de clientes, contratos, cobranças e financeiro para um negócio de empréstimos — reconstruído como **aplicação de portfólio** com frontend, API REST Spring Boot, MySQL, autenticação JWT, autorização por papéis e integrações **DEMO/MOCK**.

> As integrações externas (Pix, pagamentos, Open Finance e e-mail) são simuladas para demonstração e portfólio. Não há conexão com bancos, gateways ou SMTP reais.

## Objetivo

Demonstrar, em um único repositório, um fluxo completo e apresentável:

HTML → CSS → JavaScript → REST → Spring Boot → JPA → MySQL

Uma pessoa deve conseguir clonar, subir o Docker, fazer login DEMO e usar o sistema com dados fictícios.

## Preview

Abra `http://localhost:8080` após `docker compose up`.

Telas principais: login, dashboard, clientes, contratos, agenda, gastos, relatórios, Pix Demo, Open Finance Demo e notificações.

## Funcionalidades

- Autenticação real (BCrypt + JWT)
- Autorização `ADMIN` / `USER`
- CRUD de clientes, contratos, gastos, lançamentos, limites, agenda
- Dashboard e relatórios alimentados pelo banco
- Pix Demo e pagamento Demo (PENDING → PAID / FAILED / CANCELLED)
- Open Finance Demo (instituições, conta e saldo fictícios)
- E-mail Demo (console + persistência)
- Notificações internas
- Seed automático com dados fictícios
- Swagger/OpenAPI
- Docker Compose

## Tecnologias

- Frontend: HTML5, CSS3, JavaScript (vanilla)
- Backend: Java 21, Spring Boot 3.4, Spring Web, Spring Data JPA, Spring Security, Bean Validation
- Banco: MySQL 8
- Testes: JUnit, MockMvc, Spring Security Test, Node (frontend)
- Docs: springdoc-openapi
- DevOps: Docker Compose

## Arquitetura

```text
Frontend
   ↓  REST + JWT
Spring Boot
   ↓  JPA
MySQL
```

`localStorage` guarda apenas token JWT e preferências de interface (ocultar valores / tema). **Não** é a fonte de persistência.

## Estrutura

```text
pixfactory/
├── frontend/          # páginas, CSS, JS
├── backend/           # API Spring Boot
├── database/seed/     # documentação do seed
├── mocks/             # contrato das integrações DEMO
├── docs/              # arquitetura
├── tests/e2e/         # guia Playwright opcional
├── docker-compose.yml
├── Dockerfile
├── .env.example
└── README.md
```

## Backend

Pacotes: `web` → `service` → `repo` / `domain`, com `security`, `integration`, `seed` e `exception`.

A API é servida em `http://localhost:8080/api`. O próprio Spring também entrega o frontend.

## Frontend

Páginas estáticas em `frontend/`. Em desenvolvimento, o backend lê `../frontend`. No Docker, os arquivos entram em `classpath:/static`.

## Banco

MySQL 8. O Hibernate cria/atualiza o schema (`ddl-auto=update`) e o `DataSeeder` popula dados DEMO na primeira execução (quando não há usuários).

## Demo / Mock Services

| Serviço | Modo atual | Como substituir no futuro |
|---|---|---|
| Pix | `DemoPixService` | Criar `RealPixService` implementando `PixService` |
| Pagamentos | `DemoPaymentGateway` | Criar `RealPaymentGateway` implementando `PaymentGateway` |
| Open Finance | `DemoOpenFinanceService` | Criar `RealOpenFinanceService` |
| E-mail | `DemoEmailService` | Criar `RealEmailService` (SMTP) |

Fluxo Pix Demo:

```text
Contrato → Pix Demo → TXID fictício → PENDING
       → Confirmar → PAID → atualiza contrato no MySQL → dashboard
```

## Conta Demo

| Perfil | E-mail | Senha |
|---|---|---|
| USER | `demo@pixfactory.app` | `Demo@123` |
| ADMIN | `admin@pixfactory.app` | `Admin@123` |

Credenciais fictícias, apenas para o ambiente de portfólio.

## Instalação

### Docker (recomendado)

```bash
git clone https://github.com/Romarioarg/pixfactory.git
cd pixfactory
docker compose up --build
```

Abra [http://localhost:8080](http://localhost:8080) e entre com a conta DEMO.

### Desenvolvimento local

1. MySQL 8 em `localhost:3306`, database/user/password `pixfactory`.
2. Java 21.
3. Maven:

```bash
cd backend
mvn spring-boot:run
```

O frontend é servido em `http://localhost:8080`.

Copie `.env.example` para `.env` se quiser sobrescrever variáveis. **Não commite `.env`.**

## Configuração

Variáveis principais (veja `.env.example`):

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET` (mínimo 32 caracteres; troque fora do DEMO)
- `PIXFACTORY_SEED`

## Docker

```bash
docker compose up --build
```

Sobe MySQL + API. Para zerar o seed:

```bash
docker compose down -v
docker compose up --build
```

## API

Exemplos:

```text
POST   /api/auth/login
GET    /api/auth/me
GET    /api/clients
POST   /api/clients
PUT    /api/clients/{id}
DELETE /api/clients/{id}
GET    /api/contracts
POST   /api/contracts/{id}/payments
POST   /api/payments/{id}/confirm
GET    /api/dashboard
GET    /api/reports
GET    /api/appointments
GET    /api/open-finance/institutions
```

## Swagger

- UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- OpenAPI: [http://localhost:8080/api/docs](http://localhost:8080/api/docs)

Authorize com `Bearer <token>` após o login.

## Testes

Backend (H2 em memória):

```bash
cd backend
mvn test
```

Frontend:

```bash
node frontend/tests/store.test.js
node frontend/tests/links.test.js
```

Com Docker Maven, a partir da raiz:

```bash
docker run --rm -v "${PWD}/backend:/src" -w /src maven:3.9.9-eclipse-temurin-21 mvn test
```

## Segurança

- Senhas com BCrypt
- JWT no header `Authorization`
- CORS restrito
- JPA/parametrização (sem concatenar SQL)
- Sem credenciais pessoais reais no repositório
- Stack traces não são enviados ao cliente

Troque `JWT_SECRET` se for além de um DEMO local.

## Limitações

- Pix, pagamentos, Open Finance e e-mail são **DEMO**
- Não há deploy de produção configurado neste repositório
- O schema usa `ddl-auto=update` (adequado a portfólio, não a produção)
- WhatsApp abre o `wa.me` com números fictícios do seed

## Roadmap

- Implementar `RealPixService` / `RealPaymentGateway`
- Flyway/Liquibase no lugar de `ddl-auto`
- Deploy (Render, Railway ou VM + Nginx)
- Playwright E2E no CI

## Autor

Anderson (Romarioarg) — projeto de portfólio.
