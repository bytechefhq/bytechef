export interface ParsedWebhookEventsI {
    events: string[];
    malformed: boolean;
}

/**
 * Parses the JSON-encoded `events` column on an `AiObservabilityWebhookSubscription` row. The backend stores this as a
 * JSON array of strings, but historical rows or a bad migration can leave malformed or null data — rendering the raw
 * `JSON.parse` result would crash the entire webhooks page into an error boundary.
 *
 * Returns both the parsed event list and a {@code malformed} flag so the caller can surface a "broken subscription"
 * badge in the UI instead of silently rendering an apparently-healthy subscription that delivers zero webhooks. An
 * empty-but-valid {@code []} payload reports {@code malformed: false}.
 */
export const parseWebhookEventsDetailed = (raw: string | null | undefined): ParsedWebhookEventsI => {
    if (!raw) {
        return {events: [], malformed: false};
    }

    try {
        const parsed = JSON.parse(raw);

        if (!Array.isArray(parsed)) {
            return {events: [], malformed: true};
        }

        const events = parsed.filter((event): event is string => typeof event === 'string');

        return {events, malformed: events.length !== parsed.length};
    } catch (error) {
        console.error('Failed to parse webhook events', error);

        return {events: [], malformed: true};
    }
};

/**
 * Backward-compatible helper that returns just the event list. Prefer {@link parseWebhookEventsDetailed} for new call
 * sites that need to render a "malformed subscription" indicator.
 */
export const parseWebhookEvents = (raw: string | null | undefined): string[] => parseWebhookEventsDetailed(raw).events;
