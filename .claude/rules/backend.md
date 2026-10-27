---
paths:
  - "Backend/**/*.java"
  - "Backend/**/pom.xml"
  - "Backend/**/application*.properties"
  - "Backend/**/application*.yml"
---

# Backend (Spring Boot) rules

## Package layout per feature

`<service>/src/main/java/com/sonhoang2/<pkg>/<feature>/`:
`XController`, `XService` (interface), `XServiceImpl`, `XRepository`, `dto/` (`XCreateRequest`, `XUpdateRequest`, `XResponse`), `entity/`.
Cross-cutting code goes in `common/{config,dto,exception,security}`, Feign clients in `feign/`, RabbitMQ in `events/` + `config/RabbitMQConfig`.

## Controllers

- `@RequestMapping("/<plural>")`. The `/api/v1` prefix is added by `WebMvcConfig`, so don't repeat it.
- Every user-facing handler takes `@RequestHeader("X-User-Id") UUID userId` and passes it to the service.
- Return `ResponseEntity<JSendResponse<Map<String, T>>>` and wrap the payload under a named key: `JSendResponse.success(Map.of("task", dto))`. Pages go under `"page"` as `PageResponse<T>`.
- Create returns `201` with a `Location` header (`ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")`). Delete returns `200` with `JSendResponse.success(null)`.
- Use `@Valid @RequestBody` and partial updates with `@PatchMapping`. Bean-validation messages are field-oriented (`"title is required"`) because the frontend shows them.

## Services

- Class-level `@Service @Transactional @RequiredArgsConstructor`, and `@Transactional(readOnly = true)` on reads.
- Check authorization in the service, before touching data: `projectAccessGuard.requireMember / requireManager / requireTaskMember`. `requireMember` throws `AccessDeniedException` (403).
- Map with the injected `ModelMapper`. It is configured with `STRICT` matching and `skipNull`, so `map(updateRequest, entity)` gives PATCH semantics.
- Hot read paths get `@RateLimit(points, durationSeconds, keyPrefix = "<area>:<op>")` from common-lib.
- If you change project membership, call `membershipCache.evictAfterCommit(userIds)`. task-service caches memberships in Redis.

## Exceptions → HTTP

Throw `com.sonhoang2.common.exception.ResourceNotFoundException` (404) and `ResourceConflictException` (409), plus Spring's `AccessDeniedException` (403).
In user/project/task-service, the per-service copies under `<pkg>.common.exception` are **not** handled by common-lib's `GlobalExceptionHandler`, so they fall through to a 500. sprint-service and notification-service have their own `GlobalExceptionHandler`; use their local classes there.

## Entities

- `@Id @GeneratedValue(strategy = GenerationType.UUID) UUID id`, Lombok `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`, `@CreationTimestamp/@UpdateTimestamp Instant`.
- Store enums as `@Enumerated(EnumType.STRING)`.
- Reference other services' data by UUID only (`projectId`, `assigneeId`). There are no cross-service JPA relations or shared tables.
- Add an `@Index` for every FK-like column you filter by.

## Inter-service calls

- Feign: `@FeignClient(name = "x-service", url = "${services.x-service.url:http://x-service}", path = "/api/v1")`, forwarding `@RequestHeader("X-User-Id")`. The return type is the service's `JSendResponse<Map<String, T>>`; unwrap it with `.data().get("key")`.
- Prefer events over synchronous calls for side effects. See the `adding-rabbitmq-event` skill.

## Tests

- Service: `@ExtendWith(MockitoExtension.class)`, `@Mock` the dependencies, `@InjectMocks XServiceImpl`. Include the 403 path (guard throws) and the 404 path.
- Controller: `@WebMvcTest(XController.class) @Import(SecurityConfig.class)`, `@MockitoBean XService`, assert on `jsonPath("$.status")` and `$.data.<key>`.
- Test names follow `method_ShouldX_WhenY`. Don't add `*ApplicationTests`; CI skips them.
