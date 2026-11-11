# 99. Cheatsheet and interview Q&A

**Read this the night before, once, slowly.** It is a compressed map of lessons 00-12, not a replacement for them. Each section names the lesson to re-read when something is not clear. Every setting and number quoted here is the one used in the reference project (`reference/lesson-10`) unless it says "general".

Contents: 1 Course map. 2 Annotations. 3 Configuration keys. 4 HTTP status codes. 5 Pattern decision table. 6 Numbers to quote. 7 Commands. 8 Symptom lookup. 9 Rapid-fire Q&A. 10 Last-night checklist.

## 1. Course map in one table

| Lesson | Core idea | One sentence to say |
|---|---|---|
| 00 Big picture | Boot = Spring + starters + auto-config + embedded server + Actuator | "One runnable jar; layers controller, service, repository; calls go downward only" |
| 01 Core | IoC, DI, AOP | "The container creates and wires beans; AOP is proxies, so self-invocation bypasses them" |
| 02 Boot internals | Auto-config, config sources, profiles, Actuator | "Conditional configuration that backs off when I define my own bean; one jar, many environments" |
| 03 REST, validation | MVC flow, status codes, problem+json | "Shape validation with annotations, business rules in the service, one advice for errors" |
| 04 Data JPA | Repositories, transactions, N+1 | "`@Transactional` is a proxy; N+1 is measured and fixed with a fetch join" |
| 05 Security, JWT | Filter chain, stateless tokens | "401 unknown, 403 refused; JWT is signed not encrypted; short lifetime" |
| 06 Testing | Pyramid, slices, fakes | "Each rule at the lowest level that can show it fail; real database for persistence proof" |
| 07 Resilience | Retry, breaker, bulkhead, idempotency | "Retry only idempotent calls; fallback on the outermost annotation; never a transaction around a remote call" |
| 08 Saga, outbox, CQRS, Flyway | Consistency without 2PC | "State and event in one transaction; saga with compensation; read model from events; Flyway owns the schema" |
| 09 Kafka | Log, partitions, groups, offsets | "At-least-once plus idempotent consumer; order per key; dead-letter topic" |
| 10 Containers, Kubernetes | Image, probes, graceful shutdown | "One image for all environments; liveness is the process only; rolling update loses no requests" |
| 11 System design | The eight-step method | "Clarify, estimate, interfaces, data, architecture, deep dives, failures, trade-offs" |
| 12 Mock interview | Delivery | "Headline, reason, trade-off, failure mode; 60-90 seconds" |

## 2. Annotations

**Core and Boot**

| Annotation | Meaning |
|---|---|
| `@SpringBootApplication` | `@SpringBootConfiguration` + `@EnableAutoConfiguration` + `@ComponentScan` (scans its package and below) |
| `@Component`, `@Service`, `@Repository`, `@Controller`, `@RestController` | Stereotypes: found by scanning and registered as beans. `@RestController` = `@Controller` + `@ResponseBody` |
| `@Configuration` + `@Bean` | Define beans in code (third-party classes, conditional wiring) |
| `@Autowired` | Injection. With one constructor it is implicit: prefer **constructor injection** |
| `@Primary`, `@Qualifier` | Choose among several beans of one type (otherwise `NoUniqueBeanDefinitionException`) |
| `@Scope` | Default singleton (one per container, shared by threads, keep stateless); `prototype`, `request`, `session` |
| `@Value`, `@ConfigurationProperties` | Single value versus a typed group bound from properties; prefer the second, with `@Validated` |
| `@Profile`, `@ConditionalOnProperty`, `@ConditionalOnMissingBean` | Register a bean only in a profile, when a property has a value, or when the user has not defined one (back-off) |
| `@Aspect`, `@Around`, `@Pointcut` | AOP: advice around method calls, applied through proxies |
| `@Scheduled` | Run a method periodically (needs `@EnableScheduling`); used by the outbox relay |

**Web and validation**

| Annotation | Meaning |
|---|---|
| `@RequestMapping`, `@GetMapping`, `@PostMapping` | Map HTTP method and path |
| `@PathVariable`, `@RequestParam`, `@RequestBody` | Take values from the path, the query string, the JSON body |
| `@Valid` / `@Validated` | Trigger Bean Validation on the body (before the method runs) |
| `@NotBlank`, `@NotNull`, `@Positive`, `@Pattern` | Constraints on fields |
| `@RestControllerAdvice`, `@ExceptionHandler` | One place that maps exceptions to problem+json responses |
| `@ResponseStatus` | A fixed status for a handler or an exception |

**Data**

| Annotation | Meaning |
|---|---|
| `@Entity`, `@Table`, `@Id`, `@GeneratedValue` | JPA mapping |
| `@OneToMany`, `@ManyToOne`, `@ManyToMany` | Relations. Collections are **lazy** by default; `@ManyToOne` is **eager** by default (a common surprise) |
| `@Transactional` | A proxy opens and commits a transaction. Default propagation `REQUIRED`; rolls back on **unchecked** exceptions and errors; `rollbackFor` to include checked ones; `readOnly = true` as a hint |
| `@EntityGraph`, `join fetch` | Fix N+1 by loading the association in the same query |
| `@Query` | An explicit JPQL or native query |

**Security**

| Annotation / type | Meaning |
|---|---|
| `SecurityFilterChain` bean | The security rules as a chain of filters; ours: two chains (Basic for `/auth/**`, bearer token for the API) |
| `@EnableMethodSecurity`, `@PreAuthorize("hasRole('OPS')")` | Role checks next to the code |
| `JwtEncoder`, `JwtDecoder` | Sign and validate tokens (HS256 here with a 32+ character secret) |

**Testing**

| Annotation | Meaning |
|---|---|
| `@SpringBootTest` | Full context. `webEnvironment = RANDOM_PORT` starts the real server; `NONE` no web server |
| `@WebMvcTest` | Only the web layer (controllers, advice, security); beans mocked with `@MockitoBean` |
| `@DataJpaTest` | Only JPA with an embedded database; rolls back after each test |
| `@AutoConfigureMockMvc` | Adds `MockMvc` to a full-context test |
| `@ActiveProfiles` | Activate profiles in a test |
| `@ParameterizedTest`, `@Nested` | Many inputs for one test; group tests |
| `@EmbeddedKafka` | An in-JVM Kafka broker for tests (no Docker) |
| `@Testcontainers`, `@Container`, `@ServiceConnection` | A real PostgreSQL in Docker, wired as the datasource |

**Resilience, messaging**

| Annotation | Meaning |
|---|---|
| `@Retry`, `@CircuitBreaker`, `@Bulkhead` (Resilience4j) | Nest as Retry( CircuitBreaker( Bulkhead( call ) ) ); put `fallbackMethod` on the **outermost** |
| `@KafkaListener` | A listener container polls and calls the method; offset committed after it returns (`ack-mode: record`) |

## 3. Configuration keys used in the project

| Key | Value / meaning |
|---|---|
| `spring.profiles.default: dev` | Used only when no profile is activated |
| `spring.jpa.hibernate.ddl-auto: validate` | Flyway owns the schema; Hibernate only checks |
| `spring.jpa.open-in-view: false` | No lazy loading in the view layer; mistakes fail early |
| `spring.kafka.producer.acks: all`, `enable.idempotence: true` | Durable, duplicate-free producer |
| `spring.kafka.consumer.group-id`, `auto-offset-reset: earliest`, `enable-auto-commit: false` | New group starts at the oldest record; Spring commits after the listener |
| `spring.kafka.listener.ack-mode: record` | Commit after each record |
| `trade.events.transport: in-process \| kafka` | Selects the event transport (profile `kafka` sets `kafka`) |
| `trade.outbox.poll-interval-ms: 2000` | How often the relay looks for unpublished events |
| `trade.security.jwt-secret: ${JWT_SECRET:}` (prod) | No default: a missing secret stops the start-up |
| `trade.security.token-ttl: 15m` | Token lifetime: short, because revocation is not instant |
| `management.endpoints.web.exposure.include: health,info` | Expose little; dev opens more |
| `management.endpoint.health.probes.enabled: true` | `/actuator/health/liveness` and `/readiness` |
| `server.shutdown: graceful` | Finish running requests on SIGTERM |
| `spring.lifecycle.timeout-per-shutdown-phase: 20s` | Cap for that wait (below the platform's grace period) |
| `logging.structured.format.console: ecs` | JSON logs, one object per line (Boot 3.4+) |

**Where a setting comes from (highest wins):** command line, environment variables (relaxed binding: `SPRING_DATASOURCE_URL` is `spring.datasource.url`), profile file (`application-prod.yml`), base `application.yml`, code defaults.

## 4. HTTP status codes in the course

| Situation | Status |
|---|---|
| Created a resource | `201` + `Location` header |
| Read or action succeeded | `200` |
| Malformed JSON, failed validation | `400` |
| Not authenticated (no or bad token) | `401` |
| Authenticated but not allowed (wrong role) | `403` |
| Unknown id | `404` |
| Business state conflict (already settled) | `409` |
| Unexpected error | `500` with a generic body; detail only in the log |
| Dependency refused by the open circuit | A visible fallback response (lesson 07), not a hung call |
| Health `DOWN` | `503` |

Error bodies are `application/problem+json` (`type`, `title`, `status`, `detail`, `instance`).

## 5. Pattern decision table

| Problem | Pattern | Remember |
|---|---|---|
| Two writes (DB and broker) must not diverge | **Transactional outbox** | State and event in one transaction; relay publishes after the broker's acknowledgement; at-least-once |
| Work across services with no global transaction | **Saga** (orchestrated) | Persist progress; compensate in reverse; steps and compensations idempotent; no isolation |
| Duplicates after retries or redelivery | **Idempotent consumer / idempotency key** | Dedup table in the same transaction as the effect; unique key on the API |
| A dependency is slow or down | **Timeout + retry (backoff, jitter) + circuit breaker + bulkhead + fallback** | Retry only idempotent operations and only technical errors |
| Different read and write needs | **CQRS read model** | Built from events; eventually consistent; rebuildable |
| Schema changes across releases | **Flyway + expand and contract** | Never edit an applied migration; two-step deployment |
| A message that never succeeds | **Dead-letter topic** | Bounded retries, alert, replay tool |
| Ordering | **Partition key** | Order per key only |
| Rolling deployment without errors | **Readiness + preStop + graceful shutdown + maxUnavailable 0** | Sum of delays below the grace period |
| Migrating a legacy batch system | **Strangler fig + shadow mode + reconciliation** | No big bang |
| Two services need the same data | **Events or an API, not a shared database** | Each service owns its data |

## 6. Numbers to quote

| Item | Value |
|---|---|
| Spring Boot / Java | 3.5.x / 21 |
| Retry | 3 attempts, 200 ms base wait, x2 backoff, jitter factor 0.5; ignores business rejections and the open-circuit exception |
| Circuit breaker | Count-based window of 10; minimum 5 calls; opens at 50 percent failures or 80 percent slow calls (slow = over 2 s); open for 10 s; 3 probe calls in half-open |
| Bulkhead | 10 concurrent calls; waiting time 0 (fail fast) |
| JWT | HS256, secret of at least 32 characters, lifetime 15 minutes |
| Outbox polling | Every 2 s |
| Kafka producer | `acks=all`, idempotence on; delivery timeout 30 s; the relay waits up to 10 s for the acknowledgement |
| Kafka production advice (general) | Replication 3, `min.insync.replicas=2`, partitions chosen for consumer parallelism |
| Container memory | 512 Mi limit = request; heap 75 percent (`MaxRAMPercentage`); measured about 351 MiB used |
| Image | 573 MB on disk; dependency layer 88.6 MB, our code layer 111 kB |
| Shutdown | Spring waits up to 20 s; preStop sleeps 5 s; grace period 30 s; measured stop with nothing in flight 0.6 s, exit code 143 |
| Exit codes | 143 = SIGTERM (normal stop), 137 = SIGKILL (often OOM or grace period exceeded), 1 = error |
| Probes | startup: 30 x 5 s; liveness every 10 s, 3 failures; readiness every 5 s, 3 failures |
| HPA | 2 to 6 replicas at 70 percent of the CPU request (250m) |
| Capacity example | 5M instructions a day, 12 h window: 116 per second average, about 1,160 peak, 40 million events (40 GB) a day, 10 TB a year |
| Tests | 113 in the final project (23 test classes), 2 on real PostgreSQL, 4 on an embedded Kafka |

## 7. Commands

**Maven (from the project folder, PowerShell)**

| Command | Why |
|---|---|
| `.\mvnw.cmd -q clean verify` | Full build and all tests, quiet: no output means success |
| `.\mvnw.cmd "-Dtest=ClassName" test` | One test class |
| `.\mvnw.cmd -q -DskipTests package` | Build the jar without tests |
| `.\mvnw.cmd -q spring-boot:run "-Dspring-boot.run.profiles=dev,kafka"` | Run with profiles |

**Docker and Compose**

| Command | Why |
|---|---|
| `docker version --format "{{.Server.Version}}"` | Is the engine running? |
| `docker build -t trade-service:0.0.1 .` | Build the image |
| `docker compose up -d --build` | Start the stack (build first) |
| `docker compose ps` / `logs -f app` / `down` / `down -v` | Status / logs / stop / stop and delete data |
| `docker history <image>` | Layers and sizes |
| `docker stop <c>` then `docker inspect <c> --format "{{.State.ExitCode}}"` | Test graceful shutdown (143 is good) |
| `docker exec trade-postgres psql -U trade -d trades -c "<sql>"` | Query the database in the container |
| `docker exec trade-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list` | List topics (use PowerShell: Git Bash rewrites `/opt` paths) |

**Kubernetes** (listed for knowledge; **not run** in this course because no cluster was available)

| Command | Why |
|---|---|
| `kubectl apply -f k8s/` | Create or update the objects |
| `kubectl -n trade get pods` / `describe pod <p>` / `logs <p> --previous` | Status / events / the crashed container's log |
| `kubectl -n trade rollout status\|undo deployment/trade-service` | Watch / roll back |
| `kubectl -n trade port-forward svc/trade-service 8080:80` | Reach a Service from your laptop |
| `kubectl -n trade get endpoints trade-service` | Empty means the selector matches no ready pod |
| `python tools\check_k8s.py reference\lesson-10\k8s` | Offline structure check |

**API calls (PowerShell)**

| Command | Why |
|---|---|
| `curl.exe -s -u alice:password -X POST localhost:8080/auth/token` | Get a JWT (alice TRADER, bob OPS, carol VIEWER, admin ADMIN) |
| `curl.exe -s -H "Authorization: Bearer $TOKEN" localhost:8080/trades` | Call the API |
| `curl.exe -s localhost:8080/actuator/health/readiness` | The probe endpoint |

**Revision**

| Command | Why |
|---|---|
| `python tools\drill.py -n 10 --answers` | Ten random quiz questions, answer aloud, then compare |

## 8. Symptom lookup

| Symptom | First thing to check |
|---|---|
| `NoUniqueBeanDefinitionException` | Two beans of one type: `@Primary` or `@Qualifier` |
| `@Transactional` does nothing | Self-invocation, a private or final method, a swallowed exception, a checked exception without `rollbackFor` |
| `LazyInitializationException` | Association accessed outside the transaction: fetch join or a DTO |
| Slow list endpoint | N+1: count statements; fetch join or entity graph |
| `401` | Missing or expired token, wrong secret or issuer |
| `403` | Role missing; check `@PreAuthorize` and the token's roles |
| Retry makes an outage worse | No backoff or jitter; retrying permanent errors; no breaker |
| Duplicate booking | A non-idempotent retry or consumer: key and unique constraint |
| Consumer lag grows | Slow handler, poison record, fewer consumers than partitions, rebalance storm |
| `Unsupported Database` (Flyway on PostgreSQL) | Add `flyway-database-postgresql` |
| Container exit 137 | OOM kill or SIGKILL: memory limit, heap percentage, shell-form `ENTRYPOINT` |
| `Unable to access jarfile` | The jar name in `ENTRYPOINT` (after layer extraction it is `app.jar`) |
| Pod `CrashLoopBackOff` | `logs --previous`, probe timing, a missing Secret |
| Service has no endpoints | Selector versus labels; readiness failing |
| Errors at every deployment | preStop delay, graceful shutdown, readiness gate |

## 9. Rapid-fire Q&A

Answer each in 60-90 seconds in the four moves (headline, reason, trade-off, failure mode). The lines below are the **headline plus the key reason**.

**Spring core and Boot**

1. **IoC versus DI?** IoC: the container controls creation and wiring; DI is how: dependencies are handed to the object (preferably through the constructor, so they are final and testable).
2. **How does auto-configuration work?** Conditional `@Configuration` classes registered through a list in the starter jars; a condition such as "class present and no bean of that type" decides. `--debug` prints the conditions report.
3. **Why is `@Transactional` sometimes ignored?** It works through a proxy: calling a method on `this` bypasses it; private or final methods cannot be proxied; a swallowed or checked exception does not roll back by default.
4. **Bean scopes?** Singleton by default (shared, so stateless), prototype, and web scopes request and session.
5. **How do profiles and config precedence work?** Profile files layer over the base file; environment variables and the command line win; secrets come from the environment, never the image.
6. **What is Actuator for?** Operational endpoints: health (with liveness and readiness groups), metrics through Micrometer, info, conditions. Expose little, secure the rest.

**REST, data, security, testing**

7. **A good error response?** `application/problem+json` from one `@RestControllerAdvice`; domain exceptions carry no HTTP; the unexpected becomes a generic `500` with the detail in the log.
8. **What is N+1 and how do you fix it?** One query for the parents plus one per child with lazy loading; fix with a fetch join or entity graph; verify by counting statements.
9. **JPA: persistence context and dirty checking?** The first-level cache of managed entities per transaction; changes to managed entities are written at flush without calling `save`.
10. **401 versus 403?** 401: not authenticated; 403: authenticated but refused.
11. **Is a JWT encrypted?** No: signed, so tampering is detected, but the claims are readable; keep it short-lived; revocation is not instant.
12. **Why stateless security and CSRF off?** Tokens in the `Authorization` header are not sent automatically by the browser, so CSRF does not apply; with cookie-based sessions it must stay on.
13. **How do you test a controller?** `@WebMvcTest` with a mocked service for the web contract, plus a few full-stack tests; each rule at the lowest level that can show it fail.
14. **Mock, fake or real?** Real or fakes by default; mocks for boundaries and "must not be called" checks; contract tests keep fakes honest.

**Microservices and resilience**

15. **Microservices or a monolith?** Start modular monolith; split along business capabilities when team or scaling needs justify the cost of distribution.
16. **Retry safely?** Only idempotent operations, with a stable key, backoff and jitter, a cap, technical errors only.
17. **How does a circuit breaker work?** Closed counts outcomes; open at the failure or slow-call threshold and refuses calls; half-open lets a few probes through and closes if they succeed.
18. **Bulkhead?** A limit on concurrent calls to one dependency so it cannot consume all threads.
19. **Where does the fallback go?** On the outermost annotation, otherwise it would swallow the failures the retry needs to see.

**Consistency and messaging**

20. **Why not two-phase commit?** Locks, availability, coupling, and external parties (a CSD, a broker) do not take part.
21. **What is the dual-write problem?** Updating the database and publishing to the broker separately: a crash between them leaves one without the other, in either order.
22. **How does the outbox solve it?** The event is a row written in the same transaction as the change; a relay publishes it and marks it after the acknowledgement; at-least-once, so consumers deduplicate.
23. **Saga, orchestration versus choreography?** A sequence of local transactions with compensations. Orchestration has a central coordinator that tracks state (clear, testable); choreography reacts to events (loose but spread out).
24. **CQRS?** Separate models for writes and reads; the read model is built from events and is eventually consistent.
25. **Kafka versus a queue?** A replicated log: retained records, consumer offsets, replay, many groups; a queue deletes after acknowledgement and offers richer per-message features.
26. **Ordering in Kafka?** Per partition only; choose the key so that what must stay in order shares it.
27. **Exactly-once?** Within Kafka (idempotent producer plus transactions). With side effects outside, at-least-once plus an idempotent consumer.
28. **Poison message?** Bounded retries, then a dead-letter topic with an alert and a replay tool.
29. **Flyway rules?** Versioned SQL migrations with checksums; never edit an applied one; Hibernate only validates; schema changes backward compatible (expand and contract).

**Containers and Kubernetes**

30. **Container versus VM?** An isolated process sharing the host kernel versus a full guest OS; faster and smaller, weaker isolation, so harden.
31. **A good Dockerfile for Boot?** Multi-stage, slim JRE, layered jar, non-root numeric user, exec-form entrypoint, `MaxRAMPercentage`, `.dockerignore`, no secrets.
32. **Liveness versus readiness versus startup?** Restart if broken; take traffic or not; protect a slow start. Dependencies do not belong in liveness.
33. **Zero-downtime deploy?** Rolling update with `maxUnavailable: 0`, readiness gate, preStop delay, graceful shutdown inside the grace period, at least two replicas and a PDB, backward-compatible schema.
34. **Requests versus limits?** Requests are reserved and used for scheduling and the autoscaler; limits are ceilings: memory over the limit is an OOM kill; CPU over the limit is throttled.
35. **How do pods find each other?** Services select pods by labels and route only to ready ones; DNS names such as `trade-service.trade`.
36. **Config and secrets in Kubernetes?** ConfigMap and Secret as environment variables or files; a Secret is only base64, so use RBAC, encryption at rest and an external store.
37. **Why Testcontainers?** The real database in the test: no H2 differences, no shared test database.

**Design and behaviour**

38. **How do you structure a design answer?** Clarify, estimate, interfaces, data, architecture, deep dives, failures and operations, trade-offs and evolution; say the plan first.
39. **How do you prevent a double settlement?** Unique client reference, idempotency key on the CSD call, unique constraint on the confirmation, idempotent consumers, persisted saga state, reconciliation as the detective control.
40. **How do you migrate a batch platform?** Strangler: run beside it in shadow mode, compare outputs, move one flow at a time, keep reconciliation as a safety net.
41. **When you do not know?** The closest thing you know, reasoning from first principles, and how you would verify.
42. **Troubleshooting method?** Detect and scope, isolate with evidence, mitigate then fix, prevent (test, alert, runbook, design change).

## 10. Last-night checklist

- [ ] Re-read the **Recap** (section 11) of lessons 00-12 (about 30 minutes in total).
- [ ] Read this file once. Do **not** try to memorise it all.
- [ ] `python tools\drill.py -n 15 --answers`: answer aloud; mark misses; re-read only those lessons' sections 3, 6 and 7.
- [ ] Say your **90-second pitch** and your **six stories** once each (lesson 12).
- [ ] Draw the settlement architecture (lesson 11) from memory on a blank page, with the numbers.
- [ ] Prepare **three questions** for the interviewer.
- [ ] Prepare the room: water, pen, notepad, charger, camera, a test call.
- [ ] Remember the honesty line: *what I built and ran* (the reference project: outbox, saga, Kafka, resilience, Docker, PostgreSQL tests), *what I only checked offline* (the Kubernetes manifests), *what I did not build* (a matching engine, real CSD connectivity, reconciliation, multi-region).
- [ ] Sleep.
