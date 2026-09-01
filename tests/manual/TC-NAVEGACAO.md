# Navegação, gastos, perfil e administração

## TC-NAV-001

| Campo | Conteúdo |
| --- | --- |
| **ID** | TC-NAV-001 |
| **Cenário** | Menu lateral e atalhos |
| **Pré-condições** | Login. Desktop e viewport 375px. |
| **Passos** | 1. Percorrer links: Clientes, Novo Cliente, Contratos, Agenda, Simulador, Gastos, Exportação, Planos, Configurações, Perfil, Notificações. 2. No mobile, abrir `.hamburger` e fechar clicando fora. |
| **Resultado Esperado** | Cada página carrega o layout com item `active`. Menu hambúrguer abre `.sidebar.open`. Identidade visual (fundo escuro, verde) preservada. |
| **Resultado Obtido** | |
| **Status** | Não executado |

## TC-GAS-001

| Campo | Conteúdo |
| --- | --- |
| **ID** | TC-GAS-001 |
| **Cenário** | Controle de gastos e lançamentos |
| **Pré-condições** | Login. |
| **Passos** | 1. `controle-de-gastos.html`. 2. `lancamentos.html`: criar lançamento com descrição, valor e data. |
| **Resultado Esperado** | POST `/api/launches` persiste. Item aparece na lista. |
| **Resultado Obtido** | |
| **Status** | Não executado |

## TC-ADM-001

| Campo | Conteúdo |
| --- | --- |
| **ID** | TC-ADM-001 |
| **Cenário** | Usuário USER não lista usuários |
| **Pré-condições** | Login `demo@pixfactory.app`. |
| **Passos** | 1. Tentar `gerenciamento_usuarios.html` ou GET `/api/users` com o token USER. |
| **Resultado Esperado** | HTTP 403. Menu Usuários não aparece para USER. |
| **Resultado Obtido** | |
| **Status** | Não executado |

## TC-ADM-002

| Campo | Conteúdo |
| --- | --- |
| **ID** | TC-ADM-002 |
| **Cenário** | Admin lista usuários |
| **Pré-condições** | Login `admin@pixfactory.app` / `Admin@123`. |
| **Passos** | 1. Abrir gerenciamento de usuários. |
| **Resultado Esperado** | GET `/api/users` 200. Lista inclui e-mails DEMO. |
| **Resultado Obtido** | |
| **Status** | Não executado |

## TC-PAY-001

| Campo | Conteúdo |
| --- | --- |
| **ID** | TC-PAY-001 |
| **Cenário** | Pagamento Pix Demo |
| **Pré-condições** | Contrato existente. |
| **Passos** | 1. Criar pagamento via contrato (`POST /api/contracts/{id}/payments`). 2. Confirmar `POST /api/payments/{id}/confirm`. |
| **Resultado Esperado** | Status PENDING → PAID no ambiente DEMO. Sem gateway real. |
| **Resultado Obtido** | |
| **Status** | Não executado |
