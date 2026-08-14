(function (root) {
  const PF = root.PF = root.PF || {};

  PF.auth = {
    currentUser() {
      return PF.store.currentUser();
    },
    requireUser() {
      if (!PF.api.getToken()) {
        window.location.href = "index.html";
        return null;
      }
      return PF.store.currentUser();
    },
    async login(email, password) {
      try {
        const data = await PF.api.post("/api/auth/login", { email: email, password: password });
        PF.api.setToken(data.token);
        PF.store.setCurrentUser(data.user);
        return { ok: true, user: data.user };
      } catch (err) {
        return { ok: false, error: err.message || "E-mail ou senha incorretos." };
      }
    },
    async register({ name, email, phone, password }) {
      try {
        const data = await PF.api.post("/api/auth/register", { name, email, phone, password });
        PF.api.setToken(data.token);
        PF.store.setCurrentUser(data.user);
        return { ok: true, user: data.user };
      } catch (err) {
        return { ok: false, error: err.message || "Não foi possível cadastrar." };
      }
    },
    logout() {
      PF.api.setToken(null);
      window.location.href = "index.html";
    },
    async changePassword(userId, currentPassword, nextPassword) {
      try {
        await PF.api.post("/api/auth/change-password", { currentPassword, nextPassword });
        return { ok: true };
      } catch (err) {
        return { ok: false, error: err.message || "Não foi possível alterar a senha." };
      }
    },
    async forgotPassword(email) {
      try {
        return { ok: true, data: await PF.api.post("/api/auth/forgot-password", { email }) };
      } catch (err) {
        return { ok: false, error: err.message || "E-mail não encontrado." };
      }
    },
    async resetPassword(email, code, nextPassword) {
      try {
        await PF.api.post("/api/auth/reset-password", { email, code, nextPassword });
        return { ok: true };
      } catch (err) {
        return { ok: false, error: err.message || "Não foi possível redefinir a senha." };
      }
    },
  };
})(typeof window !== "undefined" ? window : globalThis);
