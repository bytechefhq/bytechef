/**
 * Extracts text chunk from streaming data.
 * Handles various streaming payload formats by attempting to extract text from common field names.
 *
 * @param data - The incoming streaming data (can be a plain string or JSON object)
 * @returns Extracted text chunk as a string
 */
export function extractStreamChunk(data: unknown): string {
    let chunk = '';

    try {
        const obj = typeof data === 'string' ? JSON.parse(data) : data;

        chunk = obj?.text ?? obj?.delta ?? obj?.token ?? obj?.message ?? obj?.content?.[0]?.text ?? obj?.content ?? '';

        if (typeof chunk !== 'string') {
            chunk = String(chunk ?? '');
        }
    } catch {
        // Not JSON, treat as raw string
        chunk = String(data ?? '');
    }

    return chunk;
}
