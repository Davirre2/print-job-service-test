# Print/Render Service - Technical Interview

### Context

We run a rendering system. Clients submit a **render job** (a template id plus some parameters);
the job is processed **asynchronously**, and processing can occasionally fail transiently.
Clients poll for the job's status and fetch the result once it is done.

The set of available templates already exists (`RenderTemplate` / `GET /templates`) - you do not
need to build template management. Your job is to implement the render job lifecycle end to end,
**including how the service is packaged and run**.

### Prerequisites

- **JDK 25** installed locally (the Maven wrapper handles Maven itself, but you need a JDK to run
  `./mvnw` or your IDE).
- **Docker Desktop** (Mac/Windows) or **Docker Engine + the Compose plugin** (Linux), installed
  and running. Everything containerization-related in this exercise (the `Dockerfile` you're
  given, and the `docker-compose.yml` you write) is public/open-source - no account, license, or
  paid service is required. `docker compose version` should print a v2.x version.
- Ports **8080** (the app) and **5432** (Postgres) free on your machine, or be ready to remap them
  in your `docker-compose.yml` if something else is already using them.
- Git, to fork/push your solution.

### Functional Requirements

- **Submit a job**: `POST /jobs`
  - Body: `{ "templateId": "<uuid>", "parameters": { "any": "key-value data" } }`
  - Must return immediately (do not block the HTTP response on the actual rendering work).
  - Reject with `400` if `templateId` does not match an existing template.
  - On success, return `201` with the created job (id, status `QUEUED`, timestamps).

- **Process a job asynchronously**: once queued, a job must move through
  `QUEUED -> PROCESSING -> DONE` or `QUEUED -> PROCESSING -> FAILED`, driven by a background
  worker - not by an incoming HTTP request. "Rendering" can be simulated (e.g. a short delay);
  it does not need to produce a real document.
  - Some renders fail transiently. A failed attempt should be retried a bounded number of times
    before the job is marked `FAILED` with an error reason recorded.

- **Get job status**: `GET /jobs/{id}` - current status, attempt count, error message if failed,
  and whether a result is available.

- **List jobs**: `GET /jobs` - optionally filterable by status, e.g. `GET /jobs?status=FAILED`.

- **Fetch a result**: `GET /jobs/{id}/result` - returns the rendered output once the job is
  `DONE`. Decide yourself what should happen if it's called before the job finishes, or if the
  job failed.

### Required

- Your solution - code, commit messages, and README - must be in English.
- Java 25.
- This repo contains the existing project skeleton (template lookup, project setup, and a starter
  `Job` entity with the fields implied by the API contract above); fork or create a public
  repository with your solution. The `Job` entity does not model retry scheduling/backoff - that's
  part of what you're designing.
- You decide the scope of automated tests - we expect to see some.
  `RenderTemplateResourceTest` shows the MockMvc setup used in this repo, if that's a useful
  starting point.
- **The service must be containerized and runnable via Docker Compose alongside an actual
  PostgreSQL server** - i.e. a `postgres` Docker image running as its own container, not H2 or
  any other in-memory/embedded database. A `Dockerfile` for this service is provided; you need to
  write the `docker-compose.yml` that wires this app together with that `postgres` service (the
  app already reads its DB connection from `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` /
  `DB_PASSWORD`, see `application.properties`). H2 is fine for your own test suite - it must not
  be what the running application connects to.
- The service must expose:
  - A liveness endpoint.
  - A readiness endpoint that reflects more than "the HTTP server is up" - think about what else
    a caller would want to know before considering this service ready to take traffic.
  - Some form of basic metrics (job counts by status is enough; format is up to you).
  - Paths for all three are up to you - just document them in your README so we know where to
    look.
- **Add a short "Design Decisions" section to your README** covering the points listed below
  under "A few things we deliberately left open." A few sentences per point is enough - this is
  the starting point for the design discussion, not a design doc.
- **Once the code is complete, reply to your hiring contact with a link to your repository.**

### Design Decisions

**Queue / worker.** `JobWorker` is a `@Scheduled` poller
that asks the database for the oldest `QUEUED` job via
`SELECT ... FOR UPDATE` (`JobRepository.findNextJobForUpdate`), so a single row lock is what
prevents two callers from claiming the same job. On top of that, a distributed lock
(`WorkerLockService`) restricts *which instance* even attempts to claim work: in production it
uses a PostgreSQL transaction-scoped advisory lock (`pg_try_advisory_xact_lock`), released
automatically when the claiming transaction commits. Claiming a job is its own short transaction that
commits before rendering starts, so the simulated render never holds a DB lock open.
`POST /jobs` only ever writes a row and returns. The worker discovers new work purely by polling,
so submission and processing are fully decoupled.

**Retry policy.** `Job.attempts` is incremented each time a job is claimed, before rendering is
attempted. `JobService.MAX_ATTEMPTS = 3` on a transient render failure the job goes back to
`QUEUED` as long as `attempts < MAX_ATTEMPTS`. Once the 3rd attempt also fails it moves to `FAILED`
with the last error message recorded. There's no backoff delay between attempts beyond the
worker's own poll interval. "render.transient-failure-probability is configurable, 
which is how the retry path was exercised during testing"

**Readiness.** `/health/readiness` checks each dependency independently (currently: database
connectivity, plus a storage-accessibility placeholder) and returns `200` with `ready: true` only if all of them are
`UP`; if any is `DOWN` it returns `503` with a `details` map showing which one failed. Liveness
(`/health/liveness`) deliberately does *not* touch the database - it only confirms the JVM/HTTP
layer is responding, so a temporarily slow database doesn't get a healthy process killed by an
orchestrator's liveness probe when a readiness failure would be the correct signal instead.


### Optional (not required to complete the exercise)

The optional parts of the exercise have been completed and can be found in the project.

- Demonstrate that running two instances of your app against the same database does not cause a
  job to be processed twice.
- A Kubernetes `Deployment`/`Service` manifest for this app (it does not need to be applied to a
  real cluster - we're interested in the manifest itself, e.g. how you wire up probes).

### Added endpoints

Job lifecycle:

- `GET /jobs/{id}`
- `GET /jobs?status=<STATUS>`: `status` is optional
  (`QUEUED` / `PROCESSING` / `DONE` / `FAILED`); omit it to list every job. `400` if `status` isn't
  one of those four values.
- `GET /jobs/{id}/result`: the rendered output, once the job is `DONE`. `200` with the result
  body; `404` if the id doesn't exist; `409` if the job exists but hasn't finished yet or ended in
  `FAILED`.

Health:

- `GET /health/liveness`: `200` with `{"status": "UP"}` whenever the process is up. Doesn't check
  any dependency on purpose (see Design Decisions).
- `GET /health/readiness`: `200` with `{"status": "UP", "ready": true, "details": {...}}` when
  every checked dependency is healthy; `503` with `ready: false` and the failing check(s) named in
  `details` otherwise.
- For readiness, storage is hardcoded to be `"UP"`.
If the `"DOWN"` outcome is wanted, the variables must be modified.

Metrics (`/objects_count`):
- `GET /objects_count`: total jobs and total templates.
- `GET /objects_count/jobs`: total jobs only.
- `GET /objects_count/jobs/status/{status}`: job count for one status (`400` for an invalid
  status).
- `GET /objects_count/templates`: total templates only.

### How to run

Building
```shell
$ ./mvnw compile
```

Test
```shell
$ ./mvnw test
```

Start the application (once you've written `docker-compose.yml`)
```shell
$ docker compose up --build
```

Listing available templates
```shell
$ curl localhost:8080/templates
```
