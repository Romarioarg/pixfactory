# Contratos, simulador e dashboard

## TC-CTR-001

| Campo | Conteúdo |
| --- | --- |
| **ID** | TC-CTR-001 |
| **Cenário** | Listar e filtrar contratos |
| **Pré-condições** | Login. Seed com contratos. |
| **Passos** | 1. Abrir `contratos.html`. 2. Clicar chips `#filters` (Todos, Ativos, Pendentes, Atrasados, Encerrados). 3. Buscar em `#contract-search`. |
| **Resultado Esperado** | Tabela `#contracts-body` atualiza. Status exibido em badge. |
| **Resultado Obtido** | |
| **Status** | Não executado |

## TC-CTR-002

| Campo | Conteúdo |
| --- | --- |
| **ID** | TC-CTR-002 |
| **Cenário** | Criar contrato para cliente existente |
| **Pré-condições** | Pelo menos um cliente. |
| **Passos** | 1. `novo_contrato.html`. 2. Selecionar `#cliente`. 3. Valor, juros, parcelas, vencimento. 4. Salvar contrato. |
| **Resultado Esperado** | POST `/api/contracts` 201. Contrato na lista. Preview em `#contrato-preview` antes do save. |
| **Resultado Obtido** | |
| **Status** | Não executado |

## TC-SIM-001

| Campo | Conteúdo |
| --- | --- |
| **ID** | TC-SIM-001 |
| **Cenário** | Simular empréstimo Price |
| **Pré-condições** | Login. `simulador.html`. |
| **Passos** | 1. Aba Empréstimo. 2. Nome, valor 1000, taxa 2.5, 12 parcelas. 3. Calcular. |
| **Resultado Esperado** | `#res-nome` com o nome. `#res-parcela` e `#res-total` diferentes de R$ 0,00. Tabela `#amort-body` com 12 linhas. Simulação não cria contrato. |
| **Resultado Obtido** | |
| **Status** | Não executado |

## TC-SIM-002

| Campo | Conteúdo |
| --- | --- |
| **ID** | TC-SIM-002 |
| **Cenário** | Aba apenas juros |
| **Pré-condições** | `simulador.html`. |
| **Passos** | 1. Clicar `[data-tab=juros]`. 2. Saldo 1000, taxa 2.5. 3. Calcular juros. |
| **Resultado Esperado** | Resultado mostra juro do período. Contrato no banco permanece inalterado. |
| **Resultado Obtido** | |
| **Status** | Não executado |

## TC-DASH-001

| Campo | Conteúdo |
| --- | --- |
| **ID** | TC-DASH-001 |
| **Cenário** | Dashboard com métricas do banco |
| **Pré-condições** | Login DEMO. |
| **Passos** | 1. Abrir `dashboard.html`. 2. Conferir cards Clientes ativos, Contratos vigentes, Total emprestado. 3. Alternar `#toggle-values`. |
| **Resultado Esperado** | Números vindos de `/api/dashboard` (via bootstrap). Ocultar valores aplica classe `hidden`. |
| **Resultado Obtido** | |
| **Status** | Não executado |

## TC-AGE-001

| Campo | Conteúdo |
| --- | --- |
| **ID** | TC-AGE-001 |
| **Cenário** | Agenda de cobranças |
| **Pré-condições** | Login. |
| **Passos** | 1. `agenda.html`. 2. Alternar filtros Mês / Hoje / Atrasados. 3. Navegar `#prev-month` / `#next-month`. |
| **Resultado Esperado** | Calendário `#calendar-grid` renderiza. Detalhe do dia em `#day-details`. |
| **Resultado Obtido** | |
| **Status** | Não executado |
