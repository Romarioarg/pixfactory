# language: pt
Funcionalidade: Cadastro e lista de clientes
  Como operador
  Quero registrar pessoas com CPF único
  Para operar contratos em cima da base real do MySQL

  Cenário: Listar seed
    Dado que fiz login como DEMO
    Quando abro "clientes.html"
    Então "#clientes-lista" contém "João da Silva" ou "Maria Oliveira"

  Cenário: Novo cliente válido
    Dado que estou em "cadastro.html"
    Quando preencho nome, CPF válido, telefone, e-mail, endereço e classificação
    E clico em "Salvar cliente"
    Então sou levado a "clientes.html"
    E o nome cadastrado aparece na lista

  Cenário: CPF inválido
    Dado o formulário "#cadastro-form"
    Quando informo CPF "111.111.111-11"
    Então o campo mostra "CPF inválido."
    E nenhum POST é persistido

  Cenário: CPF duplicado
    Dado que já existe o CPF "529.982.247-25"
    Quando tento cadastrar outro cliente com o mesmo CPF
    Então a API responde 409
    E a mensagem cita CPF
