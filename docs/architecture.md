# Arquitetura PixFactory

```text
Browser (HTML/CSS/JS)
        |
        |  REST JSON + JWT
        v
Spring Boot API  (/api)
        |
        |  JPA
        v
MySQL
```

Camadas do backend:

`web` → `service` → `repository` → `entity`

Integrações externas passam por interfaces em `integration`, com implementações DEMO em `integration.demo`.
