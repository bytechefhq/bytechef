/**
 * Extracts text chunk from streaming data.
 * Handles various streaming payload formats by attempting to extract text from common field names.
 *
 * @param data - The incoming streaming data (can be a plain string or JSON object)
 * @returns Extracted text chunk as a string
 */
export function extractStreamChunk(data: unknown): string {
    try {
        const obj = typeof data === 'string' ? JSON.parse(data) : data;
        const value =
            obj?.text ?? obj?.delta ?? obj?.token ?? obj?.message ?? obj?.content?.[0]?.text ?? obj?.content ?? '';

        return typeof value === 'string' ? value : String(value ?? '');
    } catch {
        // Not JSON, treat as raw string
        return String(data ?? '');
    }
}
