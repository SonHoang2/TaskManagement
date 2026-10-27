---
name: adding-rabbitmq-event
description: Use when one service must react to something that happened in another (a new notification type, task assigned/commented, project invitation, any async side effect), or when touching RabbitMQ config, EventPublisher or EventConsumer in Backend/.
---

# Adding a RabbitMQ event

## Overview

All events go over one durable **topic exchange `task.events`**. The producer (task-service, project-service) and the consumer (notification-service) **each** declare the same queue and binding, and each keeps its **own copy** of the event class (no shared jar). The JSON shape is the contract, so both copies must match field for field.

Naming: routing key `<area>.<thing>.<verb-past>` (e.g. `task.comment.created`), queue `<routing-key>.queue`, class `<Area><Thing><Verb>Event`.

## Checklist (example: `sprint.completed`)

**Producer service**
1. In `application.properties`, add `app.rabbitmq.queue.sprint-completed=sprint.completed.queue` and `app.rabbitmq.routing-key.sprint-completed=sprint.completed`. The exchange stays `task.events`.
2. In `config/RabbitMQConfig`, add the `@Value` fields, a `Queue` bean (`QueueBuilder.durable`) and a `Binding` bean. Keep the `Jackson2JsonMessageConverter` bean.
3. Add `events/SprintCompletedEvent` (`@Data @Builder @NoArgsConstructor @AllArgsConstructor`). It carries ids plus the display text the consumer needs (title), so the consumer never calls back.
4. Add `EventPublisher.publishSprintCompletedEvent(event)` using `rabbitTemplate.convertAndSend(exchange, routingKey, event)`.
5. Publish from the `*ServiceImpl` only when the state actually changed (compare old vs new, as `TaskServiceImpl.update` does for the assignee).

**notification-service**

6. Mirror steps 1–3: same keys in `application.properties`, the same beans in `config/RabbitMQConfig`, and an identical event class in `events/`.
7. Add `EventConsumer`: `@RabbitListener(queues = "${app.rabbitmq.queue.sprint-completed}")`. Build the recipients as a `Set`, drop `null` and the actor, and create one notification per recipient inside its own `try/catch`, so a single failure doesn't requeue the whole message forever.
8. If it's a new kind, add a value to `NotificationType`. Then update `NotificationType` in `Frontend/src/app/core/models/index.ts` and `META` in `features/notifications/notification-meta.ts` (a `Record`, so the build fails if it's missing).
9. WebSocket delivery is automatic. `NotificationServiceImpl.create` triggers `NotificationPusher` after commit.

**Tests**: assert that the publisher is called (or not called) in the producer's `*ServiceImplTest` with `verify(eventPublisher).publish…(argThat(...))`, and unit-test the consumer's recipient logic.

## Common mistakes

| Mistake | Consequence |
|---|---|
| Queue/binding declared only on one side | Messages are dropped when the consumer isn't up yet (sprint/notification default to `replicas: 0`) |
| Field renamed in one copy of the event | Jackson leaves it `null` silently. No error anywhere |
| Using a new exchange name | Not bound, so nothing arrives |
| Publishing inside a transaction that later rolls back | The consumer acts on a change that never happened. For risky flows, publish after commit (`TransactionSynchronization.afterCommit`, as `MembershipCache.evictAfterCommit` does) |
| Notifying the actor about their own action | Noise. Remove `event.getUserId()` from recipients |
| Throwing from a listener | Message redelivered in a loop. Catch and log per recipient |
