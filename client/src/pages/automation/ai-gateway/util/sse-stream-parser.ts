export interface ParsedSseEventI {
    event: string;
    data: string;
}

/**
 * Consumes a buffer of raw SSE text and splits it into discrete events + the unconsumed tail. SSE messages are
 * separated by {@code "\n\n"} (or {@code "\r\n\r\n"} for servers that use CRLF line endings); each event is a set of
 * lines, some of which start with {@code "event:"} or {@code "data:"}. Multiple {@code "data:"} lines within one
 * event are concatenated with {@code "\n"} per the SSE spec. This helper extracts only the two fields the AI Gateway
 * playground cares about and leaves every partial (trailing-incomplete) event in {@code remainder} so the next read
 * can complete it.
 *
 * <p>Returning structured events instead of parsing them inline in the component has two reasons:
 * <ol>
 *   <li>pure functions are unit-testable without mocking {@code fetch}, {@code ReadableStream}, or a DOM;</li>
 *   <li>server-emitted {@code event: error} frames can be dispatched by the caller to the error UI instead of being
 *       silently ignored when the discriminator isn't on the default {@code "message"} event name.</li>
 * </ol>
 */
export const splitSseEvents = (buffer: string): {events: ParsedSseEventI[]; remainder: string} => {
    const normalized = buffer.replace(/\r\n/g, '\n');
    const segments = normalized.split('\n\n');
    const remainder = segments.pop() ?? '';

    const events: ParsedSseEventI[] = [];

    for (const segment of segments) {
        const lines = segment.split('\n');

        let eventName = 'message';
        const dataLines: string[] = [];

        for (const line of lines) {
            if (line.startsWith('event:')) {
                eventName = line.slice('event:'.length).trim();
            } else if (line.startsWith('data:')) {
                dataLines.push(line.slice('data:'.length).trim());
            }
        }

        const data = dataLines.join('\n');

        if (data !== '') {
            events.push({data, event: eventName});
        }
    }

    return {events, remainder};
};

/**
 * Safely parses a stream chunk's JSON body. Returns {@code null} on parse failure and logs to the console so a
 * truncated or malformed chunk doesn't abort the entire stream — the caller skips the bad event and continues. If
 * every chunk is bad the stream eventually terminates on its own.
 */
export const parseStreamChunk = <T>(json: string): T | null => {
    try {
        return JSON.parse(json) as T;
    } catch (error) {
        console.error('Failed to parse SSE stream chunk', error, json);

        return null;
    }
};
