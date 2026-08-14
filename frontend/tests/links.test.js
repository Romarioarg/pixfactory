const fs = require("fs");
const path = require("path");

const root = path.join(__dirname, "..");
const htmlFiles = fs.readdirSync(root).filter((f) => f.endsWith(".html"));
const existing = new Set(htmlFiles);
let failed = 0;

function assert(cond, msg) {
  if (!cond) {
    failed += 1;
    console.error("FAIL:", msg);
  } else {
    console.log("OK:", msg);
  }
}

assert(htmlFiles.length >= 20, "expected the original pages to still exist");

const required = [
  "index.html", "cadastro-login.html", "recuperar_senha.html", "dashboard.html",
  "clientes.html", "cadastro.html", "editar-cliente.html", "contratos.html",
  "novo_contrato.html", "agenda.html", "simulador.html", "exportacao.html",
  "controle-de-gastos.html", "lancamentos.html", "limites-gastos.html",
  "controle-relatorios.html", "conexao-bancaria.html", "configuracoes.html",
  "perfil.html", "editar_perfil.html", "notificacoes.html", "planos.html",
  "gerenciamento_usuarios.html", "novos-mes.html",
];
required.forEach((file) => assert(existing.has(file), "page exists: " + file));

const hrefRe = /(?:href|src)=["']([^"']+)["']/g;
htmlFiles.forEach((file) => {
  const html = fs.readFileSync(path.join(root, file), "utf8");
  assert(html.includes("lang=\"pt-BR\""), file + " has lang");
  assert(html.includes("viewport"), file + " has viewport");
  assert(!html.includes("72641400rR@"), file + " has no hardcoded personal password");
  assert(!html.includes("arg.dev.java@gmail.com"), file + " has no hardcoded personal email");
  let match;
  while ((match = hrefRe.exec(html))) {
    const ref = match[1];
    if (ref.startsWith("http") || ref.startsWith("mailto:") || ref.startsWith("#") || ref.startsWith("data:")) continue;
    const clean = ref.split("?")[0];
    if (clean.endsWith(".html")) {
      assert(fs.existsSync(path.join(root, clean)), file + " -> " + clean);
    } else if (clean.endsWith(".js") || clean.endsWith(".css") || clean.endsWith(".svg")) {
      assert(fs.existsSync(path.join(root, clean)), file + " asset " + clean);
    }
  }
});

["css/app.css", "js/api.js", "js/store.js", "js/auth.js", "js/layout.js", "js/format.js", "js/ui.js", "js/validators.js"].forEach((file) => {
  assert(fs.existsSync(path.join(root, file)), "shared file " + file);
});

if (failed) {
  console.error("\n" + failed + " test(s) failed");
  process.exit(1);
}
console.log("\nAll link/structure tests passed");
