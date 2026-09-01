# Coleção Postman — PixFactory API

Base: `http://localhost:8080` (mesmo host do Docker Compose).

## Importar

1. Abra o Postman.
2. Import → `PixFactory.postman_collection.json`.
3. Import → `PixFactory.postman_environment.json`.
4. Selecione o environment **PixFactory Local**.

## Rodar

Pela UI: Collection → Run.

Pela CLI:

```bash
npx newman run tests/api/PixFactory.postman_collection.json -e tests/api/PixFactory.postman_environment.json
```

A pasta **Auth** deve rodar primeiro: o teste de login grava `token` na environment.

## Contratos conferidos

| Método | Caminho | Sucesso | Erro |
| --- | --- | --- | --- |
| POST | `/api/auth/login` | 200, `token`, `user.email` | 401 senha inválida |
| GET | `/api/auth/me` | 200 | 401 sem Bearer |
| GET | `/api/clients` | 200 array | 401 |
| POST | `/api/clients` | 201 | 409 CPF duplicado |
| GET | `/api/clients/{id}` | 200 | 404 |
| GET | `/api/contracts` | 200 | 401 |
| GET | `/api/dashboard` | 200 métricas | 401 |
| GET | `/api/users` | 200 como ADMIN | 403 como USER |
| GET | `/api/appointments` | 200 | 401 |
| GET | `/api/payments` | 200 | 401 |

Credenciais DEMO: `demo@pixfactory.app` / `Demo@123` e `admin@pixfactory.app` / `Admin@123`.
