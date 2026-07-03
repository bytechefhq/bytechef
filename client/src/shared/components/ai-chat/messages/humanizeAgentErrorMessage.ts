/**
 * Extracts a user-readable explanation from the raw RUN_ERROR message Spring AI surfaces. The raw form is
 * usually a Java exception toString like:
 *
 * <pre>com.anthropic.errors.BadRequestException: 400: {"type":"error","error":{"type":"invalid_request_error","message":"Your credit balance is too low to access the Anthropic API. Please go to Plans & Billing to upgrade or purchase credits."},"request_id":"req_..."}</pre>
 *
 * Rendering that verbatim leaks the FQCN, HTTP code, and internal {@code request_id} into the chat
 * transcript. This helper tries common provider envelopes (Anthropic, OpenAI) first, falls through to a
 * "ClassName: message" strip when the rest is a plain exception, and returns the input unchanged when
 * nothing matches.
 *
 * Shared by both the AI Hub and Copilot runtime providers so a server-side run failure renders the same
 * humanized text in either surface.
 */
export const humanizeAgentErrorMessage = (raw: string): string => {
    // Find the first JSON object inside the message and try to parse it. Anthropic + OpenAI both shape
    // their error envelopes as `{"error":{"message":"..."}}` so a single nested-message lookup covers both.
    const jsonStart = raw.indexOf('{');

    if (jsonStart !== -1) {
        // Greedy slice to the last '}' — the toString chain may include `request_id` JSON tails or trailing
        // commentary. JSON.parse is strict; we only succeed when the bracketed slice is a clean object.
        const jsonEnd = raw.lastIndexOf('}');

        if (jsonEnd > jsonStart) {
            const candidate = raw.slice(jsonStart, jsonEnd + 1);

            try {
                const parsed = JSON.parse(candidate);
                const nestedMessage =
                    typeof parsed?.error?.message === 'string'
                        ? parsed.error.message
                        : typeof parsed?.message === 'string'
                          ? parsed.message
                          : null;

                if (nestedMessage && nestedMessage.length > 0) {
                    return nestedMessage;
                }
            } catch {
                // Fall through to the regex-based strip.
            }
        }
    }

    // Plain Java exception like "java.lang.IllegalStateException: Current user is not set!" — strip the
    // FQCN prefix so the user sees only the message. The regex anchors at the start and requires a colon
    // + space so it doesn't accidentally chop a normal URL or namespaced identifier from real prose.
    const exceptionStripMatch = raw.match(/^[\w.$]+(?:Exception|Error):\s+(.+)$/s);

    if (exceptionStripMatch && exceptionStripMatch[1].length > 0) {
        return exceptionStripMatch[1];
    }

    return raw;
};
