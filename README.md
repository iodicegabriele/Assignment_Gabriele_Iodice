# Feed Standardization Service

A Spring Boot microservice that receives `ODDS_CHANGE` and `BET_SETTLEMENT` messages from two
feed providers (Alpha and Beta), each with its own format, converts them into one standard
internal format and sends them to a (mocked) message queue.

## Requirements

- **Java 27.** If it is not installed, Gradle downloads it automatically on the first build
  (Foojay toolchain resolver). Gradle itself only needs any JDK 17+ to start.
- Nothing else: Gradle is included through the wrapper (`gradlew` / `gradlew.bat`).

## Run

```bash
./gradlew bootRun
```

On Windows use `gradlew.bat bootRun`. The service starts on `http://localhost:8080`.

Or build and run the jar:

```bash
./gradlew bootJar
java -jar build/libs/Assignment_Gabriele_Iodice-0.0.1-SNAPSHOT.jar
```

## Test

```bash
./gradlew test
```

The suite contains unit tests for the converters, `@WebMvcTest` tests for the controllers and
an end-to-end test that runs the whole application with the sample messages of the assignment.

### Postman API tests

[`AssessmentGabrieleIodice.postman_collection.json`](AssessmentGabrieleIodice.postman_collection.json)
contains the valid sample messages and the invalid inputs (wrong outcomes, missing fields, unknown
message types, missing or invalid odds, malformed JSON, wrong method or content type). Every request
has a post-response script that checks the status code and the exact error message.

Import it in Postman, or run it from the command line against a running service:

```bash
npx newman run AssessmentGabrieleIodice.postman_collection.json --env-var baseUrl=http://localhost:8080
```

### Continuous integration

- `.github/workflows/ci.yml`: on every push and pull request, runs the unit tests, starts the
  service and runs the Postman collection with Newman.
- `.github/workflows/claude-code-review.yml`: on every push to a branch with an open pull
  request, Claude reviews the change and comments on the pull request
  (needs the `ANTHROPIC_API_KEY` repository secret).

## Endpoints

Both endpoints accept `POST` with `Content-Type: application/json`, one message per request.

### `POST /provider-alpha/feed`

```bash
curl -i -X POST localhost:8080/provider-alpha/feed -H "Content-Type: application/json" \
  -d '{"msg_type":"odds_update","event_id":"ev123","values":{"1":2.0,"X":3.1,"2":3.8}}'

curl -i -X POST localhost:8080/provider-alpha/feed -H "Content-Type: application/json" \
  -d '{"msg_type":"settlement","event_id":"ev123","outcome":"1"}'
```

| Field      | Values                                                   |
|------------|----------------------------------------------------------|
| `msg_type` | `odds_update` or `settlement`                            |
| `event_id` | mandatory                                                |
| `values`   | for `odds_update`: exactly the keys `1`, `X`, `2`        |
| `outcome`  | for `settlement`: one of `1`, `X`, `2`                   |

### `POST /provider-beta/feed`

```bash
curl -i -X POST localhost:8080/provider-beta/feed -H "Content-Type: application/json" \
  -d '{"type":"ODDS","event_id":"ev456","odds":{"home":1.95,"draw":3.2,"away":4.0}}'

curl -i -X POST localhost:8080/provider-beta/feed -H "Content-Type: application/json" \
  -d '{"type":"SETTLEMENT","event_id":"ev456","result":"away"}'
```

| Field      | Values                                                   |
|------------|----------------------------------------------------------|
| `type`     | `ODDS` or `SETTLEMENT`                                   |
| `event_id` | mandatory                                                |
| `odds`     | for `ODDS`: exactly the keys `home`, `draw`, `away`      |
| `result`   | for `SETTLEMENT`: one of `home`, `draw`, `away`          |

### Responses

| Status | When                                                                                 |
|--------|--------------------------------------------------------------------------------------|
| `204`  | The message was converted and sent to the queue (no body)                            |
| `400`  | Malformed JSON, missing fields, unknown message type, invalid outcome or odds (≤ 1.0) |
| `405`  | Method other than `POST`                                                             |
| `415`  | Content type other than JSON                                                         |
| `500`  | Unexpected error (logged)                                                            |

## Standard message format

Both providers are normalized to the same two messages, using a shared `Outcome` enum
(`HOME` = "1", `DRAW` = "X", `AWAY` = "2"):

| Message                | Fields                                                      |
|------------------------|-------------------------------------------------------------|
| `OddsChangeMessage`    | `id` (event id), `odds`: `Map<Outcome, Double>`             |
| `BetSettlementMessage` | `id` (event id), `result`: `Outcome`                        |

For example, the Alpha sample odds message and the Beta sample settlement message are logged as:

```
OddsChangeMessage[id=ev123, odds={HOME=2.0, DRAW=3.1, AWAY=3.8}]
BetSettlementMessage[id=ev456, result=AWAY]
```

## Message queue

As allowed by the assignment, the queue is mocked: the standardized message is written to the
application log.

## Project structure

```
controllers/            AlphaFeedController, BetaFeedController (one endpoint per provider)
controllers/advices/    FeedControllerAdvice (maps errors to HTTP statuses)
dtos/                   AlphaFeedDTO, BetaFeedDTO (provider formats + validation)
entities/               StandardMessage, Outcome and the two standard messages
services/               ConverterService<T> and one implementation per provider
```

Adding a provider means adding a DTO, a `ConverterService` implementation and a controller.
