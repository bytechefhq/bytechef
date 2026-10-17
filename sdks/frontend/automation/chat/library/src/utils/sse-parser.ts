const EVENT_PREFIX = 'event:';
const DATA_PREFIX = 'data:';
const SPACE = ' ';

type EventHandlersType = Record<string, (data: unknown) => void>;

export function parseAndDispatchSSE(
    chunk: string,
    onDefaultMessage: (data: unknown) => void,
    customHandlers?: EventHandlersType
): void {
    const events = chunk.split(/\r?\n\r?\n+/);

    for (const event of events) {
        if (!event.trim()) {
            continue;
        }

        const dataLines: string[] = [];
        let eventType = 'message';
        const lines = event.split(/\r?\n/);

        for (const line of lines) {
            if (line.startsWith(EVENT_PREFIX)) {
                let value = line.slice(EVENT_PREFIX.length);

                if (value.startsWith(SPACE)) {
                    value = value.slice(SPACE.length);
                }

                eventType = value;
            } else if (line.startsWith(DATA_PREFIX)) {
                let dataLine = line.slice(DATA_PREFIX.length);

                if (dataLine.startsWith(SPACE)) {
                    dataLine = dataLine.slice(SPACE.length);
                }

                dataLines.push(dataLine);
            }
        }

        const rawData = dataLines.join('\n');
        let data: unknown;

        try {
            data = JSON.parse(rawData);
        } catch {
            data = rawData;
        }

        if (customHandlers && customHandlers[eventType]) {
            try {
                customHandlers[eventType](data);
            } catch (error) {
                console.error('SSE handler threw for event type', eventType, error);
            }
        } else {
            onDefaultMessage(data);
        }
    }
}

export async function drainSseResponse(response: Response, eventHandlers: EventHandlersType): Promise<void> {
    if (!response.body) {
        return;
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder('utf-8');
    let buffer = '';

    while (true) {
        const {done, value} = await reader.read();

        if (done) {
            break;
        }

        buffer += decoder.decode(value, {stream: true});

        const parts = buffer.split(/\r?\n\r?\n/);

        buffer = parts.pop() ?? '';

        for (const part of parts) {
            parseAndDispatchSSE(part, () => undefined, eventHandlers);
        }
    }

    if (buffer.trim()) {
        parseAndDispatchSSE(buffer, () => undefined, eventHandlers);
    }
}
