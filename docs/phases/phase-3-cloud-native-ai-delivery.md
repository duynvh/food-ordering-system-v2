# Phase 3 — Cloud-Native AI Delivery

> **Combines:** Cloud + AI
> **Window:** ≈ late Feb → mid Jun 2027 (~17 weeks, ~83 hrs)
> **Mission:** Ship an *intelligent system to production*. The AI capstone and the cloud deployment land together — you deploy a RAG-backed feature to AWS, observed by the [Phase 1](phase-1-observable-and-fast.md) stack and secured by the [Phase 2](phase-2-resilient-and-secure-edge.md) edge.

Part of the [one-year senior-Java plan](../plan.md). This is the capstone: everything built so far gets containerized, deployed, and put behind a real, intelligent feature.

---

## Why these two together

By Phase 3 the system is observable, fast, resilient, and secure — but it only runs on your laptop. Cloud delivery and AI combine because:

- The **AI capstone** is the user-visible reason to deploy — it gives the cloud work a real payload instead of a hollow "hello world on EKS."
- The **cloud platform** is where AI features actually live (managed model providers, vector stores, autoscaling for inference cost).

Deploying a RAG-backed feature to AWS is one story: *"I shipped an intelligent system to production, observed and secured."*

## Starting point in the codebase

| Concern | Current state | Where |
|---|---|---|
| App containerization | None (only infra is dockerized) | no app Dockerfiles |
| Kubernetes / Helm | None | — |
| Cloud / IaC | None | — |
| CI/CD | CI builds only (Java 25) | GitHub Actions |
| AI | Spring AI structured-output note interpreter | `order-service/order-ai`, `AIOrderNoteInterpreter.java` |
| Vector store | None (Postgres present, no pgvector) | — |

## Sub-milestones & week-by-week

### Sub-milestone A — Cloud delivery (weeks 1–9, ~45 hrs)
- **W1** — **Dockerfiles** per `*-container` service (layered build or Jib); small, reproducible images. Verify locally.
- **W2** — Local **Kubernetes** (kind/minikube): Deployments, Services, ConfigMaps; bring up the whole system on k8s, not just compose.
- **W3** — Wrap it in a **Helm chart** (values per environment); move Phase-2 secrets to **k8s Secrets**.
- **W4** — Port the Phase-1 observability stack and Phase-2 gateway/Keycloak into the k8s setup; confirm dashboards + auth still work in-cluster.
- **W5** — **AWS account + free-tier guardrails**; budget alerts; IAM basics. Decide **EKS vs ECS/Fargate** (Fargate is cheaper/simpler for this scale — document the call).
- **W6** — **Terraform** for the base infra (VPC, cluster/Fargate, **RDS** Postgres, **ElastiCache** Redis, **MSK** or self-hosted Kafka).
- **W7** — Push images to **ECR**; deploy the app to AWS via Helm/Terraform; smoke-test the order flow in the cloud.
- **W8** — **GitHub Actions CI/CD**: build → test → push to ECR → deploy. One-command-ish pipeline.
- **W9** — Verify a full `terraform apply` → working system, and a **`terraform destroy`** teardown. Document cost discipline.

**Exit check:** `terraform apply` stands the system up on AWS; the Phase-1 dashboards work against it; `terraform destroy` cleanly tears it down.

### Sub-milestone B — AI capstone (weeks 10–17, ~38 hrs)
- **W10** — Add **pgvector** to the Postgres/RDS schema; wire Spring AI's vector store abstraction.
- **W11** — **Embeddings pipeline**: ingest restaurant/menu data into the vector store; keep it updated on menu changes.
- **W12** — **RAG** retrieval: a feature that answers menu/restaurant questions grounded in real data (extends the `order-ai` module).
- **W13** — **Tool/function calling**: let the model query real order/menu state through safe, typed tools.
- **W14** — **Guardrails**: build on the existing injection-hardened system prompt; validate tool inputs/outputs; cap scope.
- **W15** — **Evaluation harness**: a repeatable eval set scoring retrieval relevance + answer quality (not just "looks good").
- **W16** — **Token-usage observability** on Grafana (ties back to Phase 1); optional **AWS Bedrock** as an alternate model provider.
- **W17** — Deploy the RAG feature to AWS end-to-end; final write-up + year-end retrospective.

## Depth marker (definition of done)

A reproducible **`terraform apply`** that stands up the system on AWS **running the RAG-backed feature end-to-end**, observable via your Phase-1 stack and behind your Phase-2 secured gateway — plus a clean **teardown**. The AI piece must include an **eval suite**, not just a working prompt.

## Learning resources (just-in-time)

- AWS docs (EKS or ECS/Fargate, ECR, RDS, ElastiCache, MSK)
- A Terraform + AWS tutorial
- GitHub Actions CI/CD docs
- Spring AI docs (vector store, RAG, function calling) — via context7
- A RAG primer + an LLM-evaluation overview

## Deliverables

- [ ] Dockerfiles for all services + Helm chart
- [ ] Local k8s manifests; full stack runs in-cluster
- [ ] Terraform IaC for AWS (cluster, RDS, ElastiCache, Kafka/MSK)
- [ ] GitHub Actions pipeline: build → ECR → deploy
- [ ] pgvector + embeddings + RAG feature in `order-ai`
- [ ] Function calling + guardrails + **eval harness**
- [ ] Token-usage dashboard; optional Bedrock provider
- [ ] **One merged PR** + **~700-word write-up** + year-end retrospective

## Phase-specific risks

- **Cloud cost is the #1 risk** → do *everything* on local k8s first; touch AWS only to prove deployment; use free tier + smallest instances; **tear down between sessions**; set a budget alert in W5.
- **EKS complexity** → prefer ECS/Fargate if EKS eats too much time; the learning goal is "I can deploy + IaC + CI/CD to AWS," not "I mastered Kubernetes internals."
- **AI scope creep** → one solid RAG feature with an eval suite beats three half-built AI gimmicks; the eval harness is the senior signal, not the model.
- **Managed-service substitution** → if MSK/RDS/ElastiCache cost too much, run them self-hosted in-cluster and note the tradeoff; the deployment story still holds.
