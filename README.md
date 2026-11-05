# Feed Standardization Service

A Spring Boot microservice that acts as the standardization layer of a sports betting feed
pipeline. It receives `ODDS_CHANGE` and `BET_SETTLEMENT` messages from two feed providers
(Alpha and Beta), each in its own proprietary format, converts them into one standard internal
format and sends them to a (mocked) message queue.

## Contents

- [Quick start](#quick-start)
- [Requirements](#requirements)
- [Run](#run)
- [Endpoints](#endpoints)
- [Standard message format](#standard-message-format)
- [Error handling](#error-handling)
- [Tests](#tests)
- [Continuous integration](#continuous-integration)
- [Project structure](#project-structure)
- [Design decisions and assumptions](#design-decisions-and-assumptions)
- [Possible improvements](#possible-improvements)

## Quick start

```bash
git clone https://github.com/iodicegabriele/Assignment_Gabriele_Iodice.git
cd Assignment_Gabriele_Iodice
./gradlew bootRun
```

Then, in another terminal:

```bash
curl -i -X POST localhost:8080/provider-alpha/feed -H "Content-Type: application/json" \
  -d '{"msg_type":"odds_update","event_id":"ev123","values":{"1":2.0,"X":3.1,"2":3.8}}'
```

The response is `204 No Content` and the standardized message appears in the service log.

## Requirements

- **Java 27** to compile and run the service. If it is not installed, Gradle downloads it
  automatically on the first build (Foojay toolchain resolver). To start, Gradle itself needs
  any JDK 17 or later on the `PATH` or in `JAVA_HOME`.
- Nothing else: Gradle comes with the wrapper (`gradlew` / `gradlew.bat`).
- Optional: Node.js, to run the Postman collection from the command line with Newman.

## Run

```bash
./gradlew bootRun
```

On Windows use `gradlew.bat bootRun`. The service listens on `http://localhost:8080`.
To use another port, add `--args='--server.port=9090'`.

Or build and run the executable jar:

```bash
./gradlew bootJar
java -jar build/libs/Assignment_Gabriele_Iodice-0.0.1-SNAPSHOT.jar
```

## Endpoints

There is one endpoint per provider. Both accept `POST` with `Content-Type: application/json`,
one message (odds change or settlement) per request.

| Provider | Endpoint                    |
|----------|-----------------------------|
| Alpha    | `POST /provider-alpha/feed` |
| Beta     | `POST /provider-beta/feed`  |

### Provider Alpha

```bash
# ODDS_CHANGE
curl -i -X POST localhost:8080/provider-alpha/feed -H "Content-Type: application/json" \
  -d '{"msg_type":"odds_update","event_id":"ev123","values":{"1":2.0,"X":3.1,"2":3.8}}'

# BET_SETTLEMENT
curl -i -X POST localhost:8080/provider-alpha/feed -H "Content-Type: application/json" \
  -d '{"msg_type":"settlement","event_id":"ev123","outcome":"1"}'
```

| Field      | Required          | Values                                         |
|------------|-------------------|------------------------------------------------|
| `msg_type` | always            | `odds_update` or `settlement`                  |
| `event_id` | always            | any non blank string                           |
| `values`   | for `odds_update` | exactly the keys `1`, `X`, `2`, each odd > 1.0 |
| `outcome`  | for `settlement`  | one of `1`, `X`, `2`                           |

### Provider Beta

```bash
# ODDS_CHANGE
curl -i -X POST localhost:8080/provider-beta/feed -H "Content-Type: application/json" \
  -d '{"type":"ODDS","event_id":"ev456","odds":{"home":1.95,"draw":3.2,"away":4.0}}'

# BET_SETTLEMENT
curl -i -X POST localhost:8080/provider-beta/feed -H "Content-Type: application/json" \
  -d '{"type":"SETTLEMENT","event_id":"ev456","result":"away"}'
```

| Field      | Required         | Values                                                  |
|------------|------------------|---------------------------------------------------------|
| `type`     | always           | `ODDS` or `SETTLEMENT`                                  |
| `event_id` | always           | any non blank string                                    |
| `odds`     | for `ODDS`       | exactly the keys `home`, `draw`, `away`, each odd > 1.0 |
| `result`   | for `SETTLEMENT` | one of `home`, `draw`, `away`                           |

## Standard message format

Both providers are normalized to the same two messages. The 1X2 outcomes are represented by a
shared `Outcome` enum, so the rest of the platform never sees provider specific keys:

| Outcome | Alpha | Beta   | Meaning       |
|---------|-------|--------|---------------|
| `HOME`  | `1`   | `home` | Home team win |
| `DRAW`  | `X`   | `draw` | Draw          |
| `AWAY`  | `2`   | `away` | Away team win |

| Standard message       | Type           | Fields                                         |
|------------------------|----------------|------------------------------------------------|
| `OddsChangeMessage`    | ODDS_CHANGE    | `id`: event id, `odds`: `Map<Outcome, Double>` |
| `BetSettlementMessage` | BET_SETTLEMENT | `id`: event id, `result`: `Outcome`            |

For example, the Alpha message
`{"msg_type":"odds_update","event_id":"ev123","values":{"1":2.0,"X":3.1,"2":3.8}}`
and the Beta message
`{"type":"ODDS","event_id":"ev123","odds":{"home":2.0,"draw":3.1,"away":3.8}}`
both become:

```
OddsChangeMessage[id=ev123, odds={HOME=2.0, DRAW=3.1, AWAY=3.8}]
```

### Message queue

As allowed by the assignment, the queue is mocked: the standardized message is written to the
application log, for example:

```
INFO ... c.g.i.controllers.AlphaFeedController    :
**************************
OddsChangeMessage[id=ev123, odds={HOME=2.0, DRAW=3.1, AWAY=3.8}]
**************************
```

## Error handling

| Status | When                                                                     | Body                                    |
|--------|--------------------------------------------------------------------------|-----------------------------------------|
| `204`  | The message was converted and sent to the queue                          | none                                    |
| `400`  | Missing or blank mandatory field, invalid outcome                        | JSON: `{"<field>": "<message>"}`        |
| `400`  | Unknown message type, missing or invalid odds, missing outcome, bad JSON | plain text message                      |
| `405`  | Method other than `POST`                                                 | plain text message                      |
| `415`  | Content type other than JSON                                             | plain text message                      |
| `500`  | Unexpected error (logged with its stack trace)                           | `Generic Error please contact support.` |

Examples:

```
POST /provider-alpha/feed  {"msg_type":"settlement","event_id":"ev123","outcome":"Y"}
400  {"outcome":"Possible outcomes can only be 1|X|2"}

POST /provider-beta/feed   {"type":"ODDS","event_id":"ev456"}
400  "odds" MUST be in the format {"home": 1.95, "draw": 3.2, "away": 4.0}

POST /provider-beta/feed   {"type":"FOO","event_id":"ev456"}
400  Beta Feed message type not supported: FOO
```

## Tests

```bash
./gradlew test
```

The HTML report is written to `build/reports/tests/test/index.html`.

| Test class                                              | Scope                                                                |
|---------------------------------------------------------|----------------------------------------------------------------------|
| `AlphaConverterServiceTest`, `BetaConverterServiceTest` | Unit tests of the conversion: every outcome, invalid odds and types |
| `AlphaFeedControllerTest`, `BetaFeedControllerTest`     | `@WebMvcTest` with a mocked converter: statuses, validation, errors |
| `FeedEndToEndTest`                                      | Whole application with the real converters and the sample messages   |

### Postman API tests

[`AssessmentGabrieleIodice.postman_collection.json`](AssessmentGabrieleIodice.postman_collection.json)
contains the sample messages of the assignment and many invalid inputs (wrong outcomes, missing
fields, unknown message types, missing or invalid odds, malformed JSON, wrong method or content
type), grouped in folders. Every request has a post-response script that checks the status code
and the exact error message.

Import it in Postman, or run it with Newman against a running service:

```bash
npx newman run AssessmentGabrieleIodice.postman_collection.json --env-var baseUrl=http://localhost:8080
```

The `baseUrl` collection variable defaults to `http://localhost:8080`.

## Continuous integration

| Workflow                                   | Trigger                                | What it does                                                          |
|--------------------------------------------|----------------------------------------|-----------------------------------------------------------------------|
| `.github/workflows/ci.yml`                 | every push and pull request            | Runs the unit tests, starts the service, runs the Postman collection |
| `.github/workflows/claude-code-review.yml` | every push to a branch with an open PR | Claude reviews the change and comments on the pull request            |

The code review needs one of these repository secrets (Settings → Secrets and variables →
Actions); without them it is skipped with a warning:

- `ANTHROPIC_API_KEY`: an API key from the Anthropic Console, or
- `CLAUDE_CODE_OAUTH_TOKEN`: a Claude Pro/Max subscription token, created with `claude setup-token`.

## Project structure

```
src/main/java/com/gabriele/iodice/
├── controllers/            AlphaFeedController, BetaFeedController: one endpoint per provider
│   └── advices/            FeedControllerAdvice: maps exceptions to HTTP statuses
├── dtos/                   AlphaFeedDTO, BetaFeedDTO: provider formats and their validation
├── entities/               StandardMessage, Outcome
│   └── impl/               OddsChangeMessage, BetSettlementMessage: the standard messages
└── services/               ConverterService<T>: provider format -> standard message
    └── impl/               AlphaConverterService, BetaConverterService, OutcomeMapper
```

## Design decisions and assumptions

- **One converter per provider behind a generic interface.** `ConverterService<T>` keeps each
  provider's format isolated. Adding a provider means adding a DTO, a converter and a controller,
  without touching the existing ones. The shared mapping rules live in `OutcomeMapper`, so all
  providers are validated the same way.
- **Validation in two layers.** Bean Validation on the DTOs checks what does not depend on the
  message type (mandatory fields, allowed outcome values). The converters check what does
  (odds only for odds messages, outcome only for settlements).
- **`Outcome` enum instead of strings or indexes.** The standard messages cannot carry an invalid
  outcome, and consumers do not need to know any provider's keys.
- **`204 No Content`.** The service only forwards messages and has nothing to return, so the
  same status is used for both providers.
- **Odds must be greater than 1.0.** Decimal odds are always greater than 1, so a lower value is
  treated as an invalid message rather than forwarded.
- **Exactly the three 1X2 keys.** Missing or additional keys are rejected, since the assignment
  only covers the 1X2 market.

## Possible improvements

- Replace the logging with a real producer (e.g. Kafka or RabbitMQ) behind a
  `MessagePublisher` interface, so the queue can be swapped or mocked in tests.
- Add the message type, the source provider and a received timestamp to the standard messages.
- Return errors in a structured format (e.g. RFC 9457 `application/problem+json`).
- Handle duplicate or out of order messages (idempotency on `event_id`).
- Add Spring Boot Actuator for health checks and metrics, and a Dockerfile.
