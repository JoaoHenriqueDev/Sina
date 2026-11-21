#!/usr/bin/env python3
"""Mede a detecção de golpes do Sina contra um conjunto de mensagens fictícias.

Cada mensagem é anonimizada como o app faz (porte de Anonymizer.kt), enviada à API por um
dispositivo PROTECTED criado só para a avaliação e excluído no final.

    python3 eval/avaliar.py --api http://localhost:8081
    python3 eval/avaliar.py --dry-run          # só mostra o que seria enviado
"""
import argparse
import json
import re
import statistics
import sys
import time
import unicodedata
import urllib.error
import urllib.request
from collections import Counter, defaultdict
from datetime import datetime, timezone
from pathlib import Path

AQUI = Path(__file__).resolve().parent

# ---- Porte de app/.../protection/Anonymizer.kt (manter em sincronia) ----
MAX_LENGTH = 1000
SHORTENERS = {"bit.ly", "tinyurl.com", "cutt.ly", "is.gd", "t.co", "goo.gl", "rb.gy", "shorturl.at",
              "tiny.cc", "ow.ly", "s.id", "encurtador.com.br", "abre.ai", "l1nk.dev", "v.gd", "t.ly"}
TLDS = ("com|br|net|org|info|biz|top|xyz|site|online|link|click|shop|store|app|io|me|ly|co|cc|gd|at|id|ai|dev|"
        "live|vip|club|icu|cfd|sbs|lat|pro|gov|edu|us|tk|ml|ga|cf|gq|pw|ws|in|ru|cn|to|gl")
EMAIL = re.compile(r"[\w.+-]+@[\w-]+(?:\.[\w-]+)+")
URL = re.compile(r"(?i)\b(?:https?://)?(?:www\.)?((?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+(?:" + TLDS
                 + r"))\b(?![.\w-]*@)(?:[/?#][^\s]*)?")
CPF = re.compile(r"(?<!\d)\d{3}\.\d{3}\.\d{3}-\d{2}(?!\d)")
MONEY = re.compile(r"(?i)R\$\s?\d+(?:\.\d{3})*(?:,\d{1,2})?|(?<![\d.,])\d{1,3}(?:\.\d{3})*,\d{2}(?![\d,])"
                   r"|(?<!\d)\d+(?:[.,]\d+)?\s?(?:reais|mil reais)\b")
PHONE = re.compile(r"(?<![\d\w])(?:\+?55[\s-]?)?(?:\(?\d{2}\)?[\s-]?)?9?\s?\d{4}[\s-]?\d{4}(?!\d)")
CODE = re.compile(r"(?<![\d/:\[])\d{4,}(?![\d/])")
SPACES = re.compile(r"[ \t]+")
URGENCY = re.compile(r"\b(urgente|urgencia|imediat\w*|agora|hoje|ate amanha|24 ?h|bloquead\w*|suspens\w*|cancelad\w*"
                     r"|ultimo aviso|ultimo dia|prazo|expira\w*|rapido)\b")
PIX = re.compile(r"\b(pix|transfer\w*|deposit\w*|boleto|pagar|pague|pagamento|taxa|emprest\w*|chave)\b")
CODE_REQUEST = re.compile(r"\b(senha|token)\b|\bcodigo\b.{0,40}\b(envi\w*|mand\w*|pass\w*|inform\w*|digit\w*|fal\w*"
                          r"|confirm\w*|repass\w*)\b|\b(envi\w*|mand\w*|pass\w*|inform\w*|repass\w*)\b.{0,40}\bcodigo\b")


def sender_signal(title, is_group=False):
    t = (title or "").strip()
    if not t or is_group:
        return None
    digits = sum(c.isdigit() for c in t)
    only_number = digits >= 4 and all(c.isdigit() or c in " +-()." for c in t)
    return "UNKNOWN_SENDER" if only_number or t.startswith("~") else "SAVED_CONTACT"


def _normalize(text):
    return "".join(c for c in unicodedata.normalize("NFD", text.lower()) if not unicodedata.combining(c))


def _replace_name(text, sender):
    if not sender or not sender.strip() or sum(c.isdigit() for c in sender) >= 4:
        return text
    parts = sorted({p.strip() for p in [sender.strip()] + sender.split() if len(p.strip()) >= 3},
                   key=len, reverse=True)
    for part in parts:
        text = re.sub(r"(?i)(?<!\[)(?<![^\W\d_])" + re.escape(part) + r"(?![^\W\d_])(?!\])", "[NOME]", text)
    return text


def anonymize(text, sender=None):
    signals = []
    out = EMAIL.sub("[EMAIL]", text)

    def link(m):
        host = m.group(1).lower().removeprefix("www.")
        if host in SHORTENERS and "SHORTENED_LINK" not in signals:
            signals.append("SHORTENED_LINK")
        return f"[LINK:{host}]"

    out = URL.sub(link, out)
    out = CPF.sub("[CPF]", out)
    out = MONEY.sub("[VALOR]", out)
    out = PHONE.sub(lambda m: "[TELEFONE]" if sum(c.isdigit() for c in m.group()) >= 8 else m.group(), out)
    out = CODE.sub("[CODIGO]", out)
    out = _replace_name(out, sender)
    out = SPACES.sub(" ", out).strip()[:MAX_LENGTH]
    n = _normalize(text)
    for name, rx in (("URGENCY", URGENCY), ("PIX_REQUEST", PIX), ("CODE_REQUEST", CODE_REQUEST)):
        if rx.search(n):
            signals.append(name)
    return out, signals


def _autoteste():
    texto, sinais = anonymize("Oi mãe, faz um Pix de R$ 1.850,00 hoje pra 11987654321 em bit.ly/x", "Ana Souza")
    assert texto == "Oi mãe, faz um Pix de [VALOR] hoje pra [TELEFONE] em [LINK:bit.ly]", texto
    assert sinais == ["SHORTENED_LINK", "URGENCY", "PIX_REQUEST"], sinais
    assert anonymize("Ana, seu código 4829 chegou", "Ana Souza")[0] == "[NOME], seu código [CODIGO] chegou"
    assert sender_signal("+55 11 98765-4321") == "UNKNOWN_SENDER" and sender_signal("Filha Ana") == "SAVED_CONTACT"


# ---- Chamada à API ----
def http(api, method, path, token=None, body=None, timeout=60):
    req = urllib.request.Request(api + path, method=method,
                                 data=None if body is None else json.dumps(body).encode(),
                                 headers={"Content-Type": "application/json", "User-Agent": "sina-eval/1"})
    if token:
        req.add_header("Authorization", "Bearer " + token)
    with urllib.request.urlopen(req, timeout=timeout) as r:
        raw = r.read()
        return r.status, (json.loads(raw) if raw else None)


def analisar(api, token, payload):
    for tentativa in range(4):
        t0 = time.perf_counter()
        try:
            _, resp = http(api, "POST", "/v1/analyze", token, payload)
            return resp, (time.perf_counter() - t0) * 1000, None
        except urllib.error.HTTPError as e:
            if e.code == 429 and tentativa < 3:
                print("  limite de requisições, aguardando 65 s…", file=sys.stderr)
                time.sleep(65)
                continue
            return None, None, f"HTTP {e.code}: {e.read()[:200].decode(errors='replace')}"
        except (urllib.error.URLError, TimeoutError) as e:
            return None, None, str(e)
    return None, None, "limite de requisições"


# ---- Métricas ----
def pct(a, b):
    return f"{100 * a / b:.1f}%" if b else "—"


def metricas(res):
    ok = [r for r in res if r["veredito"]]
    golpes = [r for r in ok if r["esperado"] == "SCAM"]
    legit = [r for r in ok if r["esperado"] == "SAFE"]
    ambig = [r for r in ok if r["esperado"] == "SUSPICIOUS"]
    avisa = lambda r: r["veredito"] == "SCAM" and r["confianca"] >= 0.8
    lat = sorted(r["latencia_ms"] for r in ok if r["motor"] == "LLM")
    q = lambda p: lat[min(len(lat) - 1, int(round(p * (len(lat) - 1))))] if lat else None
    golpes_scam = [r for r in golpes if r["veredito"] == "SCAM" and r["categoria"] not in ("PROMPT_INJECTION",)]
    return {
        "total": len(res), "respondidas": len(ok), "erros": len(res) - len(ok),
        "motores": dict(Counter(r["motor"] for r in ok)),
        "golpes": {"n": len(golpes),
                   "detectados": sum(r["veredito"] != "SAFE" for r in golpes),
                   "como_golpe": sum(r["veredito"] == "SCAM" for r in golpes),
                   "familia_avisada": sum(avisa(r) for r in golpes),
                   "perdidos": [r["id"] for r in golpes if r["veredito"] == "SAFE"]},
        "legitimas": {"n": len(legit),
                      "corretas": sum(r["veredito"] == "SAFE" for r in legit),
                      "falso_positivo": sum(r["veredito"] != "SAFE" for r in legit),
                      "falso_alarme_familia": sum(avisa(r) for r in legit),
                      "ids_falso_positivo": [r["id"] for r in legit if r["veredito"] != "SAFE"]},
        "ambiguas": {"n": len(ambig), "distribuicao": dict(Counter(r["veredito"] for r in ambig)),
                     "familia_avisada": sum(avisa(r) for r in ambig)},
        "categoria_correta": {"n": len(golpes_scam),
                              "acertos": sum(r["tipo"] == r["categoria"] for r in golpes_scam)},
        "acuracia_3_classes": sum(r["veredito"] == r["esperado"] for r in ok),
        "matriz": {e: dict(Counter(r["veredito"] for r in ok if r["esperado"] == e))
                   for e in ("SCAM", "SUSPICIOUS", "SAFE")},
        "por_categoria": {c: {"n": len(v), "detectados": sum(r["veredito"] != "SAFE" for r in v),
                              "familia_avisada": sum(avisa(r) for r in v)}
                          for c, v in sorted(_agrupa(golpes).items())},
        "latencia_llm_ms": {"n": len(lat), "p50": q(0.5), "p95": q(0.95), "max": lat[-1] if lat else None,
                            "media": round(statistics.mean(lat)) if lat else None},
    }


def _agrupa(rs):
    d = defaultdict(list)
    for r in rs:
        d[r["categoria"]].append(r)
    return d


def relatorio(m, res, api, quando):
    g, l, a, c = m["golpes"], m["legitimas"], m["ambiguas"], m["categoria_correta"]
    lat = m["latencia_llm_ms"]
    linhas = [
        "# Avaliação da detecção de golpes",
        "",
        f"Execução de {quando} contra `{api}`, com {m['total']} mensagens fictícias de `eval/mensagens.json` "
        f"({g['n']} golpes, {l['n']} legítimas e {a['n']} ambíguas), anonimizadas como no app.",
        "",
        "## Resumo",
        "",
        "| Métrica | Resultado |",
        "|---|---|",
        f"| Golpes detectados (golpe ou suspeita) | **{g['detectados']}/{g['n']} ({pct(g['detectados'], g['n'])})** |",
        f"| Golpes com aviso à família (golpe com certeza ≥ 0,8) | {g['familia_avisada']}/{g['n']} ({pct(g['familia_avisada'], g['n'])}) |",
        f"| Mensagens legítimas marcadas por engano | **{l['falso_positivo']}/{l['n']} ({pct(l['falso_positivo'], l['n'])})** |",
        f"| Falsos alarmes para a família | {l['falso_alarme_familia']}/{l['n']} ({pct(l['falso_alarme_familia'], l['n'])}) |",
        f"| Tipo de golpe correto (quando classificado como golpe) | {c['acertos']}/{c['n']} ({pct(c['acertos'], c['n'])}) |",
        f"| Acerto nas 3 classes | {m['acuracia_3_classes']}/{m['respondidas']} ({pct(m['acuracia_3_classes'], m['respondidas'])}) |",
        f"| Latência da IA (mediana / p95) | {lat['p50']} ms / {lat['p95']} ms |",
        f"| Respostas por motor | {', '.join(f'{k}: {v}' for k, v in m['motores'].items())} |",
        f"| Erros de chamada | {m['erros']} |",
        "",
        "## Matriz de confusão",
        "",
        "| Esperado \\ Obtido | SCAM | SUSPICIOUS | SAFE |",
        "|---|---|---|---|",
    ]
    for e in ("SCAM", "SUSPICIOUS", "SAFE"):
        row = m["matriz"][e]
        linhas.append(f"| {e} | {row.get('SCAM', 0)} | {row.get('SUSPICIOUS', 0)} | {row.get('SAFE', 0)} |")
    linhas += ["", "## Por tipo de golpe", "", "| Tipo | Mensagens | Detectados | Família avisada |", "|---|---|---|---|"]
    for cat, v in m["por_categoria"].items():
        linhas.append(f"| {cat} | {v['n']} | {v['detectados']} | {v['familia_avisada']} |")
    linhas += ["", f"Ambíguas: {a['distribuicao']} (família avisada em {a['familia_avisada']}).", "",
               "## Erros para revisar", ""]
    erros = [r for r in res if r["veredito"] and (
        (r["esperado"] == "SCAM" and r["veredito"] == "SAFE") or (r["esperado"] == "SAFE" and r["veredito"] != "SAFE"))]
    if not erros:
        linhas.append("Nenhum golpe perdido e nenhuma mensagem legítima marcada.")
    for r in erros:
        linhas.append(f"- `{r['id']}` esperado {r['esperado']}, obtido {r['veredito']} ({r['confianca']:.2f}): "
                      f"{r['enviado']['text']}")
    falhas = [r for r in res if not r["veredito"]]
    for r in falhas:
        linhas.append(f"- `{r['id']}` sem resposta: {r['erro']}")
    linhas += ["", "## Limites", "",
               "- Conjunto pequeno e sintético, escrito pela equipe: serve para comparar versões do prompt, "
               "não como taxa de acerto no mundo real.",
               "- Cada mensagem é analisada sozinha, sem o contexto da conversa que o app envia quando há histórico.",
               "- A IA não é determinística: rodadas diferentes podem variar em uma ou duas mensagens.", ""]
    return "\n".join(linhas)


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--api", default="http://localhost:8081")
    ap.add_argument("--dados", default=str(AQUI / "mensagens.json"))
    ap.add_argument("--intervalo", type=float, default=2.2, help="segundos entre análises (limite de 30/min)")
    ap.add_argument("--limite", type=int, help="analisa só as N primeiras mensagens")
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()
    _autoteste()

    msgs = json.loads(Path(args.dados).read_text())[: args.limite]
    api = args.api.rstrip("/")
    quando = datetime.now(timezone.utc).astimezone().strftime("%d/%m/%Y %H:%M")
    payloads = []
    for m in msgs:
        texto, sinais = anonymize(m["texto"], m["remetente"])
        s = sender_signal(m["remetente"])
        payloads.append({"source": m["fonte"], "text": texto, "localSignals": sinais + ([s] if s else []),
                         "receivedAt": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"), "messageCount": 1})
    if args.dry_run:
        for m, p in zip(msgs, payloads):
            print(f"{m['id']} [{m['esperado']}] {p['source']} {p['localSignals']}\n    {p['text']}")
        return

    _, reg = http(api, "POST", "/v1/devices", body={"role": "PROTECTED", "displayName": "Avaliação Sina",
                                                     "email": "avaliacao@example.com", "phone": "11900000000"})
    token = reg["deviceToken"]
    res = []
    try:
        for i, (m, p) in enumerate(zip(msgs, payloads), 1):
            resp, ms, erro = analisar(api, token, p)
            r = {"id": m["id"], "esperado": m["esperado"], "categoria": m["categoria"], "enviado": p,
                 "veredito": resp and resp["verdict"], "tipo": resp and resp.get("scamType"),
                 "confianca": resp["confidence"] if resp else None, "motor": resp and resp["engine"],
                 "mensagem_idoso": resp and resp.get("elderlyMessage"),
                 "latencia_ms": round(ms) if ms else None, "erro": erro}
            res.append(r)
            marca = "ok" if r["veredito"] == m["esperado"] else "≠ "
            conf = "" if r["confianca"] is None else f"{r['confianca']:.2f}"
            print(f"[{i:3}/{len(msgs)}] {marca} {m['id']} esperado {m['esperado']:<10} obtido "
                  f"{r['veredito'] or erro} {conf} {r['motor'] or ''}")
            time.sleep(args.intervalo)
    finally:
        try:
            http(api, "DELETE", "/v1/devices/me", token)
        except Exception as e:  # noqa: BLE001
            print("Aviso: não foi possível excluir o dispositivo de avaliação:", e, file=sys.stderr)

    m = metricas(res)
    pasta = AQUI / "resultados"
    pasta.mkdir(exist_ok=True)
    carimbo = datetime.now().strftime("%Y%m%d-%H%M%S")
    (pasta / f"{carimbo}.json").write_text(json.dumps({"api": api, "quando": quando, "metricas": m, "resultados": res},
                                                      ensure_ascii=False, indent=1))
    md = relatorio(m, res, api, quando)
    (AQUI / "RESULTADOS.md").write_text(md)
    print("\n" + md)


if __name__ == "__main__":
    main()
