# Sina

Aviso de golpe para quem você ama.

O Sina é um app Android que protege idosos de golpes recebidos por SMS e WhatsApp. Ele lê o texto das notificações, um LLM avalia se a mensagem parece golpe e, em segundos, o idoso recebe um aviso simples e um familiar é notificado.

Projeto do **Hack Brasil 2026**, trilha **Cibersegurança**. Site: https://sina.ia.br

## Como funciona

1. O app do protegido lê as notificações de SMS e WhatsApp (`NotificationListenerService`).
2. Um pré-filtro local descarta o que não importa (códigos de verificação, contatos salvos, grupos).
3. O texto é anonimizado no aparelho (CPF, telefones, e-mails, nomes e valores).
4. A API em Java envia o texto anonimizado ao LLM, que devolve um JSON com veredito (`golpe`, `suspeito` ou `seguro`), tipo de golpe, confiança e o texto do aviso.
5. O idoso recebe o alerta. Se o risco é alto, o familiar recebe push (FCM) e e-mail (Resend) com um resumo.

O Sina **não** lê conversas abertas, áudio de ligações nem usa a API do WhatsApp: só o texto que aparece na notificação.

## Papéis

- **Protegido:** instala o app, concede a permissão e recebe os alertas.
- **Familiar:** pareia com o protegido por código ou QR, recebe alertas e vê o histórico e o estado da proteção.

## Stack

| Camada | Tecnologia |
| --- | --- |
| App Android | Kotlin |
| API | Java e Spring Boot |
| Banco | PostgreSQL |
| LLM | OpenAI API |
| Push | Firebase Cloud Messaging |
| E-mail | Resend |

## Estrutura

```
sina/
├── app/    app Android (Kotlin)
├── api/    API (Java, Spring Boot)
├── docs/   documentação do projeto
└── README.md
```

## Como rodar

> Preencha conforme o projeto avançar.

### API (`api/`)

1. Requisitos: Java 21 e PostgreSQL.
2. Copie `api/.env.example` para `api/.env` e preencha as variáveis (chave da OpenAI, chave do Resend, credenciais do Firebase e do banco).
3. Execute a API pelo Maven ou Gradle do projeto.

### App (`app/`)

1. Abra a pasta `app/` no Android Studio.
2. Coloque o `google-services.json` do seu projeto Firebase em `app/app/` (não versionado).
3. Rode em um aparelho ou emulador com Android 13 ou superior.

No Android 13+, se o APK for instalado fora da Play Store, libere "Permitir configurações restritas" nas informações do app antes de conceder o acesso às notificações.

## Privacidade

- O texto da mensagem é anonimizado no aparelho antes de sair dele.
- O banco guarda vínculos e alertas, nunca o texto original da mensagem. O cache usa só o hash do texto anonimizado.
- O familiar recebe apenas um resumo do alerta, não a conversa.
- As demos usam apenas mensagens sintéticas.

## Limitações

- Funciona só no Android.
- Não vê mensagens sem notificação nem notificações com pré-visualização oculta.
- A classificação é uma estimativa de risco, não uma certeza.

## Equipe

2 pessoas. Preencha os nomes e os links dos perfis.

## Licença

Veja o arquivo `LICENSE`.
