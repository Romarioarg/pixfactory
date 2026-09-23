(function (root) {
  const PF = root.PF = root.PF || {};
  const SUPPORT = "https://wa.me/5511999990000?text=" + encodeURIComponent("Olá! Preciso de ajuda com o PixFactory DEMO.");

  const NAV = [
    { href: "dashboard.html", id: "dashboard", label: "Dashboard", icon: "fa-solid fa-house" },
    { href: "clientes.html", id: "clientes", label: "Clientes", icon: "fa-solid fa-user" },
    { href: "cadastro.html", id: "cadastro", label: "Novo Cliente", icon: "fa-solid fa-user-plus" },
    { href: "contratos.html", id: "contratos", label: "Contratos", icon: "fa-solid fa-file-contract" },
    { href: "agenda.html", id: "agenda", label: "Agenda", icon: "fa-solid fa-calendar-days" },
    { href: "inadimplencia.html", id: "inadimplencia", label: "Inadimplência", icon: "fa-solid fa-triangle-exclamation" },
    { href: "caixa.html", id: "caixa", label: "Caixa do dia", icon: "fa-solid fa-cash-register" },
    { href: "simulador.html", id: "simulador", label: "Simulador", icon: "fa-solid fa-calculator" },
    { href: "controle-de-gastos.html", id: "gastos", label: "Controle de Gastos", icon: "fa-solid fa-wallet" },
    { href: "exportacao.html", id: "exportacao", label: "Exportação", icon: "fa-solid fa-download" },
    { href: "planos.html", id: "planos", label: "Planos", icon: "fa-solid fa-rocket" },
    { href: "configuracoes.html", id: "configuracoes", label: "Configurações", icon: "fa-solid fa-gear" },
    { href: "perfil.html", id: "perfil", label: "Perfil", icon: "fa-solid fa-id-badge" },
    { href: "notificacoes.html", id: "notificacoes", label: "Notificações", icon: "fa-solid fa-bell" },
  ];

  function mountSearch() {
    const form = document.getElementById("pf-search-form");
    const input = document.getElementById("pf-search");
    const box = document.getElementById("pf-search-results");
    if (!form || !input || !box) return;
    let timer = null;
    async function run(q) {
      if (!q || q.length < 2) {
        box.hidden = true;
        box.innerHTML = "";
        return;
      }
      try {
        const data = await PF.api.get("/api/busca?q=" + encodeURIComponent(q));
        const clients = data.clientes || [];
        const contracts = data.contratos || [];
        const charges = data.cobrancas || [];
        if (!clients.length && !contracts.length && !charges.length) {
          box.innerHTML = "<p class='hint'>Nada encontrado.</p>";
          box.hidden = false;
          return;
        }
        box.innerHTML =
          (clients.length ? "<p><strong>Clientes</strong></p>" + clients.map((c) => '<a href="cliente.html?id=' + c.id + '">' + PF.escapeHtml(c.nome) + "</a>").join("") : "") +
          (contracts.length ? "<p><strong>Contratos</strong></p>" + contracts.map((c) => '<a href="contratos.html">' + PF.escapeHtml((c.tipo || "Contrato") + " #" + c.id) + "</a>").join("") : "") +
          (charges.length ? "<p><strong>Cobranças</strong></p>" + charges.map((c) => '<a href="agenda.html">' + PF.escapeHtml((c.clienteNome || "Parcela") + " " + PF.formatMoney(c.saldo || c.valor)) + "</a>").join("") : "");
        box.hidden = false;
      } catch (err) {
        box.innerHTML = "<p class='hint'>Busca indisponível.</p>";
        box.hidden = false;
      }
    }
    input.addEventListener("input", () => {
      clearTimeout(timer);
      timer = setTimeout(() => run(input.value.trim()), 220);
    });
    form.addEventListener("submit", (e) => { e.preventDefault(); run(input.value.trim()); });
    document.addEventListener("keydown", (e) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "k") {
        e.preventDefault();
        input.focus();
      }
    });
    document.addEventListener("click", (e) => {
      if (!form.contains(e.target)) box.hidden = true;
    });
  }

  function mountAssistant() {
    if (document.getElementById("pf-assist")) return;
    const wrap = document.createElement("div");
    wrap.id = "pf-assist";
    wrap.innerHTML =
      '<button type="button" class="pf-assist-btn" id="pf-assist-open" aria-label="Abrir assistente"><i class="fa-solid fa-robot" aria-hidden="true"></i></button>' +
      '<section class="pf-assist-panel" id="pf-assist-panel" hidden>' +
      '<header><strong>Assistente PixFactory</strong><button type="button" class="btn-ghost" id="pf-assist-close" aria-label="Fechar">&times;</button></header>' +
      '<p class="hint">Pergunte sobre o dia, atrasos, caixa e juros. Respostas com dados do sistema — sem inventar crédito.</p>' +
      '<div class="pf-assist-log" id="pf-assist-log"></div>' +
      '<form id="pf-assist-form" class="actions-row">' +
      '<label class="sr-only" for="pf-assist-q">Pergunta</label>' +
      '<input id="pf-assist-q" placeholder="Ex.: quem está atrasado?" required>' +
      '<button type="submit">Perguntar</button></form></section>';
    document.body.appendChild(wrap);
    const panel = document.getElementById("pf-assist-panel");
    const log = document.getElementById("pf-assist-log");
    document.getElementById("pf-assist-open").addEventListener("click", () => { panel.hidden = !panel.hidden; });
    document.getElementById("pf-assist-close").addEventListener("click", () => { panel.hidden = true; });
    document.getElementById("pf-assist-form").addEventListener("submit", async (e) => {
      e.preventDefault();
      const input = document.getElementById("pf-assist-q");
      const pergunta = input.value.trim();
      if (!pergunta) return;
      log.innerHTML += "<p><strong>Você:</strong> " + PF.escapeHtml(pergunta) + "</p>";
      input.value = "";
      try {
        const data = await PF.api.post("/api/assistente", { pergunta: pergunta });
        log.innerHTML += "<p><strong>Assistente:</strong> " + PF.escapeHtml(data.resposta) + "</p>";
      } catch (err) {
        log.innerHTML += "<p><strong>Assistente:</strong> " + PF.escapeHtml(err.message || "Não foi possível responder.") + "</p>";
      }
      log.scrollTop = log.scrollHeight;
    });
  }

  PF.layout = {
    async mount(pageId) {
      if (!PF.api.getToken()) {
        window.location.href = "index.html";
        return null;
      }
      try {
        if (!PF.store.hydrated()) await PF.store.hydrate();
      } catch (err) {
        if (err.status === 401) {
          PF.auth.logout();
          return null;
        }
        PF.ui.toast(err.message || "Falha ao carregar dados da API.", "error");
      }
      const user = PF.auth.currentUser();
      if (!user) {
        window.location.href = "index.html";
        return null;
      }

      if (!document.getElementById("pf-fa")) {
        const fa = document.createElement("link");
        fa.id = "pf-fa";
        fa.rel = "stylesheet";
        fa.href = "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css";
        document.head.appendChild(fa);
      }

      const hamburger = document.createElement("button");
      hamburger.className = "hamburger";
      hamburger.type = "button";
      hamburger.setAttribute("aria-label", "Abrir menu");
      hamburger.innerHTML = '<i class="fa-solid fa-bars" aria-hidden="true"></i>';

      const unread = PF.store.notifications.all().filter((n) => !n.read).length;
      const items = NAV.slice();
      if (user.role === "ADMIN") {
        items.splice(12, 0, {
          href: "gerenciamento_usuarios.html",
          id: "usuarios",
          label: "Usuários",
          icon: "fa-solid fa-users-gear",
        });
      }

      const links = items.map((item) => {
        const extra = item.id === "notificacoes" && unread ? " (" + unread + ")" : "";
        return '<li><a href="' + item.href + '" class="' + (item.id === pageId ? "active" : "") + '"><i class="' + item.icon + '" aria-hidden="true"></i> ' + item.label + extra + "</a></li>";
      }).join("");

      const aside = document.createElement("aside");
      aside.className = "sidebar";
      aside.innerHTML =
        '<div class="logo">PixFactory</div>' +
        '<form class="sidebar-search" id="pf-search-form"><label class="sr-only" for="pf-search">Busca</label>' +
        '<input id="pf-search" type="search" placeholder="Buscar · Ctrl+K" autocomplete="off">' +
        '<div class="search-results" id="pf-search-results" hidden></div></form>' +
        "<nav><ul>" + links +
        '<li><a href="' + SUPPORT + '" target="_blank" rel="noopener"><i class="fa-brands fa-whatsapp" aria-hidden="true"></i> Suporte</a></li>' +
        '<li><a href="#" id="pf-suggestion-link"><i class="fa-solid fa-lightbulb" aria-hidden="true"></i> Enviar sugestão</a></li>' +
        '<li><a href="#" id="pf-logout"><i class="fa-solid fa-right-from-bracket" aria-hidden="true"></i> Sair</a></li>' +
        "</ul></nav>" +
        "<footer>PixFactory © 2026 · DEMO</footer>";

      document.body.prepend(aside);
      document.body.prepend(hamburger);

      if (!document.getElementById("suggestion-modal")) {
        const modal = document.createElement("div");
        modal.id = "suggestion-modal";
        modal.className = "modal";
        modal.innerHTML =
          '<div class="modal-content">' +
          '<button class="modal-close" type="button" data-close-modal="suggestion-modal" aria-label="Fechar">&times;</button>' +
          "<h3>Enviar sugestão</h3>" +
          '<label for="suggestion-text">Sua ideia</label>' +
          '<textarea id="suggestion-text" rows="4" placeholder="Como podemos melhorar o PixFactory?"></textarea>' +
          '<p class="field-error" data-error-for="suggestion"></p>' +
          '<div class="actions-row" style="margin-top:12px"><button type="button" id="suggestion-send">Enviar</button></div>' +
          "</div>";
        document.body.appendChild(modal);
      }

      hamburger.addEventListener("click", () => aside.classList.toggle("open"));
      document.addEventListener("click", (event) => {
        if (window.innerWidth > 768) return;
        if (!aside.classList.contains("open")) return;
        if (aside.contains(event.target) || hamburger.contains(event.target)) return;
        aside.classList.remove("open");
      });
      window.addEventListener("resize", () => {
        if (window.innerWidth > 768) aside.classList.remove("open");
      });

      document.getElementById("pf-logout").addEventListener("click", (e) => {
        e.preventDefault();
        PF.auth.logout();
      });
      document.getElementById("pf-suggestion-link").addEventListener("click", (e) => {
        e.preventDefault();
        PF.ui.openModal("suggestion-modal");
      });
      document.getElementById("suggestion-send").addEventListener("click", async () => {
        const text = document.getElementById("suggestion-text").value.trim();
        if (!text) {
          PF.ui.showErrors({ suggestion: "Digite sua sugestão." });
          return;
        }
        try {
          await PF.store.suggestions.add({ text: text });
          document.getElementById("suggestion-text").value = "";
          PF.ui.closeModal("suggestion-modal");
          PF.ui.toast("Sugestão enviada. Obrigado!");
        } catch (err) {
          PF.ui.toast(err.message || "Não foi possível enviar.", "error");
        }
      });

      mountAssistant();
      mountSearch();

      return user;
    },
    financeNav(active) {
      const links = [
        ["controle-de-gastos.html", "visao", "Visão geral"],
        ["caixa.html", "caixa", "Caixa do dia"],
        ["lancamentos.html", "lancamentos", "Lançamentos"],
        ["controle-relatorios.html", "relatorios", "Relatórios"],
        ["limites-gastos.html", "limites", "Limite de gastos"],
        ["conexao-bancaria.html", "bancos", "Conexão bancária"],
      ];
      return '<nav class="subnav" aria-label="Financeiro">' + links.map(([href, id, label]) =>
        '<a href="' + href + '" class="' + (id === active ? "active" : "") + '">' + label + "</a>"
      ).join("") + "</nav>";
    },
    trialBanner(user) {
      if (!user || user.plan === "premium" || user.plan === "pro") return "";
      return '<div class="trial-banner"><span>Você está no plano gratuito. Assine para desbloquear backup e relatórios avançados.</span><a href="planos.html">Ver planos</a></div>';
    },
  };
})(typeof window !== "undefined" ? window : globalThis);
