# Java / Spring Boot Guidelines

- Java 21, Spring Boot 4.0.x, Maven (use `./mvnw`). Base package `com.tothenew.tickets`.
- Layers: `web` → `service` → `repository`. Controllers hold no business logic; repositories are never called from controllers.
- Never expose JPA entities in API responses. Use records for DTOs; map explicitly.
- Constructor injection only (no `@Autowired` on fields). Use `final` fields.
- `@Transactional` on service methods; `readOnly = true` for queries.
- Validation: Jakarta Bean Validation on request DTOs + `@Valid`. Business rules live in the service, not in controllers.
- Enums persisted with `@Enumerated(EnumType.STRING)`.
- Schema via Flyway only; `spring.jpa.hibernate.ddl-auto=validate`.
- Queries must be parameterised (JPA Specifications / JPQL). No string-concatenated SQL.
- Exceptions: domain-specific runtime exceptions, mapped centrally in `@RestControllerAdvice`. Never leak stack traces or entity internals.
- Logging via SLF4J; never log secrets or full request bodies.
- No secrets in code or `application.yml`; use env vars. `data/`, `.env` are git-ignored.
- Time: `Instant` (UTC). No `new Date()`.
- Keep methods small; no unused code; no TODOs left without a task reference.
- The state machine rules live ONLY in `TicketStatus`; change status only via `Ticket.transitionTo`. Never add a public status setter.
- Test config: override via `application.properties` or profiles, never a same-named `application.yml` (it shadows the main file).
