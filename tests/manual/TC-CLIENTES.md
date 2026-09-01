# Clientes

## TC-CLI-001

| Campo | Conteúdo |
| --- | --- |
| **ID** | TC-CLI-001 |
| **Cenário** | Listar clientes seed |
| **Pré-condições** | Login DEMO. Seed habilitado. |
| **Passos** | 1. Menu Clientes (`clientes.html`). 2. Observar `#clientes-lista`. 3. Usar `#clientSearch`. |
| **Resultado Esperado** | Lista com clientes fictícios. Busca filtra por nome, telefone ou e-mail. |
| **Resultado Obtido** | |
| **Status** | Não executado |

## TC-CLI-002

| Campo | Conteúdo |
| --- | --- |
| **ID** | TC-CLI-002 |
| **Cenário** | Cadastrar cliente válido |
| **Pré-condições** | Login. CPF ainda não usado. |
| **Passos** | 1. Abrir `cadastro.html`. 2. Preencher `#nome`, `#cpf` (CPF válido), `#telefone` (11 dígitos), `#email`, `#endereco`, `#classificacao`. 3. Clicar em Salvar cliente. |
| **Resultado Esperado** | Toast de sucesso. Redireciona para `clientes.html`. Novo nome aparece na lista. |
| **Resultado Obtido** | |
| **Status** | Não executado |

## TC-CLI-003

| Campo | Conteúdo |
| --- | --- |
| **ID** | TC-CLI-003 |
| **Cenário** | Validação de CPF no cadastro |
| **Pré-condições** | `cadastro.html`. |
| **Passos** | 1. Preencher demais campos. 2. `#cpf` = `111.111.111-11`. 3. Submeter. |
| **Resultado Esperado** | Erro "CPF inválido." em `[data-error-for=cpf]`. Cliente não é criado. |
| **Resultado Obtido** | |
| **Status** | Não executado |

## TC-CLI-004

| Campo | Conteúdo |
| --- | --- |
| **ID** | TC-CLI-004 |
| **Cenário** | CPF duplicado |
| **Pré-condições** | Já existe cliente seed. Copiar um CPF da lista. |
| **Passos** | 1. Tentar cadastrar outro nome com o mesmo CPF. |
| **Resultado Esperado** | API 409. Mensagem de conflito no campo CPF. |
| **Resultado Obtido** | |
| **Status** | Não executado |

## TC-CLI-005

| Campo | Conteúdo |
| --- | --- |
| **ID** | TC-CLI-005 |
| **Cenário** | Editar cliente |
| **Pré-condições** | Cliente existente. |
| **Passos** | 1. Abrir `editar-cliente.html` com o id. 2. Alterar telefone. 3. Salvar. |
| **Resultado Esperado** | PUT `/api/clients/{id}` 200. Dado atualizado na listagem. |
| **Resultado Obtido** | |
| **Status** | Não executado |
