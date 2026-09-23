document.addEventListener("DOMContentLoaded", () => {
  // Apenas interações da vitrine pública.
  // Não altera login, dashboard, API, banco ou qualquer lógica do PixFactory.
  document.querySelectorAll('a[href^="#"]').forEach((link) => {
    link.addEventListener("click", (event) => {
      const target = document.querySelector(link.getAttribute("href"));
      if (!target) return;
      event.preventDefault();
      target.scrollIntoView({ behavior: "smooth", block: "start" });
    });
  });
});
