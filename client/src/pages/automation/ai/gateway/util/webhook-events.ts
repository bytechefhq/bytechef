export interface ParsedWebhookEventsI {
    events: string[];
    malformed: boolean;
}

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

export const parseWebhookEvents = (raw: string | null | undefined): string[] => parseWebhookEventsDetailed(raw).events;
