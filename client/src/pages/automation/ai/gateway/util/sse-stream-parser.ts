export interface ParsedSseEventI {
    event: string;
    data: string;
}

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

export const parseStreamChunk = <T>(json: string): T | null => {
    try {
        return JSON.parse(json) as T;
    } catch (error) {
        console.error('Failed to parse SSE stream chunk', error, json);

        return null;
    }
};
