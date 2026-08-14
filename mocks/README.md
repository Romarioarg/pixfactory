# Mocks substituíveis

Este diretório documenta as integrações fictícias do PixFactory.

| Integração | Interface | Implementação atual | Substituição futura |
|---|---|---|---|
| Pix | `PixService` | `DemoPixService` | `RealPixService` |
| Pagamentos | `PaymentGateway` | `DemoPaymentGateway` | `RealPaymentGateway` |
| Open Finance | `OpenFinanceService` | `DemoOpenFinanceService` | `RealOpenFinanceService` |
| E-mail | `EmailService` | `DemoEmailService` | `RealEmailService` |

As implementações reais **não** estão neste repositório. Basta criar uma classe `@Service` e remover o `@Primary` da versão DEMO.
