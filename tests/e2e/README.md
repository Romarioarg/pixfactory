# E2E (opcional)

Os testes automatizados principais estão no backend (MockMvc + H2).

Para um fluxo de interface com Playwright:

```bash
npm init -y
npm install -D @playwright/test
npx playwright install chromium
```

Cenário sugerido:

1. Abrir `http://localhost:8080`
2. Login `demo@pixfactory.app` / `Demo@123`
3. Dashboard com métricas
4. Cadastrar cliente
5. Criar contrato
6. Pix Demo → confirmar
7. Logout

Este passo exige a API e o MySQL em execução (`docker compose up`).
