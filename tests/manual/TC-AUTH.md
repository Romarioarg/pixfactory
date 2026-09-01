# Autenticação e sessão

## TC-AUTH-001

| Campo | Conteúdo |
| --- | --- |
| **ID** | TC-AUTH-001 |
| **Cenário** | Login com credenciais DEMO válidas |
| **Pré-condições** | Aplicação no ar. Sessão limpa (sem `pixfactory.token` no localStorage). |
| **Passos** | 1. Abrir `index.html` (raiz `/`). 2. Preencher `#email` com `demo@pixfactory.app`. 3. Preencher `#password` com `Demo@123`. 4. Clicar em Entrar. |
| **Resultado Esperado** | Redireciona para `dashboard.html`. Sidebar PixFactory visível. Título com saudação "Olá". |
| **Resultado Obtido** | |
| **Status** | Não executado |

## TC-AUTH-002

| Campo | Conteúdo |
| --- | --- |
| **ID** | TC-AUTH-002 |
| **Cenário** | Login rejeita senha incorreta |
| **Pré-condições** | Tela de login. |
| **Passos** | 1. Informar e-mail DEMO. 2. Senha `errada123`. 3. Submeter `#login-form`. |
| **Resultado Esperado** | Permanece em `index.html`. Mensagem de erro no campo senha. Não entra no dashboard. |
| **Resultado Obtido** | |
| **Status** | Não executado |

## TC-AUTH-003

| Campo | Conteúdo |
| --- | --- |
| **ID** | TC-AUTH-003 |
| **Cenário** | Rota autenticada sem token |
| **Pré-condições** | localStorage sem token. |
| **Passos** | 1. Abrir `dashboard.html` diretamente. |
| **Resultado Esperado** | Redireciona para `index.html`. |
| **Resultado Obtido** | |
| **Status** | Não executado |

## TC-AUTH-004

| Campo | Conteúdo |
| --- | --- |
| **ID** | TC-AUTH-004 |
| **Cenário** | Logout |
| **Pré-condições** | Usuário autenticado no dashboard. |
| **Passos** | 1. Abrir o menu se estiver em viewport estreita (botão `.hamburger`). 2. Clicar `#pf-logout` (Sair). |
| **Resultado Esperado** | Volta para `index.html`. Novo acesso a `clientes.html` exige login. |
| **Resultado Obtido** | |
| **Status** | Não executado |

## TC-AUTH-005

| Campo | Conteúdo |
| --- | --- |
| **ID** | TC-AUTH-005 |
| **Cenário** | Recuperação de senha |
| **Pré-condições** | Tela de login. |
| **Passos** | 1. Clicar em "Esqueceu a senha?". 2. Informar `demo@pixfactory.app` em `recuperar_senha.html`. 3. Enviar. |
| **Resultado Esperado** | API `/api/auth/forgot-password` responde. Tela informa o próximo passo (código DEMO). Sem e-mail real. |
| **Resultado Obtido** | |
| **Status** | Não executado |
