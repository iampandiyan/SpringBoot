# 12. Mock interview and story rehearsal

**No new code.** This lesson is about **delivery**: turning what you built in lessons 00-11 and what you did in your career into answers you can say clearly, out loud, in the right amount of time. You practise against a script, a timer and a scoring sheet. One small tool, `tools/drill.py`, asks you random questions from every lesson's quiz.

**Starting point:** lessons 00-11 read; the quizzes at least attempted once.
**End state:** you have run a complete **mock interview** (about 90 minutes in six rounds), you have **six rehearsed stories** that are true and have numbers, a 90-second self-introduction, a list of questions to ask the interviewer, and a list of your weak topics to re-read tonight.

## 1. Goal and time box

**120 minutes** (20 reading, 90 practice, 10 planning tonight's revision). After this lesson you can:
- Answer any technical question in a **fixed shape** (headline, reason, trade-off, failure mode) and stop on time.
- Tell **six career stories** in the STAR+ format with numbers, and adapt one story to several different questions.
- Give a **90-second introduction** that sells you as an architect who has moved from EJB-era enterprise systems to Spring, containers and events.
- Handle **troubleshooting scenarios** out loud, in a structured way: detect, isolate, fix, prevent.
- Say **"I do not know"** without losing credibility, and ask good questions at the end.
- Score yourself with a **rubric** and know which lesson to re-read for each weakness.

## 2. Why it matters

Most candidates who fail architect interviews do not fail on knowledge. They fail on **delivery**: answers that are too long, no structure, no trade-offs, no numbers, a story that never reaches its result, bluffing when they do not know. Delivery is a skill: it improves with out-loud practice and does not improve by re-reading.

| You already do this at work | In the interview |
|---|---|
| Explain an incident to management in five minutes | A structured answer with the headline first |
| Write a post-incident report (cause, impact, fix, prevention) | The STAR+ story and the troubleshooting answer |
| Defend a design in a review meeting | The trade-off questions |
| Say "I will check and come back to you" | "I have not used that; here is how I would find out" (credible, not weak) |
| Present a release plan | The system-design round (lesson 11) |

## 3. Concepts explained like a teacher

### The shape of the interview loop

Loops differ by company, but for an architect role expect:

| Round | What they test | Typical length |
|---|---|---|
| Recruiter or hiring-manager screen | Fit, level, motivation, your introduction | 30 min |
| Technical deep dive (Java, Spring, data, messaging) | Depth and honesty: can you go three levels down on something you claim? | 45-60 min |
| System design | Method, numbers, trade-offs, failure thinking (lesson 11) | 45-60 min |
| Behavioural and leadership | Judgement, conflict, ownership, mentoring | 45 min |
| Final with a senior leader | Vision, communication, "would I trust this person with a platform?" | 30-45 min |

**Depth beats breadth.** An interviewer picks something from your CV and asks "why?" repeatedly. Whatever you list, be ready for three levels of follow-up. If you cannot, do not list it.

### The answer shape: headline, reason, trade-off, failure mode

Every technical answer follows four moves. It keeps you short and shows architect thinking.

| Move | Example for "How do you make a Kafka consumer reliable?" |
|---|---|
| **1. Headline** (one sentence) | "At-least-once delivery plus an idempotent consumer." |
| **2. Reason** | "Commit the offset after processing so a crash redelivers rather than loses; duplicates then happen, so processing must tolerate them." |
| **3. Trade-off** | "Exactly-once inside Kafka exists, but it does not cover my database write, so I would not rely on it there." |
| **4. Failure mode** | "A poison record would block the partition, so after bounded retries it goes to a dead-letter topic with an alert." |

**Time rule:** a normal question is answered in **60 to 90 seconds**. Then stop and say "I can go deeper on any part". A 5-minute monologue wastes the interviewer's time and loses the thread.

**Technique: headline first.** Start with the answer, not the story that leads to it. "Use the outbox pattern" first, then why.

**Technique: three levels deep.** For each topic you claim, be able to answer: *what is it*, *how does it work inside*, *how does it fail*. Lesson sections 3, 6 and 7 are written for exactly this.

### When you do not know

Honesty is graded positively **if** you do it well:

1. Say what you **do** know that is closest: "I have not used Spring Cloud Gateway in production; I have used a reverse proxy for the same job."
2. Reason aloud from first principles: "I would expect it to ..."
3. Say how you would **verify**: "I would check the documentation and test it with a small experiment."

Never invent. A bluff that is caught ends the interview; an honest gap rarely does.

### The STAR+ story format

A story is one **true** episode from your career, told in under **two minutes**. Use **STAR plus reflection**:

| Part | Content | Time |
|---|---|---|
| **S**ituation | The system, the team, the problem, in two sentences. Make the stakes clear (money, regulation, deadline) | 15 s |
| **T**ask | What **you** were responsible for. "I was asked to ..." | 10 s |
| **A**ction | What **you** did, in three or four concrete steps. "I", not "we". Include the decision and the alternative you rejected | 50 s |
| **R**esult | Numbers: time saved, errors removed, incidents avoided, money. If you have no number, an honest estimate marked as estimate | 20 s |
| **+ Reflection** | What you learned or would do differently | 15 s |

**Rules.** Only true stories. Numbers you can defend if asked "how did you measure that?". Name what **you** did, not the team. Prepare for the follow-up "what would you do differently?".

### The story bank: six stories, many questions

Prepare **six** stories; each can answer several questions. Pick them from your real work (batch/ETL, Oracle, Hibernate, EJB, Elasticsearch, SWIFT and T2S messaging):

| Slot | Prompt: a time when you ... | Answers questions such as |
|---|---|---|
| 1. **Incident** | ... found and fixed a serious production problem | "Tell me about a crisis", "how do you debug", "under pressure" |
| 2. **Design decision** | ... chose between two designs and had to defend it | "A hard technical decision", "trade-offs", "how do you decide" |
| 3. **Data correctness** | ... dealt with duplicates, a reconciliation break or lost messages | "Reliability", "quality", "an ugly bug" |
| 4. **Performance or scale** | ... made something much faster or able to handle more | "Optimisation", "measuring", "a bottleneck" |
| 5. **Change or migration** | ... modernised something old (for example EJB to Spring, batch to events, a database move) | "Leading change", "legacy", "risk management" |
| 6. **People** | ... disagreed with someone, mentored someone, or admitted a mistake | "Conflict", "leadership", "a failure", "feedback" |

**Template to fill in (one per story, bullet points, not a script):**

```
Title:
Situation (2 sentences; the stakes):
Task (my responsibility):
Action 1 / 2 / 3 / 4 (what I did; the alternative I rejected and why):
Result (numbers; how measured):
Reflection (what I learned / would change):
Spring-era link (which pattern from this course describes the same idea):
Questions this story can answer:
```

**An illustrative example (invented, only to show the shape; do not use it, write yours):**

> **Data correctness: duplicate confirmations.** *Situation:* a downstream system received some settlement confirmations twice after a broker restart, which created duplicate postings and a manual clean-up of about 40 items a week. *Task:* I owned the message handling component. *Action:* I first measured it: the duplicates came only after restarts, which pointed at redelivery of unacknowledged messages. Instead of asking the broker for "exactly once" (not available end to end), I made the handler idempotent: a processed-message table with a primary key on the message id, written in the same transaction as the posting. I rejected a "check then insert" in code because two threads could race. I added a daily count query to detect any remaining duplicate. *Result:* duplicates went from about 40 a week to zero over the next quarter; the clean-up effort disappeared. *Reflection:* I would have added the daily check first, so we could have shown the baseline earlier. *Link:* the idempotent consumer of lesson 09.

Notice the shape: a measured cause, a decision with a rejected alternative, a number, a reflection, and a link to a pattern. That is what to build with your real material.

### Bridge sentences: from your world to Spring

You have 15+ years of enterprise experience. Use it as a **bridge**, never as a way to avoid the new vocabulary:

| You say (then translate) | Spring-era vocabulary |
|---|---|
| "Session beans with container-managed transactions" | "...which is `@Transactional` on a Spring bean, with the same propagation rules" |
| "JNDI lookups and the EJB container" | "...replaced by dependency injection: the container hands the dependency to the constructor" |
| "Message-driven beans on a queue" | "...a `@KafkaListener` or `@JmsListener`: a listener container that calls my method, with the same need for idempotency" |
| "Hibernate with the session per request" | "...Spring Data JPA over Hibernate; the persistence context, lazy loading and N+1 are the same traps" |
| "Oracle with partitioned tables" | "...the same thinking with PostgreSQL, plus Flyway to version the schema" |
| "Nightly batch with restart points" | "...a restartable saga or a Spring Batch job; and for near real time, events through an outbox" |
| "Elasticsearch for searching" | "...a CQRS read model: a query-optimised copy fed by events" |
| "SWIFT message duplicates and repair queues" | "...at-least-once delivery, idempotency keys, dead-letter topics" |
| "Application server clustering" | "...stateless instances behind a Service, replaced by Kubernetes during a rolling update" |

### Troubleshooting out loud: detect, isolate, fix, prevent

Scenario questions ("the service is slow; what do you do?") test method. Always answer in four moves:

1. **Detect and scope:** what exactly is wrong, since when, for whom? Check dashboards and recent changes (a deployment, a config change).
2. **Isolate:** narrow it down with the evidence: logs by correlation id, metrics, traces, thread dump, `kubectl describe`. Form a hypothesis and test it.
3. **Fix:** mitigate first (rollback, scale, disable a feature), then fix the cause.
4. **Prevent:** a test, an alert, a runbook, a design change. Say it; this is the architect's part.

Eight scenarios to rehearse (answers sketched; say yours aloud):

| Scenario | Likely path and where it is covered |
|---|---|
| **The service responds slowly under load; the database CPU is high** | Check slow queries, N+1 (lesson 04), missing indexes, connection pool wait; add the right fetch plan and an index; measure; alert on p95 |
| **Pods restart every few minutes** | `kubectl describe` for `OOMKilled` or probe failures; compare heap with the memory limit (lesson 10); a database check in liveness; a slow start without a startup probe |
| **Duplicate settlements appeared** | Find which step is not idempotent: relay duplicates, consumer redelivery, retry to the CSD (lessons 07-09); add an idempotency key and a unique constraint; reconciliation to detect |
| **Kafka consumer lag keeps growing** | Is the handler slow, stuck on a poison record, or fewer consumers than partitions? Rebalance storms? (lesson 09); scale, fix the handler, DLT |
| **`401` in production but not in test** | Token expiry, clock skew, wrong issuer or secret, the prod profile's JWT secret (lessons 05 and 10); check the `WWW-Authenticate` header and the token claims |
| **Errors for a few seconds at every deployment** | Readiness, preStop delay, graceful shutdown, `maxUnavailable` (lesson 10) |
| **A retry storm brings down a dependency** | Retries without backoff and jitter, no circuit breaker, no bulkhead (lesson 07); add them, cap attempts, retry only technical errors |
| **A new column breaks the old version during rollout** | Not backward compatible (lesson 08): expand and contract, deploy in two steps |

### The mock interview format

You will run it alone, in front of a phone camera or an audio recorder, or with a friend. The script, with time boxes:

| Round | Time | What happens | Preparation |
|---|---|---|---|
| 0. Introduction | 2 min | "Tell me about yourself" | Your 90-second pitch |
| 1. Rapid fire | 15 min | Ten random questions from `drill.py`, 60-90 seconds each | The quizzes |
| 2. Troubleshooting | 10 min | Two scenarios from the table above, chosen by the dice | The method above |
| 3. System design | 30 min | One prompt, the eight-step method of lesson 11 | A blank sheet |
| 4. Behavioural | 15 min | Three questions; you choose the story from your bank | Six stories |
| 5. Your questions | 5 min | Ask the interviewer | Your list |
| 6. Debrief | 10 min | Score yourself, list weaknesses | The rubric |

## 4. Setup steps

1. Open a PowerShell terminal in `springboot-2day`.
2. You need: a **timer**, a way to **record** (phone voice memo is enough), a blank sheet for the system design, and the story template above (print or copy it six times).
3. Check the drill tool runs (Python 3 is already installed for the lesson checkers):

```powershell
python tools\drill.py --list
```

Expected (verified; the totals grow if you add questions):

```
00-big-picture                      10 questions
01-spring-core                      10 questions
02-boot-internals                   10 questions
03-rest-validation                  10 questions
04-data-jpa                         10 questions
05-security-jwt                     10 questions
06-testing                          10 questions
07-microservices-resilience         10 questions
08-saga-outbox-cqrs                 12 questions
09-kafka                            10 questions
10-containers-kubernetes            12 questions
11-system-design-settlement         12 questions
12-mock-interview                   12 questions
total                               138
```

### Commands used in this lesson

| Command | Syntax | Why here | When you use it later |
|---|---|---|---|
| `python tools\drill.py` | `python tools\drill.py [-n N] [--lesson 08 09] [--answers] [--seed S] [--list]` | Ask random questions from the quizzes of all lessons. `-n` how many; `--lesson` restricts to lesson folders by prefix; `--answers` shows the model answer after you press Enter; `--seed` repeats the same selection; `--list` counts questions | Every revision session, the night before the interview |
| `python -I` | `python -I tools\drill.py ...` | `-I` (isolated mode) ignores environment variables and the current folder when importing: a safe habit for scripts you did not write yourself | Running downloaded scripts |

### Files to change or add, in order

You write **notes**, not code. Create a folder `interview-notes` (outside the course folder or inside, as you like) with:

| # | File | Content |
|---|---|---|
| 1 | `pitch.md` | Your 90-second introduction |
| 2 | `stories.md` | The six stories in the template |
| 3 | `questions-to-ask.md` | Eight questions for the interviewer |
| 4 | `scores.md` | One scoring table per mock run |
| 5 | `weak-topics.md` | Topics to re-read tonight, with the lesson number |

## 5. Code walkthrough

There is no code. Work through the steps in order, and keep the timer running.

### Step 1. The 90-second introduction (round 0)

**Structure: present, past, why this role.** About 200 words.

| Part | Content | Seconds |
|---|---|---|
| **Present** | Your role and the kind of systems: financial settlement, messaging, reconciliation; your scope (design, teams, platforms) | 25 |
| **Past** | The path that matters: Java and EJB-era enterprise systems, Hibernate and Oracle, batch and ETL, Elasticsearch, SWIFT and T2S messaging; **one achievement with a number** | 40 |
| **Now and next** | "Recently I have rebuilt the key patterns on Spring Boot 3: transactional outbox, saga with compensation, Kafka with idempotent consumers, resilience, containers and Kubernetes" and why this role is the next step | 25 |

**Template to complete:**

> "I am a [title] with [N] years in [domain]. Today I [scope]. I started in [Java/EJB-era systems], worked on [Hibernate and Oracle platforms, batch and ETL, search with Elasticsearch, and SWIFT and T2S messaging], and [one achievement with a number]. I have recently rebuilt the core microservice patterns on Spring Boot to stay current: [outbox, saga, Kafka, resilience, containers]. I am looking for [this role] because [reason that is specific to them]."

Say it three times aloud with the timer. Cut it to fit 90 seconds.

### Step 2. Rapid fire (round 1, 15 minutes)

```powershell
python -I tools\drill.py -n 10
```

Each question prints like this (example with a fixed selection so you can compare; yours will be different):

```powershell
python -I tools\drill.py -n 3 --seed 1 --lesson 01 07 10
```

Expected (verified):

```
[1] (01-spring-core, question 9)
    What does the `service class = ...` line in our demo output prove?

[2] (01-spring-core, question 5)
    At which lifecycle phase is the AOP proxy created?

[3] (07-microservices-resilience, question 7)
    In what order does Resilience4j nest the annotations, and why does the fallback belong on the outermost one?
```

**Procedure.** For each question: start the timer, **answer aloud** in the four-move shape, stop at 90 seconds. Then run the same selection with `--answers` and compare; write a tick or a cross in `scores.md`. Any cross goes to `weak-topics.md` with the lesson number.

```powershell
python -I tools\drill.py -n 10 --answers
```

(With `--answers` the tool pauses after each question so you can speak first, then press Enter to see the model answer.)

### Step 3. Troubleshooting (round 2, 10 minutes)

Roll a die or pick two scenarios from the table in section 3. For each: **four moves** (detect, isolate, fix, prevent) in two to three minutes, aloud. Then check against the lesson named in the table. The typical weakness is skipping "prevent".

### Step 4. System design (round 3, 30 minutes)

Pick **one** prompt. Run the eight steps of lesson 11 with the clock visible: clarify (5), estimate (4), interfaces (4), data (5), architecture (8), deep dives (12), failures (5), trade-offs (2) is 45 minutes in the real interview; in this mock, compress each step by a third.

| Prompt | Hints for your deep dives |
|---|---|
| **A. Design the end-of-day reconciliation platform** between our positions and the CSD's statements | Ingest (file and API formats), normalise, matching rules and tolerances, break classification and workflow, idempotent re-runs, audit, volumes, late statements |
| **B. Design a notification service that tells clients about settlement status changes** | Events in, fan-out, delivery channels (webhook, e-mail, SWIFT message), retry and DLT, per-client ordering, idempotency, rate limits, preferences, replay |
| **C. Design a trade-reporting API with search for operations (millions of rows, filters, export)** | CQRS read model, Elasticsearch or database indexes, pagination (keyset), consistency lag, access control per client, export as an asynchronous job, retention |
| **D. (Hard) Design the settlement platform of lesson 11 for two regions** | Active-passive versus active-active, replication, RPO and RTO, Kafka mirroring, idempotency across regions, split brain, data residency |

**Self-check afterwards:** did you ask questions first, estimate, name a failure for each box, and state what you rejected?

### Step 5. Behavioural (round 4, 15 minutes)

Choose three questions at random from this list. Tell the story in under 2 minutes each, then spend 1 minute on "what would you do differently".

1. Tell me about the hardest production incident you handled.
2. Describe a technical decision you made that you later regretted.
3. Tell me about a time you disagreed with an architect or a manager.
4. Describe a time you improved performance or reliability measurably.
5. Tell me about leading a migration or a modernisation.
6. How have you mentored or raised the level of a team?
7. Describe a time you had to deliver under a tight deadline and what you cut.
8. Tell me about a time you were wrong and how you found out.

### Step 6. Your questions for them (round 5)

Good questions show architect thinking and help **you** judge the job. Prepare eight; ask three or four.

1. What does a typical architecture decision look like here, and who makes it?
2. What is the biggest technical debt you want this role to reduce?
3. How do teams own and operate their services (on-call, release frequency, platform support)?
4. How do you handle reconciliation and data quality across systems?
5. What is the current deployment platform and what is the migration path?
6. How is success measured for this role in the first six months?
7. What is the hardest incident of the last year and what changed after it?
8. How do architects work with the platform and security teams?

Avoid asking about salary, benefits or leave here; that belongs with the recruiter.

### Step 7. Score yourself (round 6)

Score each dimension from **1 (weak) to 4 (strong)** after each mock:

| Dimension | 1 | 4 |
|---|---|---|
| **Structure** | Rambling, no plan | Headline, reason, trade-off, failure; plan stated |
| **Technical depth** | Names only | Three levels down, correct internals |
| **Trade-offs** | One option, no alternatives | Alternatives named and rejected with a reason |
| **Failure thinking** | Happy path only | Failure modes, detection and recovery without being asked |
| **Numbers** | None | Estimates used to justify decisions |
| **Honesty** | Bluffed, or "I do not know" with nothing more | Clear limits, reasoning, how to verify |
| **Communication** | Too long, filler words | On time, plain words, checks with the interviewer |
| **Stories** | No result, "we" everywhere | STAR+ with a number and a reflection |

Below 3 on any dimension: that is your revision list for tonight. Typical mapping: depth gaps go back to the lesson's sections 3, 6, 7; failure thinking to lessons 07-09; numbers and structure to lesson 11.

### Step 8. Tonight and tomorrow

**Tonight (after the mock):**
1. Re-read the **Recap** of every lesson (12 short sections).
2. Read `99-cheatsheet-and-interview-qa.md` once, slowly.
3. Re-do the questions you marked with a cross, aloud.
4. Re-say your pitch and your six stories once each.
5. Sleep. A tired candidate answers worse than an unprepared one.

**Interview day checklist:**
- Water, a pen, a blank notepad (for the design round, draw while you talk), a charged laptop if the interview is remote, a quiet room, a camera at eye level, a test call 10 minutes before.
- Read the company's engineering blog, product and the job description once; prepare two questions that use what you read.
- Start each answer with the headline. Stop at 90 seconds. Ask "shall I go deeper?".
- If you freeze: say "let me think for a moment", write the four-move shape on paper, and start with what you do know.

## 6. Common mistakes

| Mistake | How the interviewer sees it | Fix |
|---|---|---|
| Five-minute answers | Cannot prioritise, cannot communicate | Headline first, 90 seconds, then offer to go deeper |
| Reciting definitions | Memorised, not understood | Add a trade-off and a failure mode every time |
| "We did..." in every story | Cannot tell your contribution | "I decided...", "I implemented...", and credit the team once |
| A story without a result | No impact | A number, even an estimate marked as estimate |
| Inventing experience | Caught by the second follow-up | Only true stories; honest limits |
| Criticising a former employer | Poor judgement | Describe the constraints neutrally and what you learned |
| Over-claiming a technology | Three levels down you fall | List only what you can defend; show what you built (the reference project) |
| No questions at the end | Little curiosity | Prepare and ask three |
| Skipping the "prevent" step in troubleshooting | A fixer, not an architect | Always add the test, alert, runbook or design change |
| Not practising aloud | Fluent in your head, stuck in your mouth | Record yourself and listen |
| Memorising the cheat sheet the same night | No retention | Read the recaps, do spaced drills across days |
| Arguing with the interviewer's hint | Not collaborative | Take hints as help: "good point, that changes X" |

## 7. Production issues and design notes

For this lesson "production" is **interview day**: the things that go wrong live, and how to recover.

| Situation | Recover by |
|---|---|
| You do not know the answer | The three moves: closest knowledge, reason from first principles, how you would verify |
| You gave a wrong answer and notice later | Say so: "I want to correct something I said earlier: ..." A visible self-correction scores well |
| You freeze | Pause, say "let me structure this", write headline, reason, trade-off, failure mode, and fill them in |
| You ran out of time on the design | Say "I will summarise the rest": list the remaining deep dives and the one decision you would make for each |
| The interviewer disagrees with a choice | Ask for the constraint behind it, restate your choice under it, and be willing to change |
| A question about a technology you list but have used little | Say how much you have used it and how, then relate it to a pattern you know |
| The video or audio fails | Say so at once, offer to repeat, keep answers shorter |
| The interviewer is silent | Do not fill the silence with a worse answer; ask "does that answer your question, or shall I go deeper?" |
| Nerves | Slow down; a pause is not a mistake. Breathe; speak at a pace slower than feels natural |

**Design notes for your preparation.**
- **Spaced practice beats cramming.** Ten random questions a day for a week beats one long session.
- **Teach it.** Explaining the outbox pattern to a colleague, a rubber duck or a recorder shows quickly where you are vague.
- **Keep a "one-page" of your numbers** (the capacity example, the retry and breaker settings, the pod resources, the layer sizes) so you quote them correctly; recall of one verified number makes a statement credible.

## 8. Architect lens

- **What an architect interview actually asks:** not "do you know the API?" but "can you make a decision, communicate it, and be accountable for its consequences?" Every answer should show *judgement*.
- **Level signals.** A **senior engineer** answers correctly. A **lead** answers correctly and names the pitfalls. An **architect** also names the alternatives, the cost of each, the effect on teams and operations, and how the decision can be changed later.
- **Reversible versus irreversible decisions:** spend design effort in proportion to how hard the decision is to undo (a public event contract and a database schema are hard; a library choice inside one service is easy). Saying this shows maturity.
- **Communication is part of the design:** a diagram the room understands, a one-page decision record (context, options, decision, consequences), and the ability to explain to a non-technical sponsor why a reconciliation is worth its cost.
- **Credibility comes from specifics.** "I built an outbox with a relay that polls every two seconds and marks rows after the broker's acknowledgement" is stronger than "I know the outbox pattern". The reference project gives you a bank of specifics: use it.
- **Honest limits are a strength.** Saying what you did not build (the matching engine, real CSD connectivity, multi-region) shows that you can scope a claim.
- **Link to your world:** an interview is a design review with an unknown audience: state the problem, the options, the choice, the risks and the monitoring, then listen.

## 9. Self-check quiz

1. What are the four moves of a technical answer, and how long should the answer be?
2. What does STAR+ stand for, and what does the "+" add?
3. Why are "we" stories a problem, and what replaces them?
4. What do you do when you do not know the answer?
5. What are the four moves for a troubleshooting question, and which one is skipped most often?
6. Name three things you should be able to say about any technology on your CV.
7. How do you correct a mistake you notice after answering?
8. Give two good questions to ask the interviewer and say what each reveals.
9. Why prepare six stories rather than one per question?
10. What is the difference between a senior, a lead and an architect answer?
11. How should you prepare the night before?
12. What is the first thing to say when you start a system-design question?

<details><summary>Answers</summary>

1. Headline, reason, trade-off, failure mode. Sixty to ninety seconds, then offer to go deeper.
2. Situation, Task, Action, Result. The plus is the **reflection**: what you learned or would change, which shows self-awareness.
3. The interviewer cannot tell what you did. Say "I decided, I implemented", and credit the team once.
4. Say what you know that is closest, reason from first principles, and say how you would verify. Never invent.
5. Detect and scope, isolate, fix (mitigate first), prevent. "Prevent" is skipped most often.
6. What it is, how it works inside, how it fails (and what you built or used it for).
7. Say so plainly: "I want to correct something I said earlier". It is a positive signal.
8. For example "How do teams own and operate their services?" (operating model, on-call, platform) and "What is the hardest incident of the last year and what changed?" (learning culture, real problems).
9. One story answers several questions; six cover the main themes (incident, decision, data correctness, performance, change, people) and each is rehearsed deeply.
10. Senior: correct. Lead: correct plus pitfalls. Architect: also alternatives, costs, team and operational impact, and reversibility.
11. Read the recaps and the cheat sheet once, redo the missed questions aloud, say the pitch and the stories once, and sleep.
12. The plan, and then questions: "I will clarify requirements, estimate, then interfaces, data, architecture, deep dives, failures and trade-offs; first, a few questions."

</details>

## 10. Interview Q&A

These are the questions about **you**, with answer frameworks (write your own content into them).

**Q1. Tell me about yourself.**
The 90-second pitch: present, past, why this role (Step 1). End with something that invites a follow-up, for example the Spring-era patterns you rebuilt.

**Q2. You have a long EJB and Oracle background. Why should we trust you with Spring Boot and Kubernetes?**
"The concepts are the same ones I have used for years, with new names: transactions, dependency injection, messaging, idempotency, reconciliation. To prove it I rebuilt the patterns end to end: outbox, saga, Kafka with an idempotent consumer, resilience, containers and a Kubernetes deployment, with tests. I can show the code and I can tell you exactly what I have **not** run in production." (Then give one concrete example.)

**Q3. Why do you want to be an architect rather than stay hands-on?**
Frame it as **leverage**, not escape: you want to influence decisions that affect many teams and many years, while staying close enough to the code to keep your credibility (you still read the code, review the critical parts and prototype the risky ones).

**Q4. Tell me about a difficult decision and how you made it.**
Pick the design-decision story: the options, the criteria (correctness, cost, risk, time), who you consulted, what you chose, what you rejected and why, what happened. End with the reflection.

**Q5. How do you handle disagreement with a team or a manager?**
Seek the **constraint behind the position**, bring data (a measurement, a prototype, a failure scenario), propose a reversible experiment where possible, and commit to the decision once made. Give a real example.

**Q6. How do you decide between build, buy and reuse?**
Differentiation (does it make us better?), cost of ownership over years, integration risk, team skills, vendor lock-in and exit cost, regulatory fit. Build what is core, buy what is a commodity, and always keep an exit path.

**Q7. How do you keep your skills current?**
A concrete habit, not "I read blogs": for example "I rebuild a pattern in code whenever it appears in my work, so I know where it breaks; recently the outbox and the idempotent consumer on Spring Boot 3."

**Q8. Tell me about a failure.**
A real one with a real consequence, your part in it, how you found out, what you did in the first hour, and the **lasting change** (a test, an alert, a process). Never a disguised success ("I work too hard").

**Q9. How do you mentor and raise the level of a team?**
Design reviews with written decision records, pairing on the hard parts, templates and paved roads (a service skeleton with security, logging and health already wired), and clear standards for what "done" means (tests, runbook, dashboards).

**Q10. (Depth) What would you do in your first 90 days?**
Listen and learn (30 days: systems, people, incidents, the decision history), find and fix one visible pain with a small win (60 days), propose and agree a short roadmap with measurable outcomes (90 days). Avoid announcing a rewrite on day one.

**Q11. (Depth) How do you convince people who do not want to change?**
Make the cost of the status quo visible with data, run a small pilot with a team that wants it, document the result, make the new way the easy way (a template, tooling), and respect operational concerns (support, risk, training).

**Q12. What questions do you have for us?**
Use your prepared list (Step 6).

## 11. Recap

1. **Delivery is a skill.** Answer in four moves (headline, reason, trade-off, failure mode) in 60-90 seconds, then offer to go deeper.
2. **Six true stories** in STAR+ with numbers (incident, decision, data correctness, performance, change, people); use "I", end with a reflection.
3. **A 90-second pitch**: present, past, why this role; bridge your EJB, Oracle, batch and SWIFT experience to Spring-era vocabulary.
4. **Troubleshooting** is detect, isolate, fix, **prevent**.
5. **When you do not know:** closest knowledge, first-principles reasoning, how you would verify. Never bluff.
6. **Practise aloud and score yourself** with the rubric; `tools/drill.py` supplies random questions from every quiz. Mark the misses and re-read those lessons tonight.
7. **The night before:** recaps, the cheat sheet, the missed questions, the pitch and the stories, then sleep.
