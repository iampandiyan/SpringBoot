#!/usr/bin/env python3
"""Generates the SVG diagrams for Day 2 lessons (day2/NN-*/images/*.svg). Run render_svgs.py afterwards for the PNGs."""
import os, html, sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "day2")

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

    def frame(self, x, y, w, h, label, color="grey"):
        """An empty rounded rectangle with a label in its top-left corner (for nesting and grouping)."""
        fill, stroke = C[color]
        self.parts.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="10" fill="{fill}" stroke="{stroke}" stroke-width="2"/>')
        self.text(x + 12, y + 22, label, 14, bold=True)

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


def lesson07():
    # 1. how the Resilience4j annotations nest
    s = Svg(900, 400, "One call to the gateway: how the three annotations nest")
    s.frame(40, 55, 820, 300, "@Retry  (outermost: repeats a failed attempt, owns the fallback)", "yellow")
    s.frame(80, 100, 740, 230, "@CircuitBreaker  (counts attempts, refuses calls while OPEN)", "red")
    s.frame(120, 145, 660, 160, "@Bulkhead  (at most N calls in flight)", "purple")
    s.box(180, 190, 540, 80, ["gateway.send(\"T7\", trade)", "the real remote call"], "blue", title="ResilientGatewayClient.confirm")
    s.text(450, 385, "Fixed order, outermost first: Retry( CircuitBreaker( Bulkhead( call ) ) ). Put the fallback on the OUTERMOST one.", 13, anchor="middle")
    s.save("07-microservices-resilience/images/01-annotation-nesting.svg")

    # 2. circuit breaker state machine
    s = Svg(900, 420, "Circuit breaker states")
    s.box(60, 150, 220, 110, ["calls pass through", "failures are counted"], "green", title="CLOSED")
    s.box(620, 150, 220, 110, ["calls refused at once", "(CallNotPermittedException)"], "red", title="OPEN")
    s.box(340, 310, 220, 90, ["a few probe calls", "are let through"], "yellow", title="HALF_OPEN")
    s.arrow(280, 190, 620, 190, "failure rate or slow-call rate over the threshold", lx=450, ly=178)
    s.arrow(780, 260, 560, 335, "wait-duration-in-open-state is over", lx=740, ly=322)
    s.arrow(340, 350, 190, 260, "probes succeed", lx=190, ly=330)
    s.arrow(470, 310, 650, 262, "a probe fails", lx=500, ly=282, dash=True)
    s.text(450, 100, "COUNT_BASED window: the last N calls. Needs minimum-number-of-calls before it may open.", 13, anchor="middle")
    s.save("07-microservices-resilience/images/02-circuit-breaker-states.svg")

    # 3. sync vs async between services
    s = Svg(900, 400, "Two ways services talk, and what each costs")
    s.frame(30, 55, 410, 300, "Synchronous (HTTP / gRPC)", "blue")
    s.box(55, 110, 120, 60, ["Trade"], "blue")
    s.box(290, 110, 120, 60, ["Settlement"], "blue")
    s.arrow(175, 140, 290, 140, "request")
    s.arrow(290, 160, 175, 160, "reply", dash=True)
    s.text(55, 215, "+ simple, immediate answer", 13)
    s.text(55, 240, "- both must be up at the same moment", 13)
    s.text(55, 265, "- latency and failures add up along the chain", 13)
    s.text(55, 290, "=> needs timeout, retry, circuit breaker", 13, bold=True)
    s.frame(460, 55, 410, 300, "Asynchronous (events / messages)", "green")
    s.box(485, 110, 100, 60, ["Trade"], "green")
    s.box(620, 110, 100, 60, ["Broker"], "yellow")
    s.box(755, 110, 100, 60, ["Settlement"], "green")
    s.arrow(585, 140, 620, 140)
    s.arrow(720, 140, 755, 140)
    s.text(485, 215, "+ sender does not wait, survives downtime", 13)
    s.text(485, 240, "+ absorbs bursts, easy to add consumers", 13)
    s.text(485, 265, "- eventual consistency, harder to debug", 13)
    s.text(485, 290, "=> needs idempotent consumers, outbox", 13, bold=True)
    s.save("07-microservices-resilience/images/03-sync-vs-async.svg")


def lesson08():
    # 1. dual write problem versus outbox
    s = Svg(900, 440, "The dual-write problem, and the outbox that solves it")
    s.frame(20, 50, 420, 370, "Dual write (broken)", "red")
    s.box(45, 100, 160, 56, ["TradeService"], "blue")
    s.box(45, 210, 160, 56, ["Database"], "green", title="1. UPDATE trade")
    s.box(245, 210, 170, 56, ["Broker"], "yellow", title="2. send event")
    s.arrow(125, 156, 125, 210)
    s.arrow(205, 128, 330, 210)
    s.text(40, 310, "Crash between 1 and 2: the trade is settled but", 13)
    s.text(40, 330, "nobody is told. Send first, DB fails: the world", 13)
    s.text(40, 350, "heard about a settlement that never happened.", 13)
    s.text(40, 380, "Two systems, no shared transaction.", 13, bold=True)
    s.frame(460, 50, 420, 370, "Transactional outbox (safe)", "green")
    s.box(480, 100, 380, 70, ["UPDATE trade  +  INSERT outbox_event"], "green", title="ONE local transaction")
    s.box(480, 215, 160, 56, ["OutboxRelay"], "purple", title="2. later")
    s.box(700, 215, 160, 56, ["Broker / handlers"], "yellow")
    s.arrow(560, 170, 560, 215, "poll", lx=595, ly=198)
    s.arrow(640, 243, 700, 243)
    s.text(480, 310, "Both rows commit together or not at all.", 13)
    s.text(480, 330, "The relay sends later and marks the row published.", 13)
    s.text(480, 350, "Crash after send, before mark: sent twice", 13)
    s.text(480, 370, "=> at-least-once, so consumers must be idempotent.", 13, bold=True)
    s.save("08-saga-outbox-cqrs/images/01-dual-write-and-outbox.svg")

    # 2. saga with compensation
    s = Svg(900, 420, "Saga: local steps forward, compensations backward")
    xs = [40, 330, 620]
    names = [("1. reserve cash", "undo: release cash"), ("2. deliver securities", "undo: take back"), ("3. confirm gateway", "pivot: nothing to undo")]
    cols = ["green", "green", "red"]
    for x, (a, b), c in zip(xs, names, cols):
        s.box(x, 90, 240, 70, [b], c, title=a)
    s.arrow(280, 125, 330, 125)
    s.arrow(570, 125, 620, 125, "FAILS", lx=595, ly=112)
    s.arrow(740, 160, 740, 230, dash=True)
    s.box(560, 230, 300, 56, ["trade -> FAILED, event TradeSettlementFailed"], "red", title="rollBack()")
    s.arrow(560, 258, 450, 258, dash=True)
    s.box(250, 230, 200, 56, ["undo step 2"], "yellow", title="compensate")
    s.arrow(250, 258, 140, 258, dash=True)
    s.box(40, 230, 100, 56, ["undo step 1"], "yellow", title="compensate")
    s.text(40, 330, "Each step commits locally and the saga saves its progress (saga_instance.next_step).", 13)
    s.text(40, 352, "A crash leaves state RUNNING; resume() continues from the saved step.", 13)
    s.text(40, 374, "Steps and compensations must be idempotent. The last step is the pivot: after it, nothing may fail.", 13, bold=True)
    s.save("08-saga-outbox-cqrs/images/02-saga-compensation.svg")

    # 3. CQRS
    s = Svg(900, 400, "CQRS: separate models for writing and for reading")
    s.box(30, 150, 140, 70, ["POST /settlement"], "blue", title="Command")
    s.box(220, 150, 190, 70, ["trade, allocation,", "saga, outbox tables"], "green", title="Write model")
    s.box(460, 150, 150, 70, ["relay + events"], "purple", title="Events")
    s.box(660, 150, 200, 70, ["settled_trade_report", "(shaped for the question)"], "yellow", title="Read model")
    s.box(660, 290, 200, 60, ["GET /reports/settled"], "blue", title="Query")
    s.arrow(170, 185, 220, 185)
    s.arrow(410, 185, 460, 185)
    s.arrow(610, 185, 660, 185, "projector")
    s.arrow(760, 290, 760, 220, "reads only")
    s.text(30, 260, "Write side: rules and consistency.", 13)
    s.text(30, 282, "Read side: speed and shape; may lag by a moment", 13)
    s.text(30, 304, "(eventually consistent), and can be rebuilt by", 13)
    s.text(30, 326, "replaying the events.", 13)
    s.save("08-saga-outbox-cqrs/images/03-cqrs-read-model.svg")

    # 4. Flyway at startup
    s = Svg(900, 360, "What Flyway does at startup")
    s.box(30, 90, 190, 80, ["classpath:db/migration", "V1 ... V4"], "blue", title="1. find scripts")
    s.box(250, 90, 190, 80, ["flyway_schema_history", "(created if missing)"], "yellow", title="2. read history")
    s.box(470, 90, 190, 80, ["apply only the pending", "versions, in order"], "green", title="3. migrate")
    s.box(690, 90, 180, 80, ["Hibernate validate", "checks entities"], "purple", title="4. then JPA")
    s.arrow(220, 130, 250, 130)
    s.arrow(440, 130, 470, 130)
    s.arrow(660, 130, 690, 130)
    s.box(250, 230, 410, 80, ["a checksum differs from the one stored => startup stops", "fix: add a NEW migration, never edit an applied one"], "red", title="Protection")
    s.arrow(450, 170, 450, 230, dash=True)
    s.save("08-saga-outbox-cqrs/images/04-flyway-startup.svg")


def lesson09():
    # 1. topic, partitions, consumer group
    s = Svg(900, 460, "Kafka in one picture: topic, partitions, consumer group")
    s.box(30, 80, 150, 70, ["TradeSettled", "key = trade id"], "blue", title="Producer")
    s.frame(230, 55, 360, 250, "Topic  trade-events  (3 partitions)", "yellow")
    for i, y in enumerate([95, 165, 235]):
        s.box(250, y, 320, 50, ["0  1  2  3  4 ...  (offsets, append-only)"], "white", title=None, mono=True)
        s.text(256, y + 14, f"partition {i}", 11, bold=True)
    s.arrow(180, 115, 250, 120)
    s.arrow(180, 125, 250, 190)
    s.arrow(180, 135, 250, 260)
    s.text(30, 175, "the partition is chosen by", 12)
    s.text(30, 192, "hash(key) % 3", 12, bold=True)
    s.frame(640, 55, 240, 250, "Consumer group  trade-report", "green")
    s.box(660, 95, 200, 50, ["reads partition 0"], "green", title="consumer A")
    s.box(660, 165, 200, 50, ["reads partitions 1, 2"], "green", title="consumer B")
    s.arrow(570, 120, 660, 120)
    s.arrow(570, 190, 660, 190)
    s.arrow(570, 260, 660, 190)
    s.text(30, 340, "One partition is an ordered log. Equal keys always land in the same partition, so one trade's events stay in order.", 13)
    s.text(30, 362, "Inside a group each partition is read by exactly ONE consumer: more partitions = more parallelism (never more consumers than partitions).", 13)
    s.text(30, 384, "Another group (say, an e-mail sender) gets its own full copy of every record. Each group tracks its own offset per partition.", 13)
    s.text(30, 416, "Records are kept for a retention period (default 7 days) whether or not anyone has read them.", 13, bold=True)
    s.save("09-kafka/images/01-topic-partitions-group.svg")

    # 2. our end-to-end flow
    s = Svg(900, 440, "Our flow: outbox to Kafka to idempotent consumer (and the dead-letter topic)")
    s.box(20, 90, 130, 70, ["status + event", "one transaction"], "green", title="TradeService")
    s.box(180, 90, 120, 70, ["outbox_event"], "blue", title="table")
    s.box(330, 90, 120, 70, ["acks=all, key", "= trade id"], "purple", title="OutboxRelay")
    s.box(480, 90, 140, 70, ["trade-events"], "yellow", title="Kafka topic")
    s.box(650, 90, 230, 70, ["listener, then", "IdempotentEventProcessor"], "blue", title="@KafkaListener")
    s.arrow(150, 125, 180, 125)
    s.arrow(300, 125, 330, 125)
    s.arrow(450, 125, 480, 125)
    s.arrow(620, 125, 650, 125)
    s.box(650, 210, 230, 80, ["already in processed_event? skip", "else handlers + insert row (1 tx)"], "green", title="dedupe")
    s.arrow(765, 160, 765, 210)
    s.box(650, 330, 230, 70, ["settled_trade_report"], "yellow", title="read model")
    s.arrow(765, 290, 765, 330)
    s.box(330, 330, 270, 70, ["after 3 retries: parked here,", "same partition number"], "red", title="trade-events.DLT")
    s.arrow(650, 150, 600, 330, "handler keeps failing", dash=True, lx=560, ly=250)
    s.text(20, 215, "Publish succeeded only when the broker acknowledged:", 13)
    s.text(20, 237, "then the relay marks the outbox row published.", 13)
    s.text(20, 270, "A crash in between means the row is sent again:", 13)
    s.text(20, 292, "duplicates are expected, so the consumer is idempotent.", 13, bold=True)
    s.save("09-kafka/images/02-our-flow.svg")


def lesson10():
    # 1. image layers: what changes often sits on top
    s = Svg(900, 400, "The Docker image: layers, least-changing at the bottom")
    s.box(60, 60, 400, 50, ["eclipse-temurin:21-jre-jammy  (Java 21 + Linux)"], "grey", size=13)
    s.box(60, 120, 400, 60, ["dependencies/  lib/*.jar  about 89 MB", "changes only when pom.xml changes"], "blue", size=13)
    s.box(60, 190, 400, 50, ["snapshot-dependencies/  (none here)"], "blue", size=13)
    s.box(60, 250, 400, 60, ["application/  app.jar  about 111 KB", "changes on EVERY build"], "yellow", size=13)
    s.box(60, 320, 400, 50, ["USER 10001 + ENTRYPOINT java -jar app.jar"], "green", size=13)
    s.text(500, 100, "Docker caches each layer by its content.", 14, bold=True)
    s.text(500, 130, "You change one class and rebuild:", 14)
    s.text(500, 156, "- the 89 MB dependency layer is REUSED", 14)
    s.text(500, 180, "- only the 111 KB application layer is rebuilt", 14)
    s.text(500, 204, "- a push to the registry sends 111 KB, not 89 MB", 14)
    s.text(500, 250, "If we kept one fat jar layer, every build", 14)
    s.text(500, 274, "would rebuild and push all 89 MB again.", 14)
    s.text(500, 320, "Stage 1 of the Dockerfile only SPLITS the jar", 14, bold=True)
    s.text(500, 344, "(java -Djarmode=tools extract --layers).", 14)
    s.text(500, 368, "Stage 2 is the image that actually ships.", 14)
    s.save("10-containers-kubernetes/images/01-image-layers.svg")

    # 2. the Kubernetes objects of this lesson
    s = Svg(900, 440, "The Kubernetes objects in k8s/ and how they point at each other")
    s.box(40, 60, 190, 60, ["Ingress / gateway", "(not in this lesson)"], "grey", size=13)
    s.box(40, 170, 190, 70, ["Service", "stable name + port 80"], "green", size=13)
    s.box(300, 60, 220, 70, ["Deployment", "replicas: 2, rolling update"], "blue", size=13)
    s.box(300, 170, 220, 60, ["ReplicaSet (created by", "the Deployment)"], "blue", size=13)
    s.box(300, 280, 100, 80, ["Pod 1", "app container", "8080"], "yellow", size=12)
    s.box(420, 280, 100, 80, ["Pod 2", "app container", "8080"], "yellow", size=12)
    s.box(600, 60, 260, 60, ["ConfigMap: profiles, URLs"], "purple", size=13)
    s.box(600, 140, 260, 60, ["Secret: JWT_SECRET, DB password"], "red", size=13)
    s.box(600, 230, 260, 60, ["HorizontalPodAutoscaler", "2 to 6 pods on CPU"], "grey", size=13)
    s.box(600, 310, 260, 60, ["PodDisruptionBudget", "keep at least 1 pod"], "grey", size=13)
    s.arrow(135, 120, 135, 170)
    s.arrow(230, 205, 300, 310, "label app=trade-service", lx=200, ly=290)
    s.arrow(410, 130, 410, 170)
    s.arrow(410, 230, 350, 280)
    s.arrow(410, 230, 470, 280)
    s.arrow(600, 90, 520, 100, "envFrom (both)", lx=560, ly=80)
    s.arrow(600, 170, 520, 118)
    s.arrow(600, 260, 520, 125, "scales", dash=True, lx=545, ly=215)
    s.text(450, 415, "Postgres and Kafka stay OUTSIDE the cluster here (managed services); the ConfigMap holds their addresses.", 13, anchor="middle")
    s.save("10-containers-kubernetes/images/02-k8s-objects.svg")

    # 3. what happens to a pod being replaced
    s = Svg(900, 360, "Replacing a pod without dropping requests (time runs left to right)")
    s.box(30, 70, 190, 70, ["new pod starts", "startupProbe passes"], "green", size=13)
    s.box(250, 70, 190, 70, ["readinessProbe UP", "Service adds it"], "green", size=13)
    s.arrow(220, 105, 250, 105)
    s.box(30, 190, 160, 70, ["old pod marked", "Terminating"], "yellow", size=13)
    s.box(215, 190, 160, 70, ["preStop: sleep 5", "Service stops routing"], "yellow", size=13)
    s.box(400, 190, 160, 70, ["SIGTERM to java", "graceful shutdown"], "red", size=13)
    s.box(585, 190, 160, 70, ["in-flight requests", "finish (max 20 s)"], "red", size=13)
    s.box(770, 190, 110, 70, ["exit 143", "pod gone"], "grey", size=13)
    for x in (190, 375, 560, 745):
        s.arrow(x, 225, x + 25, 225)
    s.text(30, 300, "SIGKILL comes at terminationGracePeriodSeconds = 30 s: 5 s preStop + 20 s Spring limit leaves 5 s spare.", 13)
    s.text(30, 325, "maxUnavailable: 0 means the old pod is only removed after the new one is ready, so capacity never dips.", 13)
    s.save("10-containers-kubernetes/images/03-rolling-update.svg")


def lesson11():
    # 1. the architecture we draw on the whiteboard
    s = Svg(940, 520, "Settlement platform: the whiteboard architecture")
    s.box(20, 70, 150, 70, ["Clients / custodians", "REST + ISO 20022 files"], "grey", size=13)
    s.box(210, 70, 170, 70, ["API gateway", "TLS, JWT, rate limit"], "purple", size=13)
    s.box(420, 70, 200, 70, ["Instruction service", "validate, store, idempotent"], "blue", size=13)
    s.box(420, 190, 200, 60, ["PostgreSQL", "instruction + OUTBOX (one tx)"], "yellow", size=13)
    s.box(660, 70, 130, 70, ["Kafka", "key = instruction id"], "green", size=13)
    s.box(660, 200, 130, 60, ["Matching", "consumer group"], "blue", size=13)
    s.box(660, 290, 130, 70, ["Settlement saga", "orchestrator"], "blue", size=13)
    s.box(820, 290, 100, 70, ["CSD / T2S", "gateway", "retry, breaker"], "red", size=12)
    s.box(420, 290, 200, 70, ["Cash + securities", "ledgers (reserve, deliver,", "compensate)"], "yellow", size=12)
    s.box(820, 180, 100, 80, ["Read model", "(CQRS)", "status, search"], "green", size=12)
    s.box(690, 410, 230, 70, ["Reconciliation", "end of day vs CSD statements"], "purple", size=13)
    s.box(210, 410, 230, 70, ["Observability + ops", "lag, DLT, fails, audit"], "grey", size=13)
    s.arrow(170, 105, 210, 105)
    s.arrow(380, 105, 420, 105)
    s.arrow(520, 140, 520, 190, "same transaction", lx=585, ly=170)
    s.arrow(620, 105, 660, 105, "relay", lx=640, ly=95)
    s.arrow(725, 140, 725, 200)
    s.arrow(725, 260, 725, 290)
    s.arrow(660, 325, 620, 325)
    s.arrow(790, 325, 820, 325)
    s.arrow(790, 120, 850, 180, "events", lx=850, ly=150)
    s.arrow(870, 360, 820, 410, "statements", dash=True, lx=880, ly=395)
    s.text(20, 505, "Business changes and events share one database transaction; everything after Kafka is at-least-once and idempotent.", 13)
    s.save("11-system-design-settlement/images/01-architecture.svg")

    # 2. instruction lifecycle
    s = Svg(940, 360, "Settlement instruction lifecycle (a simplified state machine)")
    s.box(20, 130, 120, 60, ["RECEIVED"], "grey", size=14)
    s.box(190, 130, 120, 60, ["VALIDATED"], "blue", size=14)
    s.box(360, 130, 120, 60, ["MATCHED"], "blue", size=14)
    s.box(530, 130, 140, 60, ["SETTLING", "(saga running)"], "yellow", size=13)
    s.box(740, 60, 160, 60, ["SETTLED"], "green", size=14)
    s.box(740, 230, 160, 60, ["FAILED / PENDING", "retry next cycle"], "red", size=13)
    s.box(190, 260, 120, 50, ["REJECTED"], "red", size=14)
    s.box(360, 260, 120, 50, ["CANCELLED"], "grey", size=14)
    s.arrow(140, 160, 190, 160)
    s.arrow(310, 160, 360, 160)
    s.arrow(480, 160, 530, 160)
    s.arrow(670, 145, 740, 100, "all steps ok", lx=690, ly=112)
    s.arrow(660, 190, 740, 245, "step failed", lx=745, ly=205)
    s.arrow(250, 190, 250, 260, "invalid", lx=275, ly=230)
    s.arrow(420, 190, 420, 260, "cancel request", lx=470, ly=232)
    s.arrow(740, 275, 600, 190, "recycle", dash=True, lx=640, ly=270)
    s.text(20, 340, "Every arrow is one transactional state change plus one outbox event. Terminal states: SETTLED, REJECTED, CANCELLED.", 13)
    s.save("11-system-design-settlement/images/02-lifecycle.svg")


if __name__ == "__main__":
    targets = sys.argv[1:] or ["07"]
    for t in targets:
        globals()["lesson" + t]()
