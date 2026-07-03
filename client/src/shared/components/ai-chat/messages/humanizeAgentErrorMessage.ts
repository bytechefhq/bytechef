/**
 * Extracts a user-readable explanation from the raw RUN_ERROR message Spring AI surfaces (typically a Java
 * exception toString wrapping a provider error envelope). Tries common provider envelopes (Anthropic, OpenAI)
 * first, falls back to stripping a "ClassName: message" prefix, and returns the input unchanged when nothing
 * matches — so the FQCN, HTTP code, and request_id never leak into the transcript. Shared by the AI Hub and
 * Copilot runtime providers.
 */
export const humanizeAgentErrorMessage = (raw: string): string => {
    const jsonStart = raw.indexOf('{');

    if (jsonStart !== -1) {
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

    const exceptionStripMatch = raw.match(/^[\w.$]+(?:Exception|Error):\s+(.+)$/s);

    if (exceptionStripMatch && exceptionStripMatch[1].length > 0) {
        return exceptionStripMatch[1];
    }

    return raw;
};
