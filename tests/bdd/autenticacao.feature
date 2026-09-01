# language: pt
Funcionalidade: Autenticação JWT
  Como operador do PixFactory
  Quero entrar com e-mail e senha DEMO
  Para acessar o dashboard e as rotas internas

  Cenário: Login bem-sucedido
    Dado que a aplicação está em "http://localhost:8080"
    E que não há token em localStorage
    Quando eu preencho "#email" com "demo@pixfactory.app"
    E preencho "#password" com "Demo@123"
    E envio "#login-form"
    Então sou redirecionado para "dashboard.html"
    E vejo a saudação "Olá" em "#dashboard-root"

  Cenário: Senha incorreta
    Dado que estou na tela de login
    Quando eu informo uma senha inválida
    Então permaneço em "index.html"
    E vejo erro em "[data-error-for=password]"

  Cenário: Acesso sem sessão
    Dado que não estou autenticado
    Quando abro "clientes.html"
    Então sou enviado de volta ao login

  Cenário: Logout
    Dado que estou autenticado no dashboard
    Quando clico em "#pf-logout"
    Então volto para a tela de login
    E "contratos.html" exige autenticação novamente
