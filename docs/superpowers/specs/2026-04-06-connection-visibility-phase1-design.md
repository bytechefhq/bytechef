# Connection Visibility — Design Spec

**Date:** 2026-04-06
**Status:** Draft

## Phases

- **Phase 1**: PRIVATE/PROJECT/WORKSPACE visibility, permission enforcement (service layer), environment enforcement (service layer), `project_connection` table, GraphQL mutations, connection picker UI
- **Phase 2**: ORGANIZATION visibility, Settings page for org connections
- **Phase 3**: User removal flows, workflow pausing, connection reassignment, audit logging

## Key Design Decisions

1. **Permission enforcement in the service layer** — not facade. `ConnectionServiceImpl` validates owner-or-admin before mutating operations.
2. **Environment enforcement in the service layer** — deployment creation validates all referenced connections match the target environment.
3. **GraphQL for new operations** — share/promote/demote/reassign use GraphQL mutations, not new REST endpoints.
4. **Data migration** — existing connections default to WORKSPACE visibility to preserve current behavior.
5. **ORGANIZATION connections created separately** — in Settings page by org admins, not promoted from lower scopes.

See full implementation plan: `docs/superpowers/plans/2026-04-06-connection-visibility.md`
