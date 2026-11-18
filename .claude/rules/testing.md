# Testing Guidelines

- JUnit 5 + AssertJ + MockMvc. Frontend: Vitest + React Testing Library.
- Test names describe behaviour: `rejectsTransitionFromClosedToOpen`, not `test1`.
- Arrange / Act / Assert; one behaviour per test.
- Integration tests use `@SpringBootTest` + `@AutoConfigureMockMvc` with in-memory H2; DB cleaned per test.
- Assert outcomes, not just status codes: check response body (`code`, `message`) AND persisted state.
- State machine: parameterise over ALL 25 from/to pairs against `spec/state-machine.md`; integration-test every allowed and disallowed transition; assert status unchanged after rejection.
- Every requirement in `spec/test-strategy.md` traceability table must have a test.
- No tests that only call code without assertions; no mocks of the unit under test; no sleeping.
- Do not edit a failing test to make it pass without confirming the spec says the test is wrong.
- Coverage target ≥80% on `service` and `domain` (JaCoCo).
- Vitest: never write `beforeEach(() => mock.mockReset())`; an arrow that returns the mock makes vitest call it as a cleanup hook. Use a block body: `beforeEach(() => { mock.mockReset(); })`.
- Mocked UI tests are not enough: drive the real UI against the real backend (`e2e/`) before calling a UI task done. Wait for the page to finish loading before changing server state behind it.
- Run before declaring done: `cd backend && ./mvnw verify`; `cd frontend && npm test`.
