# language: pt
Funcionalidade: Contratos e simulador
  Como operador
  Quero simular e gravar operações
  Sem misturar prévia com contrato definitivo

  Cenário: Filtrar contratos
    Dado que estou em "contratos.html"
    Quando escolho o chip "Atrasados"
    Então "#contracts-body" mostra apenas status atrasado

  Cenário: Novo contrato
    Dado que existe pelo menos um cliente
    Quando abro "novo_contrato.html"
    E seleciono o cliente, valor, juros e parcelas
    E salvo o contrato
    Então a API cria o registro em "/api/contracts"
    E o contrato volta a aparecer na lista

  Cenário: Simulação não grava empréstimo
    Dado que estou em "simulador.html"
    Quando calculo um empréstimo de 1000 a 2,5% em 12 parcelas
    Então "#res-parcela" é diferente de "R$ 0,00"
    E "#amort-body" tem 12 linhas
    E a quantidade de contratos no banco não aumenta
