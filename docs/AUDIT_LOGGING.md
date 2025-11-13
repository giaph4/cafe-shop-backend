# Audit Logging Guide

## Overview
The audit logging subsystem captures sensitive actions performed within the Coffee Shop backend. Audit records are persisted in the `audit_logs` table and can be queried for compliance, security reviews, or troubleshooting.

## Captured Data Points
Each audit entry stores:
- **Timestamp (`event_time`)** – when the action took place (UTC).
- **Action (`action`)** – code-friendly identifier of the business event.
- **Resource (`resource_type`, `resource_id`)** – the domain object affected.
- **Actor information** – user id, username, and resolved authorities at the time of the action.
- **Request metadata** – HTTP method, URI, client IP, user agent.
- **Outcome** – `success` flag, optional error message, and optional JSON `details` payload describing old/new values or contextual notes.

### Indexing
Database indexes exist on:
- `event_time` – chronological analysis.
- `actor_id`, `actor_username` – actor-based lookups.
- `resource_type`, `resource_id` – resource-level investigations.

## Service Usage
`AuditLogService` exposes `recordAction` for domain services:
```java
auditLogService.recordAction(
        "INGREDIENT_INVENTORY_ADJUSTED",
        "INGREDIENT",
        "42",
        true,
        "Inventory adjusted for ingredient ID=42",
        "{\"oldQuantity\":\"50\",\"newQuantity\":\"75\"}",
        null
);
```
The helper automatically enriches actor and HTTP request metadata from Spring `SecurityContext` and `RequestContext` when available.

### Failure Logging
In failure paths, pass `success = false` and provide an `errorMessage`. Example:
```java
auditLogService.recordAction(
        "INGREDIENT_INVENTORY_ADJUSTMENT_FAILED",
        "INGREDIENT",
        "42",
        false,
        "Inventory adjustment failed for ingredient ID=42",
        null,
        exception.getMessage()
);
```

## Current Coverage
- **Inventory adjustments** (`IngredientService#adjustInventory`) now emit success and failure audit events describing quantity changes and the initiator.

## Extensibility Checklist
1. Define clear action codes (e.g., `ORDER_CANCELLED`, `PAYROLL_APPROVAL_STEP_COMPLETED`).
2. Include sufficient context in the JSON `details` payload to reconstruct the change.
3. Ensure sensitive values (passwords, secrets) are never logged.
4. Prefer structured diff representations for update operations.
5. Add unit tests verifying `AuditLogService` invocation in new code paths.

## Operational Notes
- Audit logs should be retained according to compliance requirements. Consider scheduled archiving for long-term storage.
- Integrate the table with BI/monitoring stacks (e.g., ELK, Grafana) for dashboards and alerting.
- Protect access to audit-log APIs and database objects via strict RBAC.
