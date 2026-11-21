# Avaliação da detecção de golpes

Execução de 21/11/2026 00:04 contra `https://sinaapimobile.sina.ia.br`, com 100 mensagens fictícias de `eval/mensagens.json` (50 golpes, 40 legítimas e 10 ambíguas), anonimizadas como no app.

## Resumo

| Métrica | Resultado |
|---|---|
| Golpes detectados (golpe ou suspeita) | **50/50 (100.0%)** |
| Golpes com aviso à família (golpe com certeza ≥ 0,8) | 47/50 (94.0%) |
| Mensagens legítimas marcadas por engano | **1/40 (2.5%)** |
| Falsos alarmes para a família | 0/40 (0.0%) |
| Tipo de golpe correto (quando classificado como golpe) | 44/45 (97.8%) |
| Acerto nas 3 classes | 94/100 (94.0%) |
| Latência da IA (mediana / p95) | 2372 ms / 3670 ms |
| Respostas por motor | LLM: 100 |
| Erros de chamada | 0 |

## Matriz de confusão

| Esperado \ Obtido | SCAM | SUSPICIOUS | SAFE |
|---|---|---|---|
| SCAM | 47 | 3 | 0 |
| SUSPICIOUS | 0 | 8 | 2 |
| SAFE | 0 | 1 | 39 |

## Por tipo de golpe

| Tipo | Mensagens | Detectados | Família avisada |
|---|---|---|---|
| CLONED_ACCOUNT | 3 | 3 | 3 |
| FAKE_INVESTMENT | 4 | 4 | 4 |
| FALSE_BANK | 7 | 7 | 6 |
| FALSE_BENEFIT | 5 | 5 | 5 |
| FALSE_DELIVERY | 6 | 6 | 6 |
| FALSE_PRIZE_OR_DEBT | 5 | 5 | 5 |
| FALSE_RELATIVE | 8 | 8 | 7 |
| PHISHING_LINK | 5 | 5 | 4 |
| PROMPT_INJECTION | 2 | 2 | 2 |
| VERIFICATION_CODE | 5 | 5 | 5 |

Ambíguas: {'SUSPICIOUS': 8, 'SAFE': 2} (família avisada em 0).

## Erros para revisar

- `m066` esperado SAFE, obtido SUSPICIOUS (0.62): Banco do Brasil: transferência de [VALOR] realizada com sucesso para JOAO PEREIRA.

## Limites

- Conjunto pequeno e sintético, escrito pela equipe: serve para comparar versões do prompt, não como taxa de acerto no mundo real.
- Cada mensagem é analisada sozinha, sem o contexto da conversa que o app envia quando há histórico.
- A IA não é determinística: rodadas diferentes podem variar em uma ou duas mensagens.
