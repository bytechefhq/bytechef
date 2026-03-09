/**
 * Cache duration for static definition queries (component, action, trigger, connection,
 * cluster element, task dispatcher definitions). These rarely change during a user session,
 * so a 5-minute stale time avoids redundant network calls while allowing eventual cache cleanup.
 */
export const DEFINITION_STALE_TIME = 5 * 60 * 1000;
