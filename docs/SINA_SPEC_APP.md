# Sina — Especificação completa (produto, regras de negócio, design e telas)

> Use este arquivo como contexto do projeto. Salve na raiz do projeto Android como `SINA_SPEC.md` (ou `CLAUDE.md`) e peça ao Claude Code para implementar por etapas.

## 1. Visão

**Sina** é um app Android que avisa quando uma mensagem de SMS ou WhatsApp parece golpe. Foi pensado para pessoas idosas (papel **PROTECTED**, "pessoa protegida") e para um familiar (papel **GUARDIAN**, "familiar") que recebe o alerta na hora.

- **Sem cadastro e sem senha** para quem é protegido. O familiar informa só nome e e-mail (opcional).
- Mesmo app, dois papéis, escolhidos na primeira abertura.
- Idioma: português do Brasil. Todos os textos da interface estão neste documento.
- Tom: calmo, direto, frases curtas, sem jargão. Letras grandes, botões grandes (mín. 48 dp; botões principais 56 dp).

## 2. Papéis

| Papel | Chave | O que faz |
|---|---|---|
| Pessoa protegida | `PROTECTED` | Dá acesso às notificações. O app lê SMS/WhatsApp, anonimiza e analisa. Mostra o aviso na tela e notifica. Gera código de pareamento. |
| Familiar | `GUARDIAN` | Pareia com uma ou mais pessoas protegidas. Recebe push e e-mail em caso de golpe. Vê painel, histórico e inatividade. |

Um familiar pode proteger várias pessoas. Uma pessoa protegida pode ter vários familiares.

## 3. Regras de negócio

### 3.1 Leitura e análise de mensagens (celular da pessoa protegida)
1. `NotificationListenerService` recebe notificações. Só processa pacotes de **SMS** (app de mensagens padrão) e **WhatsApp** (`com.whatsapp`, `com.whatsapp.w4b`). Ignora o resto.
2. Antes de sair do celular, o texto é **anonimizado localmente**: nomes de contatos, números de telefone, valores em dinheiro e códigos são trocados por marcadores: `[NOME]`, `[TELEFONE]`, `[VALOR]`, `[CÓDIGO]`. Também URLs podem ser mantidas (o domínio é sinal importante) mas sem dados pessoais.
3. O texto anonimizado é enviado ao backend, que o analisa com um modelo de IA (OpenAI, conforme o texto de consentimento; o provedor deve ser configurável).
4. O backend retorna um veredito:
   - `SCAM` (golpe)
   - `SUSPICIOUS` (suspeita)
   - `SAFE` (seguro; nada é mostrado ao usuário)
   Além de: `category` (ex.: falso parente, falsa central de banco, link suspeito/entrega), `summary` (1 frase), `confidence` (0 a 1), `redactedExcerpt` (trecho sem dados pessoais).
5. **Nenhuma mensagem é guardada.** Só o resultado da análise (veredito, categoria, resumo, confiança, trecho anonimizado, origem e hora) é persistido.
6. Falha de rede: enfileirar localmente (WorkManager) por tempo curto; se não analisar a tempo, descartar. Nunca bloquear a notificação original.

### 3.2 O que cada veredito dispara
| Veredito | Pessoa protegida | Familiar |
|---|---|---|
| `SCAM` | Notificação de alta prioridade + tela "Alerta de golpe" | Push `SCAM_ALERT` (FCM) + e-mail (se cadastrado) |
| `SUSPICIOUS` | Notificação + tela "Alerta de suspeita" | **Nada** (avisos de suspeita não são enviados ao familiar) |
| `SAFE` | Nada | Nada |

O alerta do familiar contém: tipo (golpe), categoria, resumo, origem (WhatsApp/SMS), hora e trecho anonimizado.

### 3.3 Pareamento
1. Na pessoa protegida: **Ligar familiar** gera um código numérico de **6 dígitos** (exibido como `483 920`) e um QR code com o mesmo código. **Uso único** e **expira em 10 minutos** (mostrar contagem regressiva "Expira em 9:42"). Há botão "Gerar novo código".
2. No familiar: escaneia o QR ou digita o código. Se válido, cria o vínculo. Se expirado: "Código expirado. Gere um novo no celular da pessoa protegida."
3. O vínculo guarda `protectedId`, `guardianId`, data. Aparece em Ajustes da pessoa protegida ("Ligado desde 09/11/2026") com ação **Desligar**. Desligar remove o vínculo e o familiar para de receber alertas daquela pessoa.
4. Pessoa protegida pode ligar outro familiar ("Ligar outro familiar").

### 3.4 Identidade sem cadastro
- Sem login. No primeiro uso, o app gera um `deviceId` (UUID) e registra no backend junto com o papel e o token FCM. Autenticação do app com o backend por token anônimo assinado (ex.: Firebase Anonymous Auth).
- Familiar: informa **nome** (aparece no celular de quem ele protege) e **e-mail opcional** (só para alertas de golpe, sem propaganda).

### 3.5 Inatividade da proteção (heartbeat)
- O celular da pessoa protegida envia um sinal periódico ("último sinal") ao backend (WorkManager, a cada ~15 min, mais um sinal ao receber/analisar mensagem).
- Se não houver sinal por mais de um limite (ex.: 60 min), o backend marca a pessoa como **inativa** e envia ao familiar o push `PROTECTION_INACTIVE` ("Proteção do Antônio parou. O celular dele não manda sinal desde 12:10. Peça para ele abrir o Sina."). Enviar uma vez por episódio. Quando o sinal volta, o aviso some sozinho e o status volta a "Ativa".
- Causas comuns mostradas ao familiar: acesso às notificações desligado; economia de bateria fechou o Sina; celular desligado ou sem internet.
- O app deve pedir: acesso às notificações (obrigatório), ignorar otimização de bateria (recomendado), permissão de notificações do Android 13+ (POST_NOTIFICATIONS).

### 3.5.1 Permissões Android
`BIND_NOTIFICATION_LISTENER_SERVICE` (via serviço), `POST_NOTIFICATIONS`, `INTERNET`, `CAMERA` (escanear QR), `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, `RECEIVE_BOOT_COMPLETED`. Declaração de uso do acesso às notificações é exigida na Play Store.

### 3.6 Consentimento e privacidade (LGPD)
Antes de ativar a proteção, a pessoa vê a tela Consentimento e marca "Li e concordo com o uso descrito acima". Pontos: dados pessoais ficam no celular; análise por IA (OpenAI, não guarda); familiar é avisado via Firebase (Google) e e-mail (Resend); nada de texto guardado. Link para a política de privacidade.

### 3.7 Backend (sugestão)
- Stack sugerida: **Firebase** (Auth anônimo, Firestore, Cloud Functions, FCM) ou Supabase + Edge Functions. Resend para e-mail. OpenAI para classificação (chave só no servidor, nunca no app).
- Coleções/tabelas:
  - `devices` { id, role, name?, email?, fcmToken, lastSeenAt, protectionActive }
  - `links` { id, protectedId, guardianId, createdAt }
  - `pairingCodes` { code, protectedId, expiresAt, usedAt }
  - `alerts` { id, protectedId, verdict, category, summary, confidence, excerpt, source (WHATSAPP|SMS), receivedAt }
- Endpoints/functions: `registerDevice`, `createPairingCode`, `redeemPairingCode`, `unlink`, `analyzeMessage` (recebe texto anonimizado → classifica → grava alerta → dispara FCM/e-mail se SCAM), `heartbeat`, job agendado `checkInactivity`.
- Push types: `SCAM_ALERT`, `PROTECTION_INACTIVE`. Canal Android "Alertas de golpe" (importância alta).
- E-mail (Resend), remetente `alerta@sina.ia.br`, assunto "Alerta Sina: possível golpe contra {nome}".

### 3.8 Arquitetura Android
Kotlin, Jetpack Compose (Material 3), Navigation Compose, MVVM, Hilt, Retrofit/Ktor + kotlinx.serialization, DataStore (papel, deviceId, consentimento), Room (cache de alertas), WorkManager (heartbeat/fila), Firebase Messaging, CameraX + ML Kit Barcode (QR). minSdk 26, targetSdk atual.

## 4. Design

### 4.1 Padrão visual (igual ao onboarding)
Cada tela: **cabeçalho escuro** (cinza escuro) com título grande em branco, e uma **folha clara com cantos superiores arredondados (40 dp)** onde fica o conteúdo. Botão principal **pílula escura** (56 dp, raio 28). Acentos **laranja** em ícones/detalhes. Cartões brancos com raio 20 e borda sutil.

### 4.2 Tokens
| Token | Valor |
|---|---|
| Cinza escuro (fundo/cabeçalho/botão) | `#2A2C30` |
| Cinza mais escuro (corpo do cadeado, moldura) | `#1A1B1E` / `#111318` |
| Superfície clara (folha) | `#FAF9FF` |
| Cartão | `#FFFFFF`, borda `#E2E2EA` |
| Cartão selecionado | `#EEEDF4` |
| Laranja (acento) | `#FF7A1A` |
| Laranja claro (fundo de acento) | `#FFE9DA` |
| Texto principal | `#1A1B21` |
| Texto secundário | `#44474F` |
| Subtítulo sobre escuro | `#D4D4D8` |
| Neutro secundário | `#757780` |
| Golpe (vermelho) | fundo `#FFDAD6`, texto `#410002`, ícone `#93000A`, alerta forte `#BA1A1A` |
| Suspeita (âmbar) | fundo `#FFDDB4`, texto `#291800`, ícone `#5C3F00` |
| Ativo/seguro (verde) | fundo `#C9F0D4`, texto `#00210E` |

Fonte: **Roboto**. Título de tela 30 a 36 sp / peso 500; corpo 15 a 18 sp. Raios: botão 28, cartão 20, folha 40. Botões e itens de lista com altura mínima 48 a 72 dp.

### 4.3 Logo / ícone do app
Squircle cinza escuro `#2A2C30`. Dentro: **S branco fino** de traço uniforme (a curva de cima do S funciona como argola do cadeado), com um **corpo de cadeado arredondado** cinza mais escuro `#1A1B1E` atrás da metade de baixo, e uma **fechadura laranja pequena** (círculo + haste) no centro do corpo. SVG (viewBox 64):

```svg
<svg viewBox="0 0 64 64" fill="none">
  <rect x="7" y="27" width="50" height="35" rx="11" fill="#1A1B1E"/>
  <path d="M45.5 20C45.5 13 38.9 9.5 32 9.5C25.1 9.5 18.5 13.5 18.5 20C18.5 27.5 25.1 30 32 32.5C38.9 35 45.5 37.5 45.5 45C45.5 51.5 38.9 55.5 32 55.5C25.1 55.5 18.5 52 18.5 45"
        stroke="#FFFFFF" stroke-width="4.2" stroke-linecap="round" stroke-linejoin="round"/>
  <circle cx="32" cy="44" r="2" fill="#FF7A1A"/>
  <path d="M32 44.5v3.2" stroke="#FF7A1A" stroke-width="2" stroke-linecap="round"/>
</svg>
```
Em tamanhos pequenos (menos de 40 px), subir o traço do S para ~4.8 a 5.4. Usar como ícone adaptativo (fundo `#2A2C30`) e como ícone pequeno de notificação (monocromático, branco).

## 5. Fluxo e telas (24)

Fluxo: Abertura → Onboarding 1–3 → Papel → Consentimento → (se protegido) Acesso às notificações → Início do idoso; (se familiar) Cadastro → Parear → Painel.

### Abertura e onboarding
1. **Abertura (Splash):** fundo `#2A2C30`, logo grande + "Sina". Toque ou 1,5 s → Onboarding 1.
2. **Onboarding 1 – golpes:** ilustração (mensagem de golpe sinalizada). Título "Golpe não passa batido". Texto: "O Sina confere SMS e WhatsApp e avisa quando uma mensagem parece golpe." Botões: Continuar, Pular.
3. **Onboarding 2 – família:** ilustração (dois celulares + sino). "Sua família sabe na hora". "Um familiar recebe o aviso assim que um golpe chega, para ajudar sem demora." Continuar, Pular.
4. **Onboarding 3 – privacidade:** ilustração (marcadores [NOME] [VALOR] [CÓDIGO]). "Seus dados ficam com você". "Nomes, valores e códigos viram marcadores antes da análise. Nenhuma mensagem é guardada." Botão: Começar. Cada onboarding: hero escuro, 3 pontos de progresso (laranja), folha clara com "Bem-vindo ao Sina / Sem cadastro e sem senha.".

### Primeiro acesso
5. **Boas-vindas e escolha de papel:** logo 88 dp, "Bem-vindo ao Sina", "O Sina avisa quando uma mensagem de SMS ou WhatsApp parece golpe. Quem vai usar este celular?". Dois cartões de rádio: "Quero ser protegido — O Sina confere as mensagens que chegam neste celular" e "Quero proteger um familiar — Você recebe um aviso quando ele receber golpe". Botão "Continuar" (→ Consentimento se protegido; → Cadastro do familiar se familiar). Rodapé: "Sem cadastro e sem senha. Privacidade".
6. **Consentimento (idoso):** título "Privacidade". "Para proteger você, o Sina lê o texto das notificações de SMS e WhatsApp deste celular." Quatro itens com ícone: "Dados pessoais ficam no celular" (nomes, números, valores e códigos trocados por marcadores antes do envio); "Análise por inteligência artificial" (texto anonimizado analisado pela OpenAI, que não o guarda); "Seu familiar é avisado" (em caso de golpe recebe tipo, resumo e trecho sem dados pessoais, por notificação (Firebase, do Google) e e-mail (Resend)); "Nada de texto guardado" (guardamos só o resultado da análise). Link "Ler a política de privacidade". Checkbox "Li e concordo com o uso descrito acima". Botões: "Agora não" (volta) e "Ativar proteção" (habilitado só com o checkbox → tela 7).
7. **Acesso às notificações (idoso):** abre a configuração do sistema de acesso a notificações (modelo da tela do Android com o item "Sina"). Diálogo "Permitir acesso a notificações para Sina?" com texto padrão do Android; botões "Não permitir" (volta) e "Permitir" (→ Início). Mostra logo "Sina" e switch "Permitir acesso a notificações".
8. **Ajustes e vínculos (idoso):** seção "Familiares que recebem avisos" (avatar, nome, "Ligado desde 09/11/2026", botão Desligar; "Ligar outro familiar"); seção "Proteção": "Acesso às notificações — Necessário para o Sina funcionar", "Ignorar otimização de bateria — Evita que o Android feche o Sina", "Política de privacidade", "Versão 0.1.0".

### App do idoso (PROTECTED)
9. **Início:** cabeçalho com logo + "Sina" e ícone de ajustes. Selo verde "Proteção ativa". Título "Conferindo SMS e WhatsApp". Subtítulo "João recebe os avisos de golpe." Folha: "AVISOS RECENTES" com lista (ícone vermelho de alerta "Golpe: falso parente · WhatsApp · 14:20"; ícone âmbar "Cuidado: link suspeito · SMS · Ontem"; "Golpe: falsa central do banco · WhatsApp · 09/11"). Texto "Os avisos ficam só neste celular." Botão fixo "Ligar familiar" (→ tela 10).
10. **Código de pareamento:** "Ligar familiar". "No celular do seu familiar, abra o Sina, toque em Proteger um familiar e aponte a câmera para o código." QR code grande, "Ou digite o código" **483 920**, "Expira em 9:42 · uso único", botão "Gerar novo código".
11. **Alerta de golpe (SCAM):** cabeçalho vermelho-escuro (`#93000A`) no padrão da tela. "Isso é golpe." "Não faça o Pix. Ligue para seu familiar no número de sempre e confirme." Cartão: "Mensagem do WhatsApp · 14:20 — Diz ser um familiar com número novo e pede Pix com urgência." Selo "João foi avisado". Botões: "Entendi, não vou pagar" (principal) e "Abrir contatos".
12. **Alerta de suspeita (SUSPICIOUS):** cabeçalho âmbar. "Cuidado, pode ser golpe." "Não abra o link nem pague nada. Se espera uma entrega, fale com a loja pelo contato oficial." Cartão: "SMS · 9:05 — Aviso de entrega com link de um site desconhecido." Aviso: "Avisos de suspeita não são enviados ao seu familiar." Botões: "Entendi" e "Pedir ajuda a um familiar" (→ Código).

### App do familiar (GUARDIAN)
13. **Cadastro do familiar:** "Seus dados". "Você recebe uma notificação neste celular quando a pessoa protegida receber um golpe. O e-mail é um aviso extra." Campos: "Seu nome" (ajuda: "Aparece no celular de quem você proteger"); "E-mail (opcional)" ("Só para alertas de golpe. Nada de propaganda."). Nota: "Ao continuar, o Android vai perguntar se o Sina pode enviar notificações. Toque em Permitir." Botão "Continuar".
14. **Parear com a pessoa protegida:** viewfinder de câmera com cantos laranja ("Escanear código"), "No celular da pessoa protegida, toque em Ligar familiar". "Ou digite o código de 6 números" + campo Código. Erro: "Código expirado. Gere um novo no celular da pessoa protegida." Botão "Ligar".
15. **Painel do familiar:** cabeçalho logo + "Sina". "Pessoas protegidas", resumo "1 alerta hoje · 1 proteção inativa". Cartões: **Maria** — "Último sinal há 4 min", selo Ativa, "Golpe hoje, 14:20 · falso parente"; **Antônio** — "Último sinal às 12:10", selo Inativa (âmbar), "O Sina parou de funcionar no celular dele". FAB "Proteger alguém" (→ Parear). Barra inferior: Protegidos (ativo), Alertas, Ajustes.
16. **Histórico de alertas:** filtros (chips): Maria, Antônio, "Só golpes". Lista agrupada por data: HOJE "Golpe · Falso parente — Mensagem de número novo fingindo ser familiar e pedindo Pix. · Maria · WhatsApp · 14:20"; ONTEM "Suspeita · Link suspeito — Aviso de entrega com link de site desconhecido. · Maria · SMS · 9:05"; SEGUNDA-FEIRA, 9 DE NOV. "Golpe · Falsa central — Falsa central de banco pedindo o código recebido por SMS. · Maria · WhatsApp · 18:42". Toque abre o detalhe. (Obs.: pela regra 3.2, suspeitas não chegam ao familiar; o filtro "Só golpes" fica ativo por padrão. Se o item de suspeita aparecer, é só para histórico; decidir se o histórico do familiar mostra suspeitas.)
17. **Detalhe do alerta:** "Alerta de golpe". "Golpe · 98% de certeza", "Falso parente", resumo. Dados: Pessoa Maria; Recebida por WhatsApp; Quando "Hoje, 14:20". "TRECHO DA MENSAGEM": "Oi mãe, mudei de número. Preciso que faça um Pix de [VALOR] urgente pra essa chave, depois te explico." Nota: "Nomes, valores e números foram removidos no celular dela." "O QUE FAZER": "Ligue para Maria e confirme que ela não fez o Pix." "Se ela pagou, peça ao banco a devolução pelo MED do Pix."
18. **Proteção inativa:** "Antônio — Proteção desligada". "O celular dele não manda sinal desde hoje, 12:10. Enquanto isso, golpes não são detectados." "CAUSAS MAIS COMUNS": acesso às notificações desligado; economia de bateria fechou o Sina; celular desligado ou sem internet. "Peça para ele abrir o Sina. Este aviso some sozinho quando o sinal voltar."

### Como os avisos chegam (notificações)
19. **Idoso: golpe na tela de bloqueio:** notificação Sina (ícone do app) "Isso é golpe — Não faça o Pix. Ligue para seu familiar no número de sempre e confirme." Ações: "Ver aviso" (abre tela 11), "Entendi".
20. **Idoso: suspeita com o celular em uso:** heads-up sobre o app de SMS: "Cuidado, pode ser golpe — O SMS que chegou tem um link de site desconhecido. Não abra e não pague nada." Ações: "Ver aviso" (tela 12), "Entendi".
21. **Familiar: push de golpe (`SCAM_ALERT`):** "Maria recebeu um golpe — Falso parente no WhatsApp pedindo Pix. Ela já foi avisada. Ligue para confirmar que ela não pagou." Canal "Alertas de golpe". Ações: "Ver detalhes" (tela 17), "Ligar para Maria".
22. **Familiar: push de inatividade (`PROTECTION_INACTIVE`):** "Proteção do Antônio parou — O celular dele não manda sinal desde 12:10. Peça para ele abrir o Sina." Ação: "Ver o que fazer" (tela 18).
23. **Familiar: e-mail de alerta (Resend):** de `alerta@sina.ia.br`, assunto "Alerta Sina: possível golpe contra Maria". Corpo: cabeçalho escuro com logo; "Olá, João." "A Maria recebeu agora uma mensagem que o Sina identificou como golpe." Bloco com borda vermelha: "Falso parente · WhatsApp · 14:20 — Mensagem de número novo fingindo ser familiar e pedindo Pix." "Ela já foi avisada no celular. Ligue para ela e confirme que não fez nenhum pagamento." Botão "Abrir no app". Rodapé: "Você recebe este e-mail porque cadastrou seu endereço como familiar no Sina. Para parar, remova o e-mail em Ajustes."

(A tela 24 do canvas é a própria tela de Logo; tratar como referência de ícone.)

## 6. Plano de implementação sugerido

1. **Base:** projeto Compose, tema Material 3 com os tokens da seção 4, fonte Roboto, componentes: `HeroSheetScaffold` (cabeçalho escuro + folha clara), `PrimaryPillButton`, `StatusChip`, `AlertCard`, `Dot-Progress`, `SinaLogo`.
2. **Navegação e papel:** Splash → Onboarding (pager) → Escolha de papel (DataStore) → fluxos por papel.
3. **Idoso:** Consentimento, acesso a notificações (intent `ACTION_NOTIFICATION_LISTENER_SETTINGS`), `NotificationListenerService`, anonimizador local (regex + testes unitários), cliente do backend, telas de alerta, Início, Código de pareamento (QR com ZXing), Ajustes.
4. **Familiar:** Cadastro, scanner (CameraX + ML Kit), Painel, Histórico, Detalhe, Inativo, FCM com canais.
5. **Backend:** funções listadas em 3.7, `checkInactivity` agendado, envio de e-mail.
6. **Qualidade:** testes do anonimizador e da classificação com mensagens reais de golpe, tela de erro/sem internet, acessibilidade (contraste, tamanhos de fonte do sistema, TalkBack), política de privacidade e declaração do Google Play.

## 7. Decisões em aberto
- Provedor/modelo de IA (o texto de consentimento cita OpenAI).
- Limite de inatividade (sugestão: 60 min) e frequência do heartbeat.
- O histórico do familiar mostra suspeitas ou só golpes?
- Idiomas além de pt-BR.
- Backend: Firebase ou Supabase.
