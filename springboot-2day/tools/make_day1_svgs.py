#!/usr/bin/env python3
"""Generates the SVG diagrams for lessons 02-06 (day1/0N-*/images/*.svg). Run render_svgs.py afterwards for the PNGs."""
import os, html
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "day1")

C = dict(blue=("#e8f1fb", "#2f6fb5"), green=("#eaf6ec", "#2e8b57"), red=("#fdf2f2", "#c0392b"),
         yellow=("#fff8e1", "#c79100"), grey=("#f3f4f6", "#6b7280"), purple=("#f3edfb", "#7e57c2"), white=("#ffffff", "#333333"))


class Svg:
    def __init__(self, w, h, title):
        self.w, self.h, self.parts = w, h, []
        self.parts.append(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {w} {h}" font-family="Arial, Helvetica, sans-serif">')
        self.parts.append('<defs><marker id="a" markerUnits="userSpaceOnUse" markerWidth="10" markerHeight="10" refX="8" refY="5" orient="auto"><path d="M0,0 L10,5 L0,10 z" fill="#333"/></marker></defs>')
        self.parts.append(f'<rect width="{w}" height="{h}" fill="#ffffff"/>')
        self.text(w / 2, 30, title, 20, bold=True, anchor="middle")

    def text(self, x, y, s, size=14, bold=False, anchor="start", fill="#1a1a1a", mono=False):
        fam = ' font-family="Consolas, Menlo, monospace"' if mono else ""
        wt = ' font-weight="bold"' if bold else ""
        self.parts.append(f'<text x="{x}" y="{y}" font-size="{size}" text-anchor="{anchor}" fill="{fill}"{wt}{fam}>{html.escape(s)}</text>')

    def box(self, x, y, w, h, lines, color="blue", size=14, title=None, mono=False):
        fill, stroke = C[color]
        self.parts.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="8" fill="{fill}" stroke="{stroke}" stroke-width="2"/>')
        rows = ([(title, True)] if title else []) + [(l, False) for l in lines]
        lh = size + 6
        y0 = y + h / 2 - (len(rows) - 1) * lh / 2 + size / 3
        for i, (s, b) in enumerate(rows):
            self.text(x + w / 2, y0 + i * lh, s, size, bold=b, anchor="middle", mono=(mono and not b))

    def arrow(self, x1, y1, x2, y2, label=None, dash=False, lx=None, ly=None):
        d = ' stroke-dasharray="6 4"' if dash else ""
        self.parts.append(f'<line x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}" stroke="#333" stroke-width="2"{d} marker-end="url(#a)"/>')
        if label:
            self.text(lx if lx is not None else (x1 + x2) / 2, ly if ly is not None else (y1 + y2) / 2 - 6, label, 12, anchor="middle", fill="#444")

    def save(self, rel):
        path = os.path.join(ROOT, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        open(path, "w", encoding="utf-8").write("\n".join(self.parts) + "\n</svg>\n")
        print("wrote", rel)


# ------------------------------------------------------------------ lesson 02
s = Svg(900, 400, "Where does a setting come from? Highest wins")
levels = [("1. Command line", "--trade.max-quantity=500", "red"), ("2. Environment variable", "TRADE_MAX_QUANTITY=500   (JWT_SECRET)", "red"),
          ("3. application-{profile}.yml", "application-prod.yml  (profile chosen at startup)", "yellow"),
          ("4. application.yml", "base file shipped inside the jar", "yellow"), ("5. Defaults in code", "@DefaultValue, field initialisers", "grey")]
for i, (a, b, c) in enumerate(levels):
    s.box(40, 55 + i * 62, 560, 52, [b], c, title=a)
s.arrow(640, 340, 640, 70, "wins")
s.text(660, 200, "a higher level", 13)
s.text(660, 218, "overrides a lower one", 13)
s.text(40, 385, "Same jar everywhere; only the outside (profile, env vars, arguments) changes. That is a 12-factor application.", 13)
s.save("02-boot-internals/images/01-config-precedence.svg")

s = Svg(900, 360, "How /actuator/health decides UP or DOWN")
s.box(30, 140, 150, 70, ["load balancer /", "Kubernetes probe"], "grey")
s.arrow(180, 175, 250, 175, "GET")
s.box(250, 130, 170, 90, ["/actuator/health"], "blue", title="Health endpoint")
for i, (n, c) in enumerate([("db  (Hikari / JDBC)", "green"), ("diskSpace", "green"), ("gateway  (our indicator)", "yellow")]):
    s.box(500, 60 + i * 80, 190, 56, [n], c)
    s.arrow(420, 175, 500, 88 + i * 80)
s.box(740, 130, 130, 90, ["worst status wins:", "any DOWN = DOWN"], "purple", title="Aggregate")
for i in range(3):
    s.arrow(690, 88 + i * 80, 740, 175)
s.text(450, 330, "UP  -> HTTP 200      DOWN -> HTTP 503  (the load balancer stops sending traffic)", 14, anchor="middle")
s.save("02-boot-internals/images/02-health-aggregation.svg")

# ------------------------------------------------------------------ lesson 03
s = Svg(940, 420, "One request through the web layer")
xs = [20, 190, 360, 530, 700]
labels = [("Client", ["POST /trades", "JSON body"], "grey"), ("Security filter", ["lesson 05"], "grey"), ("DispatcherServlet", ["finds the", "controller method"], "blue"),
          ("Jackson", ["JSON -> record", "400 if malformed"], "blue"), ("@Valid", ["Bean Validation", "400 + errors map"], "yellow")]
for x, (t, l, c) in zip(xs, labels):
    s.box(x, 60, 150, 80, l, c, title=t)
for a, b in zip(xs, xs[1:]):
    s.arrow(a + 150, 100, b, 100)
s.box(700, 200, 150, 80, ["TradeController", "-> TradeService"], "green", title="Your code")
s.arrow(775, 140, 775, 200)
s.box(360, 330, 300, 70, ["any exception from any layer", "-> ProblemDetail JSON"], "red", title="ApiExceptionHandler")
s.arrow(775, 280, 660, 360, "throws", dash=True, lx=745, ly=335)
s.arrow(360, 365, 95, 365, "application/problem+json", lx=230, ly=355)
s.arrow(95, 330, 95, 145, dash=True)
s.save("03-rest-validation/images/01-request-pipeline.svg")

s = Svg(900, 400, "Which failure becomes which HTTP status")
rows = [("400 Bad Request", "malformed JSON, failed @Valid, bad enum in ?status=", "yellow"),
        ("401 Unauthorized", "no or invalid token (lesson 05)", "grey"),
        ("403 Forbidden", "logged in but role not allowed (lesson 05)", "grey"),
        ("404 Not Found", "TradeNotFoundException", "blue"),
        ("405 Method Not Allowed", "DELETE /trades/1 (no such mapping)", "yellow"),
        ("409 Conflict", "TradeStateException: settle an already settled trade", "purple"),
        ("422 Unprocessable", "BusinessRuleException: quantity above the limit", "purple"),
        ("500 Server Error", "anything unexpected; generic message, details only in the log", "red")]
for i, (a, b, c) in enumerate(rows):
    s.box(30, 50 + i * 41, 230, 34, [a], c, size=14)
    s.text(280, 73 + i * 41, b, 14)
s.save("03-rest-validation/images/02-status-codes.svg")

# ------------------------------------------------------------------ lesson 04
s = Svg(900, 330, "Swapping in-memory for JPA: only the bottom changes")
names = [("TradeController", "green"), ("TradeService", "green"), ("TradeRepository", "yellow"), ("JpaTradeRepository", "blue"), ("TradeJpaRepository", "blue"), ("Hibernate + H2", "grey")]
sub = ["web", "business logic", "interface (port)", "adapter: entity <-> record", "Spring Data writes it", "SQL, connection pool"]
for i, ((n, c), t) in enumerate(zip(names, sub)):
    x = 20 + i * 145
    s.box(x, 100, 130, 80, [t], c, size=11, title=n)
    if i:
        s.arrow(x - 15, 140, x, 140)
s.box(20, 230, 410, 56, ["unchanged since lesson 03"], "grey", size=13)
s.box(450, 230, 430, 56, ["new in lesson 04  (InMemoryTradeRepository stays for unit tests)"], "blue", size=13)
s.save("04-data-jpa/images/01-layers-with-jpa.svg")

s = Svg(920, 420, "What @Transactional does around a call")
s.box(20, 150, 130, 80, ["controller or", "other bean"], "grey", title="Caller")
s.arrow(150, 190, 230, 190)
s.box(230, 120, 190, 140, ["1. open transaction", "2. call real method", "3. commit or roll back"], "blue", title="Spring proxy")
s.arrow(420, 190, 500, 190)
s.box(500, 150, 150, 80, ["your code"], "green", title="TradeService")
s.arrow(650, 190, 730, 190)
s.box(730, 150, 170, 80, ["one connection,", "one commit"], "yellow", title="Database")
s.box(30, 300, 410, 95, ["RuntimeException / Error: ROLLBACK", "checked Exception: COMMIT (!)", "rollbackFor = Exception.class changes that"], "red", title="Default rollback rules", size=13)
s.box(480, 300, 410, 95, ["this.other() skips the proxy: no new transaction", "private / final methods: not intercepted", "lazy field read outside a transaction: error"], "yellow", title="Where it silently does nothing", size=13)
s.save("04-data-jpa/images/02-transaction-proxy.svg")

s = Svg(900, 400, "The N+1 query problem (5 trades, 2 allocations each)")
s.box(30, 55, 400, 40, ["findAll() then getAllocations() in a loop"], "red", title="Naive: 6 SQL statements")
s.text(40, 125, "select * from trade                                   (1)", 13, mono=True)
for i in range(5):
    s.text(40, 150 + i * 22, f"select * from allocation where trade_id = {i+1}     ({i+2})", 13, mono=True)
s.box(470, 55, 400, 40, ["one join, everything loaded together"], "green", title="Fetch join: 1 SQL statement")
s.text(480, 125, "select t.*, a.*", 13, mono=True)
s.text(480, 147, "  from trade t", 13, mono=True)
s.text(480, 169, "  left join allocation a on a.trade_id = t.id", 13, mono=True)
s.text(30, 300, "With 10,000 trades the naive version sends 10,001 statements: one network round trip each.", 14)
s.text(30, 325, "It is invisible in a unit test with 3 rows and obvious in production. We count statements in NPlusOneTest.", 14)
s.text(30, 360, "Fixes: JOIN FETCH, @EntityGraph, batch fetching (@BatchSize), or a DTO projection query.", 14)
s.save("04-data-jpa/images/03-n-plus-one.svg")

# ------------------------------------------------------------------ lesson 05
s = Svg(940, 430, "Two filter chains, two jobs")
s.box(20, 170, 130, 70, ["every HTTP", "request"], "grey")
s.arrow(150, 205, 220, 205)
s.box(220, 165, 170, 80, ["which path?"], "white", title="securityMatcher")
s.arrow(390, 185, 470, 95, "/auth/**", lx=420, ly=130)
s.arrow(390, 225, 470, 335, "everything else", lx=410, ly=300)
s.box(470, 50, 220, 90, ["HTTP Basic", "username + password"], "yellow", title="Chain 1: tokenChain")
s.arrow(690, 95, 760, 95)
s.box(760, 55, 160, 80, ["TokenController", "returns a signed JWT"], "green")
s.box(470, 290, 220, 110, ["Bearer token required", "verify signature + expiry", "roles -> authorities"], "blue", title="Chain 2: apiChain")
s.arrow(690, 345, 760, 345)
s.box(760, 305, 160, 80, ["@PreAuthorize", "hasRole('OPS')"], "purple", title="Controller")
s.text(30, 425, "401 = we do not know who you are.   403 = we know, and the answer is no.", 14)
s.save("05-security-jwt/images/01-two-filter-chains.svg")

s = Svg(940, 400, "Anatomy of the JWT our /auth/token endpoint returns")
s.box(20, 60, 270, 120, ['{ "alg": "HS256" }'], "red", title="1. Header", mono=True)
s.box(335, 60, 330, 120, ['"iss": "trade-service"', '"sub": "alice"', '"roles": ["TRADER"]', '"iat" / "exp": seconds since 1970'], "blue", title="2. Payload (claims)", size=13, mono=True)
s.box(710, 60, 210, 120, ["HMAC-SHA256(", "header + payload,", "secret)"], "purple", title="3. Signature", size=13)
s.text(305, 125, ".", 28, bold=True)
s.text(680, 125, ".", 28, bold=True)
s.text(20, 215, "Sent as   Authorization: Bearer <header>.<payload>.<signature>   (each part base64url-encoded)", 14)
s.box(20, 250, 430, 120, ["recompute the HMAC with our secret", "same result?  then nobody changed it", "check exp, then map roles to authorities"], "green", title="What the API does with it", size=13)
s.box(480, 250, 440, 120, ["The payload is ENCODED, not encrypted: anyone can read it.", "Never put secrets in claims.", "Keep tokens short-lived; HTTPS always."], "yellow", title="What it does not do", size=13)
s.save("05-security-jwt/images/02-jwt-anatomy.svg")

# ------------------------------------------------------------------ lesson 06
s = Svg(900, 430, "Our test pyramid: many fast tests, few slow ones")
levels = [("HTTP", "TradeHttpTest  (3)", "real Tomcat, random port", 330, "red"),
          ("Integration", "SecurityTest, TradeApiTest, ActuatorTest, ...", "whole context, MockMvc", 450, "yellow"),
          ("Slice", "TradeControllerTest, JpaTradeRepositoryTest", "only web OR only JPA", 570, "blue"),
          ("Unit", "TradeServiceMockTest, validation, fake repository", "no Spring, milliseconds", 690, "green")]
for i, (n, t, d, w, c) in enumerate(levels):
    x = 450 - w / 2
    s.box(x, 55 + i * 85, w, 72, [t, d], c, title=n, size=12)
s.text(30, 405, "Rule of thumb: test each rule at the LOWEST level that can show it fail. Keep a few top-level journeys as a safety net.", 13)
s.save("06-testing/images/01-test-pyramid.svg")

s = Svg(940, 400, "What each test annotation starts")
cols = ["", "Spring", "web", "security", "JPA + DB", "real port"]
rows = [("(no annotation) + Mockito", "-", "-", "-", "-", "-", "green"), ("@WebMvcTest", "yes", "yes", "if imported", "-", "-", "blue"),
        ("@DataJpaTest", "yes", "-", "-", "yes", "-", "blue"), ("@SpringBootTest", "yes", "mock", "yes", "yes", "-", "yellow"),
        ("@SpringBootTest(RANDOM_PORT)", "yes", "yes", "yes", "yes", "yes", "red")]
cx = [30, 330, 440, 540, 670, 800]
for j, c in enumerate(cols):
    if c:
        s.text(cx[j] + 40, 62, c, 14, bold=True, anchor="middle")
for i, r in enumerate(rows):
    y = 80 + i * 54
    s.box(30, y, 290, 42, [r[0]], r[6], size=13, mono=True)
    for j in range(1, 6):
        s.text(cx[j] + 40, y + 27, r[j], 14, anchor="middle")
s.text(30, 375, "More boxes ticked = slower start (about 0.5 s for a slice, 1-3 s for a full context here) and more things that can break.", 13)
s.save("06-testing/images/02-what-each-annotation-starts.svg")
