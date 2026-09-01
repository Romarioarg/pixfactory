![Smoke Tests](https://github.com/Romarioarg/pixfactory/actions/workflows/smoke-tests.yml/badge.svg)

# Testes do PixFactory

Estratégia de QA do produto de gestão de empréstimos, clientes, contratos e cobranças. Esta pasta é **aditiva**: não altera o código de produção.

## Pirâmide

```text
        / E2E Playwright (smoke + regressão) \
       /     Casos manuais + BDD Gherkin      \
      /        Coleção REST (Postman)          \
     /     JUnit das regras de domínio/serviço  \
```

A base são testes unitários rápidos das regras (status, cadastro, contrato, lançamentos). No meio, a API valida contratos HTTP e códigos de erro. No topo, Playwright cobre o que o operador vê no navegador. Casos manuais e `.feature` descrevem o comportamento esperado quando a automação ainda não chega.

## Ferramentas

| Camada | Ferramenta | Motivo |
| --- | --- | --- |
| Unitário | JUnit 5 + Mockito + AssertJ | Já é o stack do backend Spring Boot. |
| API | Postman / Newman | Coleção portátil, sem acoplar ao Maven. |
| UI crítica | Playwright (Chromium) | Login, dashboard e cadastro reais contra `localhost:8080`. |
| UI ampla | Playwright + Page Object | Isola seletores (`#email`, `#cadastro-form`, `#pf-logout`). |
| Manual / BDD | Markdown e Gherkin | Rastreio de fluxos e comunicação com revisão. |
| CI | GitHub Actions | Sobe Docker Compose e roda a smoke a cada push. |

Cálculos financeiros definitivos ficam no backend. Os testes de UI não reimplementam juros: eles conferem tela, navegação e persistência.

## Como rodar

Ambiente da aplicação (necessário para smoke, regressão e Postman):

```bash
docker compose -p pixfactory up --build -d
```

Credenciais DEMO: `demo@pixfactory.app` / `Demo@123` (operador) e `admin@pixfactory.app` / `Admin@123`.

### Manuais

Abra os arquivos em `manual/` e preencha **Resultado Obtido** e **Status** durante a execução.

### Smoke

```bash
cd tests/smoke
npm ci
npx playwright install chromium
npx playwright test
```

`BASE_URL` padrão: `http://localhost:8080`.

### Regressão

```bash
cd tests/regression
npm ci
npx playwright install chromium
npx playwright test
```

### Unitários

```bash
cd tests/unit
mvn -f pom.xml test
```

Se o Maven não estiver no PATH, use o wrapper do backend: `..\backend\mvnw.ps1 -f pom.xml test` a partir de `tests/unit`.

O módulo copia as fontes de `backend/src/main/java` só para compilar os testes (nada é alterado no backend).

### API

Importe `api/PixFactory.postman_collection.json` e `api/PixFactory.postman_environment.json`. Instruções em `api/README.md`.

```bash
npx newman run tests/api/PixFactory.postman_collection.json -e tests/api/PixFactory.postman_environment.json
```

### Cobertura

Abra `coverage-dashboard/index.html` no navegador.

### BDD

Arquivos `.feature` em `bdd/` descrevem os fluxos. Podem ser lidos como especificação ou ligados a um runner Cucumber depois.

## Escopo das suítes automatizadas

As rotas cobertas existem no produto:

- `index.html` login (`#login-form`, `#email`, `#password`)
- `dashboard.html` (`#dashboard-root`)
- `cadastro.html` / `clientes.html`
- `contratos.html` / `novo_contrato.html`
- `agenda.html`
- `simulador.html`
- API: `/api/auth/login`, `/api/auth/me`, `/api/clients`, `/api/contracts`, `/api/dashboard`, `/api/appointments`, `/api/payments`

Páginas e endpoints que ainda não estão no branch de produção (por exemplo caixa diário e cobranças) aparecem nos manuais e no BDD como extensão, sem quebrar a smoke do CI.
