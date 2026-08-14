(function (root) {
  const PF = root.PF = root.PF || {};
  let cache = emptyCache();
  let hydrated = false;

  function emptyCache() {
    return {
      user: null,
      users: [],
      clients: [],
      contracts: [],
      launches: [],
      payables: [],
      receivables: [],
      budgets: [],
      accounts: [],
      banks: [],
      notifications: [],
      suggestions: [],
      appointments: [],
      payments: [],
      emails: [],
      activity: [],
      settings: {
        companyName: "PixFactory",
        slogan: "Gestão de empréstimos e cobranças",
        defaultInterestRate: 2.5,
        referralReward: 50,
        emailNotifications: true,
        paymentReminders: true,
        theme: "dark",
        hideValues: false,
      },
      metrics: {},
    };
  }

  function normalize(item) {
    if (!item || typeof item !== "object") return item;
    const copy = Object.assign({}, item);
    ["id", "clienteId", "clientId", "contractId", "userId"].forEach((key) => {
      if (copy[key] != null) copy[key] = String(copy[key]);
    });
    if (copy.readFlag != null) copy.read = !!copy.readFlag;
    if (copy.fotoUrl && !copy.foto) copy.foto = copy.fotoUrl;
    if (Array.isArray(copy.historicoJson) && !copy.historico) copy.historico = copy.historicoJson;
    return copy;
  }

  function list(name) {
    return (cache[name] || []).map(normalize);
  }

  function replace(name, items) {
    cache[name] = (items || []).map(normalize);
    return cache[name];
  }

  function upsert(name, item) {
    const normalized = normalize(item);
    const items = cache[name] || [];
    const idx = items.findIndex((x) => String(x.id) === String(normalized.id));
    if (idx >= 0) items[idx] = Object.assign({}, items[idx], normalized);
    else items.push(normalized);
    cache[name] = items;
    return normalized;
  }

  function collection(name, basePath, options) {
    const opts = options || {};
    return {
      all() { return list(name); },
      get(id) { return list(name).find((item) => String(item.id) === String(id)) || null; },
      async add(item) {
        const created = await PF.api.post(basePath, item);
        return upsert(name, created);
      },
      async update(id, patch) {
        const updated = await PF.api.put(basePath + "/" + id, patch);
        return upsert(name, updated || Object.assign({}, this.get(id), patch));
      },
      async remove(id) {
        await PF.api.del(basePath + "/" + id);
        cache[name] = list(name).filter((item) => String(item.id) !== String(id));
      },
      path: basePath,
      extra: opts,
    };
  }

  PF.store = {
    hydrated: () => hydrated,
    cache: () => cache,
    async hydrate() {
      const data = await PF.api.get("/api/bootstrap");
      cache = Object.assign(emptyCache(), data);
      cache.clients = (data.clients || []).map(normalize);
      cache.contracts = (data.contracts || []).map(normalize);
      cache.launches = (data.launches || []).map(normalize);
      cache.payables = (data.payables || []).map(normalize);
      cache.receivables = (data.receivables || []).map(normalize);
      cache.budgets = (data.budgets || []).map(normalize);
      cache.accounts = (data.accounts || []).map(normalize);
      cache.banks = (data.banks || []).map(normalize);
      cache.notifications = (data.notifications || []).map(normalize);
      cache.suggestions = (data.suggestions || []).map(normalize);
      cache.appointments = (data.appointments || []).map(normalize);
      cache.payments = (data.payments || []).map(normalize);
      cache.emails = (data.emails || []).map(normalize);
      cache.activity = data.activity || [];
      cache.settings = Object.assign(emptyCache().settings, data.settings || {}, PF.api.getPrefs());
      cache.user = data.user ? normalize(data.user) : null;
      cache.metrics = data.metrics || {};
      try {
        cache.users = (await PF.api.get("/api/users")).map(normalize);
      } catch (err) {
        cache.users = cache.user ? [cache.user] : [];
      }
      hydrated = true;
      return cache;
    },
    users: collection("users", "/api/users"),
    clients: collection("clients", "/api/clients"),
    contracts: collection("contracts", "/api/contracts"),
    launches: collection("launches", "/api/launches"),
    payables: collection("payables", "/api/payables"),
    receivables: collection("receivables", "/api/receivables"),
    budgets: collection("budgets", "/api/budgets"),
    accounts: collection("accounts", "/api/accounts"),
    banks: collection("banks", "/api/banks"),
    notifications: collection("notifications", "/api/notifications"),
    suggestions: collection("suggestions", "/api/suggestions"),
    appointments: collection("appointments", "/api/appointments"),
    payments: collection("payments", "/api/payments"),
    emails: {
      all() { return list("emails"); },
    },
    getSettings() {
      return Object.assign({}, cache.settings, PF.api.getPrefs());
    },
    async saveSettings(patch) {
      const localKeys = { hideValues: true, theme: true };
      const local = {};
      const remote = {};
      Object.keys(patch || {}).forEach((key) => {
        if (localKeys[key]) local[key] = patch[key];
        else remote[key] = patch[key];
      });
      if (Object.keys(local).length) PF.api.savePrefs(local);
      if (Object.keys(remote).length) {
        cache.settings = Object.assign({}, cache.settings, await PF.api.put("/api/settings", remote));
      }
      cache.settings = Object.assign({}, cache.settings, local);
      return cache.settings;
    },
    async addActivity(entry) {
      cache.activity = [entry].concat(cache.activity || []);
    },
    activity() { return cache.activity || []; },
    currentUser() { return cache.user; },
    setCurrentUser(user) { cache.user = user ? normalize(user) : null; },
    clientContracts(clientId) {
      return list("contracts").filter((c) => String(c.clienteId) === String(clientId));
    },
    metrics() {
      return cache.metrics || {};
    },
    async refreshMetrics() {
      cache.metrics = await PF.api.get("/api/dashboard");
      return cache.metrics;
    },
    exportJson() {
      return JSON.stringify({
        clients: cache.clients,
        contracts: cache.contracts,
        launches: cache.launches,
        payables: cache.payables,
        receivables: cache.receivables,
        budgets: cache.budgets,
        accounts: cache.accounts,
        banks: cache.banks,
        notifications: cache.notifications,
        appointments: cache.appointments,
        payments: cache.payments,
      }, null, 2);
    },
    importJson() {
      throw new Error("A restauração agora é feita pelo banco MySQL. Recrie o ambiente com o seed DEMO.");
    },
    async contractAction(id, action, body) {
      const updated = await PF.api.post("/api/contracts/" + id + "/actions/" + action, body || {});
      return upsert("contracts", updated);
    },
    async createPixPayment(contractId, valor) {
      const payment = await PF.api.post("/api/contracts/" + contractId + "/payments", { method: "PIX", valor: valor });
      return upsert("payments", payment);
    },
    async confirmPayment(id) {
      const payment = await PF.api.post("/api/payments/" + id + "/confirm");
      upsert("payments", payment);
      await this.hydrate();
      return payment;
    },
  };
})(typeof window !== "undefined" ? window : globalThis);
