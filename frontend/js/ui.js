(function (root) {
  const PF = root.PF = root.PF || {};

  function ensureToastWrap() {
    let wrap = document.querySelector(".toast-wrap");
    if (!wrap) {
      wrap = document.createElement("div");
      wrap.className = "toast-wrap";
      wrap.setAttribute("aria-live", "polite");
      document.body.appendChild(wrap);
    }
    return wrap;
  }

  PF.ui = {
    toast(message, type) {
      const wrap = ensureToastWrap();
      const el = document.createElement("div");
      el.className = "toast" + (type ? " " + type : "");
      el.textContent = message;
      wrap.appendChild(el);
      setTimeout(() => el.remove(), 3200);
    },
    confirm(message) {
      return window.confirm(message);
    },
    openModal(id) {
      const el = document.getElementById(id);
      if (el) el.classList.add("open");
    },
    closeModal(id) {
      const el = document.getElementById(id);
      if (el) el.classList.remove("open");
    },
    showErrors(map) {
      document.querySelectorAll("[data-error-for]").forEach((el) => { el.textContent = ""; });
      Object.keys(map).forEach((key) => {
        const el = document.querySelector('[data-error-for="' + key + '"]');
        if (el) el.textContent = map[key];
      });
    },
    empty(text) {
      return '<div class="empty">' + PF.escapeHtml(text) + "</div>";
    },
  };

  document.addEventListener("click", (event) => {
    const closer = event.target.closest("[data-close-modal]");
    if (closer) PF.ui.closeModal(closer.getAttribute("data-close-modal"));
  });
})(typeof window !== "undefined" ? window : globalThis);
