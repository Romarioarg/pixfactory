function uniqueCpf() {
  const n = String(Date.now() % 1_000_000_000).padStart(9, "3");
  const digits = n.split("").map(Number);
  const check = (len) => {
    let sum = 0;
    for (let i = 0; i < len; i += 1) sum += digits[i] * (len + 1 - i);
    const rest = (sum * 10) % 11;
    return rest === 10 ? 0 : rest;
  };
  digits.push(check(9));
  digits.push(check(10));
  return digits.join("");
}

class LoginPage {
  constructor(page) {
    this.page = page;
    this.email = page.locator("#email");
    this.password = page.locator("#password");
    this.submit = page.locator("#login-form button[type=submit]");
    this.passwordError = page.locator("[data-error-for=password]");
  }

  async open() {
    await this.page.addInitScript(() => {
      try { localStorage.removeItem("pixfactory.token"); } catch (err) { /* ignore */ }
    });
    await this.page.goto("/");
  }

  async login(email, password) {
    await this.open();
    await this.email.fill(email);
    await this.password.fill(password);
    await this.submit.click();
  }
}

class DashboardPage {
  constructor(page) {
    this.page = page;
    this.root = page.locator("#dashboard-root");
    this.title = page.locator("#dashboard-root h1");
    this.toggleValues = page.locator("#toggle-values");
    this.logout = page.locator("#pf-logout");
    this.sidebar = page.locator(".sidebar");
    this.hamburger = page.locator(".hamburger");
  }

  async expectLoaded() {
    await this.page.waitForURL(/dashboard\.html/);
    await this.title.waitFor();
  }
}

class CadastroPage {
  constructor(page) {
    this.page = page;
    this.form = page.locator("#cadastro-form");
    this.nome = page.locator("#nome");
    this.cpf = page.locator("#cpf");
    this.telefone = page.locator("#telefone");
    this.email = page.locator("#email");
    this.endereco = page.locator("#endereco");
    this.classificacao = page.locator("#classificacao");
    this.cpfError = page.locator("[data-error-for=cpf]");
  }

  async open() {
    await this.page.goto("/cadastro.html");
  }

  async fillValid(overrides = {}) {
    const stamp = Date.now();
    await this.nome.fill(overrides.nome || "Cliente Regressao " + stamp);
    await this.cpf.fill(overrides.cpf || uniqueCpf());
    await this.telefone.fill(overrides.telefone || "11977776666");
    await this.email.fill(overrides.email || "reg." + stamp + "@example.com");
    await this.endereco.fill(overrides.endereco || "Av. Teste, 50");
    await this.classificacao.selectOption(overrides.classificacao || "Bom pagador");
    return stamp;
  }

  async submit() {
    await this.form.locator("button[type=submit]").click();
  }
}

class ClientesPage {
  constructor(page) {
    this.page = page;
    this.search = page.locator("#clientSearch");
    this.list = page.locator("#clientes-lista");
  }

  async open() {
    await this.page.goto("/clientes.html");
  }
}

class ContratosPage {
  constructor(page) {
    this.page = page;
    this.body = page.locator("#contracts-body");
    this.search = page.locator("#contract-search");
    this.filters = page.locator("#filters");
  }

  async open() {
    await this.page.goto("/contratos.html");
  }

  async filter(name) {
    await this.filters.locator("[data-filter=" + name + "]").click();
  }
}

class NovoContratoPage {
  constructor(page) {
    this.page = page;
    this.cliente = page.locator("#cliente");
    this.valor = page.locator("#valor");
    this.juros = page.locator("#juros");
    this.parcelas = page.locator("#parcelas");
    this.submit = page.locator("#novo-contrato-form button[type=submit]");
    this.preview = page.locator("#contrato-preview");
  }

  async open() {
    await this.page.goto("/novo_contrato.html");
  }
}

class SimuladorPage {
  constructor(page) {
    this.page = page;
    this.nome = page.locator("#nome-cliente");
    this.valor = page.locator("#valor-emprestimo");
    this.taxa = page.locator("#taxa-juros");
    this.parcelas = page.locator("#num-parcelas");
    this.calcular = page.locator("#emprestimo-form button[type=submit]");
    this.resultadoNome = page.locator("#res-nome");
    this.resultadoParcela = page.locator("#res-parcela");
    this.amortizacao = page.locator("#amort-body tr");
    this.tabJuros = page.locator("[data-tab=juros]");
  }

  async open() {
    await this.page.goto("/simulador.html");
  }
}

class AgendaPage {
  constructor(page) {
    this.page = page;
    this.grid = page.locator("#calendar-grid");
    this.filters = page.locator("#agenda-filters");
  }

  async open() {
    await this.page.goto("/agenda.html");
  }
}

module.exports = {
  uniqueCpf,
  LoginPage,
  DashboardPage,
  CadastroPage,
  ClientesPage,
  ContratosPage,
  NovoContratoPage,
  SimuladorPage,
  AgendaPage,
};
