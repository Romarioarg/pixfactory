# language: pt
Funcionalidade: Dashboard, agenda e papéis
  Como operador diário
  Quero ver números do banco e a agenda de cobrança
  Respeitando ADMIN e USER

  Cenário: Métricas do dashboard
    Dado login DEMO
    Quando abro "dashboard.html"
    Então vejo "Clientes ativos" e "Contratos vigentes"
    E os valores vêm de "/api/dashboard"

  Cenário: Agenda
    Dado login DEMO
    Quando abro "agenda.html"
    E filtro por "Hoje" e "Atrasados"
    Então "#calendar-grid" permanece visível

  Cenário: USER não lista usuários
    Dado token de "demo@pixfactory.app"
    Quando chamo GET "/api/users"
    Então o status é 403

  Cenário: ADMIN lista usuários
    Dado token de "admin@pixfactory.app"
    Quando chamo GET "/api/users"
    Então o status é 200
    E a lista contém e-mails
