# Testes unitários (JUnit)

Cobre regras de domínio e, quando as classes existirem no backend desta branch, o motor financeiro.

```bash
cd tests/unit
mvn test
```

O Maven copia apenas as fontes listadas em `pom.xml` a partir de `backend/src/main/java`. O backend original não é modificado.

Classes alvo:

- `ContractStatus`, `AccountStatus`, `PaymentStatus`, `Role`
- `ApiException` / `ConflictException` / `NotFoundException`
- `LoanEngine` e `PaymentAllocator` (se presentes no backend)
