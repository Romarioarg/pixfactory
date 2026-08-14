const fs = require("fs");
const path = require("path");
const vm = require("vm");

const root = path.join(__dirname, "..");
const context = { console, globalThis: {} };
context.globalThis = context;
vm.createContext(context);

function load(file) {
  const code = fs.readFileSync(path.join(root, file), "utf8");
  vm.runInContext(code, context, { filename: file });
}

load("js/format.js");
load("js/validators.js");

const PF = context.PF;
let failed = 0;
function assert(cond, msg) {
  if (!cond) {
    failed += 1;
    console.error("FAIL:", msg);
  } else {
    console.log("OK:", msg);
  }
}

assert(PF.formatMoney(1500).includes("1.500"), "formatMoney uses pt-BR");
assert(PF.formatDate("2026-08-13") === "13/08/2026", "formatDate");
assert(PF.validators.email("a@b.com") === "", "valid email");
assert(PF.validators.email("x") !== "", "invalid email");
assert(PF.validators.cpf("529.982.247-25") === "", "valid CPF");
assert(PF.validators.cpf("000.000.000-00") !== "", "invalid CPF");
assert(PF.validators.password("Demo@123") === "", "valid password");
assert(PF.validators.password("123") !== "", "weak password");
assert(PF.statusLabel("atrasado") === "Em atraso", "status label");

if (failed) {
  console.error("\n" + failed + " test(s) failed");
  process.exit(1);
}
console.log("\nAll frontend unit tests passed");
