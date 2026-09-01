# language: pt
Funcionalidade: Caixa e inadimplência
  Como operador de cobrança
  Quero registrar caixa do dia e tratar atraso
  Quando essas telas existirem no ambiente

  @extensao
  Cenário: Abrir caixa
    Dado que "caixa.html" está disponível
    E que não há sessão de caixa aberta hoje
    Quando informo o saldo de abertura e confirmo
    Então GET "/api/caixa/hoje" retorna aberto verdadeiro

  @extensao
  Cenário: Pagamento parcial
    Dado uma parcela de 700 em aberto
    Quando registro pagamento de 300 em "/api/cobrancas/{id}/pagamentos"
    Então o saldo fica 400
    E o status da parcela é "parcial"

  @extensao
  Cenário: Excedente não é aplicado em silêncio
    Dado uma parcela de 500
    Quando pago 800 sem informar destino do excedente
    Então a API responde 409 com codigo "excedente"
    E oferece opções de amortizar, antecipar ou crédito
