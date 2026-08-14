(function (root) {
  const PF = root.PF = root.PF || {};
  const TOKEN_KEY = "pixfactory.token";
  const PREFS_KEY = "pixfactory.prefs";

  function storage() {
    if (typeof localStorage !== "undefined") return localStorage;
    const data = {};
    return {
      getItem: (k) => (Object.prototype.hasOwnProperty.call(data, k) ? data[k] : null),
      setItem: (k, v) => { data[k] = String(v); },
      removeItem: (k) => { delete data[k]; },
    };
  }

  const mem = storage();

  PF.api = {
    TOKEN_KEY,
    PREFS_KEY,
    getToken() {
      return mem.getItem(TOKEN_KEY);
    },
    setToken(token) {
      if (token) mem.setItem(TOKEN_KEY, token);
      else mem.removeItem(TOKEN_KEY);
    },
    getPrefs() {
      try {
        return JSON.parse(mem.getItem(PREFS_KEY) || "{}");
      } catch (err) {
        return {};
      }
    },
    savePrefs(patch) {
      const next = Object.assign({}, this.getPrefs(), patch);
      mem.setItem(PREFS_KEY, JSON.stringify(next));
      return next;
    },
    async request(method, path, body) {
      const headers = { Accept: "application/json" };
      if (body !== undefined) headers["Content-Type"] = "application/json";
      const token = this.getToken();
      if (token) headers.Authorization = "Bearer " + token;
      let response;
      try {
        response = await fetch(path, {
          method: method,
          headers: headers,
          body: body !== undefined ? JSON.stringify(body) : undefined,
        });
      } catch (err) {
        const offline = new Error("API offline. Verifique se o backend está em execução.");
        offline.status = 0;
        throw offline;
      }
      if (response.status === 204) return null;
      const text = await response.text();
      let data = null;
      if (text) {
        try { data = JSON.parse(text); } catch (err) { data = { message: text }; }
      }
      if (!response.ok) {
        const error = new Error((data && data.message) || "Erro na requisição.");
        error.status = response.status;
        error.payload = data;
        throw error;
      }
      return data;
    },
    get(path) { return this.request("GET", path); },
    post(path, body) { return this.request("POST", path, body || {}); },
    put(path, body) { return this.request("PUT", path, body || {}); },
    del(path) { return this.request("DELETE", path); },
  };
})(typeof window !== "undefined" ? window : globalThis);
