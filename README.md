# Sina

Aviso de golpe para quem você ama.

O Sina é um app Android que protege idosos de golpes recebidos por SMS e WhatsApp. Ele lê o texto das notificações, um LLM avalia se a mensagem parece golpe e, em segundos, o idoso recebe um aviso simples e um familiar é notificado.

Projeto do **Hack Brasil 2026**, trilha **Cibersegurança**. Site: https://sina.ia.br

## Como funciona

1. O app do protegido lê as notificações de SMS e WhatsApp (`NotificationListenerService`).
2. Um pré-filtro local descarta o que não importa (códigos de verificação, contatos salvos, grupos).
3. O texto é anonimizado no aparelho (CPF, telefones, e-mails, nomes e valores).
4. A API em Java envia o texto anonimizado ao LLM, que devolve um JSON com veredito (`SCAM`, `SUSPICIOUS` ou `SAFE`), categoria do golpe, confiança e o texto do aviso.
5. O idoso recebe o alerta. Se for golpe com confiança de pelo menos 0,8, o familiar recebe push (FCM) e e-mail (Resend) com a categoria, um resumo e um trecho anonimizado da mensagem.
6. Se o app do idoso parar de enviar sinal de vida por 30 minutos, o familiar é avisado de que a proteção parou.

O Sina **não** lê conversas abertas, áudio de ligações nem usa a API do WhatsApp: só o texto que aparece na notificação.

## Papéis

- **Protegido:** instala o app, concede a permissão e recebe os alertas.
- **Familiar:** pareia com o protegido por código ou QR, recebe alertas e vê o histórico e o estado da proteção.

## Stack

| Camada | Tecnologia |
| --- | --- |
| App Android | Kotlin |
| API | Java 21 e Spring Boot 4 |
| Banco | PostgreSQL 18 com Flyway |
| LLM | OpenAI (Responses API, `gpt-6-luna`) |
| Push | Firebase Cloud Messaging (Admin SDK) |
| E-mail | Resend |

## Estrutura

```
sina/
├── app/    app Android (Kotlin), em desenvolvimento
├── api/    API (Java, Spring Boot)
├── docs/   documentação do projeto
└── README.md
```

A documentação completa (arquitetura, modelo de dados, contrato da API e plano) está em [`docs/sina-documentacao.html`](docs/sina-documentacao.html).

## API

| Método e caminho | Papel | O que faz |
| --- | --- | --- |
| `GET /health` | público | Verifica se a API está no ar |
| `POST /v1/devices` | público | Registra o dispositivo e devolve o token |
| `PUT /v1/devices/me/fcm-token` | ambos | Atualiza o token de push |
| `POST /v1/devices/me/heartbeat` | protegido | Sinal de vida do app do idoso |
| `POST /v1/pairing-codes` | protegido | Gera o código de pareamento |
| `POST /v1/pairings` | familiar | Usa o código e cria o vínculo |
| `GET /v1/pairings` | ambos | Lista os vínculos e o estado da proteção |
| `DELETE /v1/pairings/{id}` | ambos | Desfaz o vínculo |
| `POST /v1/analyze` | protegido | Analisa o texto anonimizado de uma notificação |
| `GET /v1/alerts` | familiar | Histórico de alertas paginado |

Autenticação por `Authorization: Bearer <deviceToken>`; a API guarda só o hash do token.

## Como rodar

### API (`api/`)

1. Requisitos: Java 21 e Docker (ou um PostgreSQL local).
2. Copie `api/.env.example` para `api/.env` e preencha:
   - chave e modelo da OpenAI;
   - nome do banco, usuário e senha;
   - chave do Resend e remetente;
   - caminho do JSON da conta de serviço do Firebase, escrito com `/`;
   - `DEVICE_TOKEN_PEPPER` com pelo menos 32 caracteres.
3. Suba a API e o banco com Docker:
   ```
   cd api
   docker compose up -d --build
   ```
   A API responde em `http://localhost:8081`.
4. Ou rode direto pelo Maven, com o PostgreSQL já no ar:
   ```
   ./mvnw spring-boot:run      # Linux e macOS
   .\mvnw.cmd spring-boot:run  # Windows
   ```
5. Testes: `./mvnw test` (precisa de um PostgreSQL; as migrações rodam sozinhas).

Sem as credenciais do Firebase ou do Resend, a API sobe e só deixa de enviar por aquele canal.

### App (`app/`)

1. Abra a pasta `app/` no Android Studio.
2. Coloque o `google-services.json` do seu projeto Firebase em `app/app/` (não versionado).
3. Rode em um aparelho ou emulador com Android 13 ou superior.

No Android 13+, se o APK for instalado fora da Play Store, libere "Permitir configurações restritas" nas informações do app antes de conceder o acesso às notificações.

## Privacidade

- O texto da mensagem é anonimizado no aparelho antes de sair dele.
- A OpenAI recebe só o texto anonimizado, sem armazenamento (`store: false`).
- O banco guarda vínculos e alertas, nunca o texto da mensagem. O cache usa só o hash do texto anonimizado e expira em 7 dias.
- Em caso de golpe, o familiar recebe a categoria, um resumo e um trecho de até 300 caracteres do texto já anonimizado. O trecho é enviado pelo Firebase Cloud Messaging (push) e pelo Resend (e-mail), mas não é gravado no banco. O consentimento do idoso cita a OpenAI, o Firebase e o Resend.
- As demos usam apenas mensagens sintéticas.

## Limitações

- Funciona só no Android.
- Não vê mensagens sem notificação nem notificações com pré-visualização oculta.
- A classificação é uma estimativa de risco, não uma certeza.

## Equipe

2 pessoas. Preencha os nomes e os links dos perfis.

## Licença

Veja o arquivo `LICENSE`.
