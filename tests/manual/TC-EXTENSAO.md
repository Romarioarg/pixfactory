# Extensão: caixa, cobrança e inadimplência

Usar quando as telas `caixa.html`, `inadimplencia.html` e as APIs `/api/caixa` e `/api/cobrancas` estiverem disponíveis no ambiente. Se a página não existir, marcar **Bloqueado**.

## TC-CX-001

| Campo | Conteúdo |
| --- | --- |
| **ID** | TC-CX-001 |
| **Cenário** | Abrir e fechar caixa do dia |
| **Pré-condições** | Login. Nenhum caixa aberto hoje. |
| **Passos** | 1. `caixa.html`. 2. Informar saldo de abertura. 3. Abrir caixa. 4. Lançar entrada. 5. Fechar com valor contado. |
| **Resultado Esperado** | Cards de abertura, entradas, saídas e esperado. Fechamento registra justificativa se houver diferença. |
| **Resultado Obtido** | |
| **Status** | Não executado |

## TC-INAD-001

| Campo | Conteúdo |
| --- | --- |
| **ID** | TC-INAD-001 |
| **Cenário** | Filtros e ações de inadimplência |
| **Pré-condições** | Parcelas atrasadas no seed. |
| **Passos** | 1. `inadimplencia.html`. 2. Chips Todos / Sem contato / Promessa hoje / faixas de dias. 3. Promessa, Receber, Recibo, Estorno. |
| **Resultado Esperado** | Tabela com cliente, parcela, valor, dias e ações. Pagamento parcial deixa status PARCIAL. Excedente pede destino e não some sozinho. |
| **Resultado Obtido** | |
| **Status** | Não executado |
