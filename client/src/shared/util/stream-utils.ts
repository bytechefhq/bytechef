/**
 * Extracts text chunk from streaming data.
 * Handles various streaming payload formats by attempting to extract text from common field names.
 *
 * @param data - The incoming streaming data (can be a plain string, a primitive or a JSON object)
 * @returns Extracted text chunk as a string
 */
export function extractStreamChunk(data: unknown): string {
    if (typeof data === 'number' || typeof data === 'boolean') {
        return String(data);
    }

    let obj = data;

    if (typeof data === 'string') {
        try {
            obj = JSON.parse(data);
        } catch {
            // Not JSON, treat as raw string
            return data;
        }

        if (typeof obj === 'string') {
            return obj;
        }

        if (obj === null || typeof obj !== 'object') {
            return data;
        }
    }

    const record = obj as {
        content?: unknown;
        delta?: unknown;
        message?: unknown;
        text?: unknown;
        token?: unknown;
    } | null;

    const value =
        record?.text ??
        record?.delta ??
        record?.token ??
        record?.message ??
        (record?.content as Array<{text?: unknown}> | undefined)?.[0]?.text ??
        record?.content ??
        '';

    return typeof value === 'string' ? value : String(value ?? '');
}
