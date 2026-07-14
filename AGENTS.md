# Agent Instructions

## Project identity

This repository is the standalone **OJ Training Data Manager**. It is an independent project: do not add a runtime, build, database, volume, source-code or documentation dependency on any other repository.

The application starts with an empty database. Do not add an implicit migration or data import from another system.

## Runtime scope

- `backend/training-api` is the only runnable Spring Boot backend and listens on port `8190` in containers.
- `frontend` is the Vue 3 application. Nginx serves it at `/` and proxies browser `/api/**` requests to `training-api`.
- `deploy/docker-compose.yml` starts exactly `training-db`, `training-api` and `frontend`.
- MySQL data must stay in this project's independently named Docker volume.
- Member identity contains only `username` plus its OJ handles and collection state. Do not add nickname, account login, role or JWT concepts.

## HTTP security contract

- Every `GET` request is public, including members, handles, collection cursors, job details and health checks.
- Only `GET`, `HEAD` and `OPTIONS` are public; every other HTTP method must provide the configured plaintext operation password in `X-Operation-Password`.
- There are no user accounts, login endpoint, JWTs, sessions or roles.
- The operation password comes from `TRAINING_OPERATION_PASSWORD`. Changing it requires updating deployment configuration and restarting the API.
- Never place the operation password in a URL, frontend storage, source code, image layer, response or log.
- Plaintext refers to the request credential format, not safe transport. Production deployments must put HTTPS in front of the service.

## Data and collection rules

- Preserve the ODS, DWD, DWM and DWS boundaries and their repeatable refresh behavior.
- Preserve Codeforces and AtCoder collection, per-member/per-OJ `lastCollectedAt`, and cleanup when a handle is changed or removed.
- A missing cursor means full-history collection. Later runs start at `lastCollectedAt - lookback`; only successful collection advances the cursor.
- Automatic submission collection and AtCoder problem metadata bootstrap/scheduling must remain disabled unless explicitly enabled by deployment configuration.
- Manual collection job state and recent job history are in process memory and are lost when `training-api` restarts. Durable submissions, warehouse data and cursors remain in MySQL.

## Logging

- Use SLF4J/Logback and write `combined.log` plus `error.log` under `LOG_DIR`.
- Error logs must include a stable `errorCode`.
- Never log operation passwords, request credential headers, database passwords, cookies, tokens or full personal sensitive data.

## Documentation and verification

- Keep the nearest module `README.md` current when files or responsibilities change.
- Run `mvn clean test` after Java changes.
- Run `pnpm test` and `pnpm build` after frontend logic changes when those scripts exist.
- Run `./scripts/compose-config.sh` after deployment changes and `./scripts/smoke-test.sh` after starting the stack.

## Git rules

- Do not commit or push unless the project owner explicitly asks.
- If the owner says to push, that authorizes committing and pushing the current requested work.
- Write merge-request titles and descriptions in Chinese.
- If a new file needs an author annotation, the author must be `huangbingrui.awa`.
