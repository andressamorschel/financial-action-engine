# Testes

Use Java 25, conforme o toolchain do projeto.

Para executar os testes de integração com REST Assured:

```sh
./gradlew integrationTest
```

O Docker precisa estar disponível. O Testcontainers cria um PostgreSQL 17 temporário,
e o Flyway aplica as migrations. O banco de desenvolvimento e o Keycloak local
não são necessários. A aplicação sobe em uma porta aleatória.

Os testes substituem apenas o decoder de JWT por um mock; os filtros de segurança,
a conversão de permissões, o controller, o serviço e o repositório são reais.
Não validam a assinatura de tokens nem a integração com o Keycloak.

Os testes de integração ficam em `src/integrationTest/java` e executam na tarefa
`integrationTest`. A classe abstrata `integration/config/IntegrationTest` concentra
a configuração de PostgreSQL, HTTP e tokens de teste. A
`integration/fixture/ActionFixture` expõe métodos estáticos que recebem o payload
criado no IT, executam o POST com autenticação e retornam a resposta HTTP. Os testes unitários ficam em
`src/test/java`.
`./gradlew check` executa tanto `test` quanto `integrationTest`.
O `SecurityConfigTest` existente continua na tarefa `test` e usa a configuração
normal da aplicação.
