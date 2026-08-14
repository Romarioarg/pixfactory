(function (root) {
  const PF = root.PF = root.PF || {};
  const SUPPORT = "https://wa.me/5511999990000?text=" + encodeURIComponent("Olá! Preciso de ajuda com o PixFactory DEMO.");

  const NAV = [
    { href: "dashboard.html", id: "dashboard", label: "Dashboard", icon: "🏠" },
    { href: "clientes.html", id: "clientes", label: "Clientes", icon: "👤" },
    { href: "cadastro.html", id: "cadastro", label: "Novo Cliente", icon: "➕" },
    { href: "contratos.html", id: "contratos", label: "Contratos", icon: "📄" },
    { href: "agenda.html", id: "agenda", label: "Agenda", icon: "📆" },
    { href: "simulador.html", id: "simulador", label: "Simulador", icon: "📊" },
    { href: "controle-de-gastos.html", id: "gastos", label: "Controle de Gastos", icon: "💰" },
    { href: "exportacao.html", id: "exportacao", label: "Exportação", icon: "📥" },
    { href: "planos.html", id: "planos", label: "Planos", icon: "🚀" },
    { href: "configuracoes.html", id: "configuracoes", label: "Configurações", icon: "⚙️" },
    { href: "perfil.html", id: "perfil", label: "Perfil", icon: "🧑‍💼" },
    { href: "notificacoes.html", id: "notificacoes", label: "Notificações", icon: "🔔" },
  ];

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

      const hamburger = document.createElement("button");
      hamburger.className = "hamburger";
      hamburger.type = "button";
      hamburger.setAttribute("aria-label", "Abrir menu");
      hamburger.textContent = "☰";

      const unread = PF.store.notifications.all().filter((n) => !n.read).length;
      const items = NAV.slice();
      if (user.role === "ADMIN") {
        items.splice(10, 0, {
          href: "gerenciamento_usuarios.html",
          id: "usuarios",
          label: "Usuários",
          icon: "👥",
        });
      }

      const links = items.map((item) => {
        const extra = item.id === "notificacoes" && unread ? " (" + unread + ")" : "";
        return '<li><a href="' + item.href + '" class="' + (item.id === pageId ? "active" : "") + '"><span>' + item.icon + "</span> " + item.label + extra + "</a></li>";
      }).join("");

      const aside = document.createElement("aside");
      aside.className = "sidebar";
      aside.innerHTML =
        '<div class="logo">PixFactory</div>' +
        "<nav><ul>" + links +
        '<li><a href="' + SUPPORT + '" target="_blank" rel="noopener">🆘 Suporte</a></li>' +
        '<li><a href="#" id="pf-suggestion-link">💡 Enviar sugestão</a></li>' +
        '<li><a href="#" id="pf-logout">🚪 Sair</a></li>' +
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

      return user;
    },
    financeNav(active) {
      const links = [
        ["controle-de-gastos.html", "visao", "Visão geral"],
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
