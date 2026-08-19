/**
 * Surface-agnostic mapper for interactive tool-result payloads. Encapsulates the parse + validate logic
 * that was previously inlined in `AiHubRuntimeProvider`'s `onToolCallResultEvent` handler, making it
 * reusable across any chat surface (AI Hub, copilot, etc.).
 *
 * Returns:
 *   - `{ok: true, data, type}` — payload was parsed and validated successfully.
 *   - `{ok: false, errorMessage, toolName}` — payload was unparseable or failed kind/shape validation.
 *   - `undefined` — `toolCallName` is not one of the tool names handled here, or the payload carries
 *     nothing renderable (e.g. a queryKnowledgeBase result with no hits).
 */

export type ToolResultDataPartType =
    {data: Record<string, unknown>; ok: true; type: string} | {errorMessage: string; ok: false; toolName: string};

/**
 * Parses a JSON string, returning the parsed value or `null` on failure. Logs a structured warning so
 * regressions in tool/LLM serializers are visible in production logs rather than silently swallowed.
 */
export const parseJson = <T>(content: string, contextLabel: string): T | null => {
    try {
        return JSON.parse(content) as T;
    } catch (parseError) {
        console.warn('Failed to parse tool-result JSON', {
            context: contextLabel,
            len: content.length,
            message: parseError instanceof Error ? parseError.message : String(parseError),
            sample: content.slice(0, 80),
        });

        return null;
    }
};

interface CreateConnectionResultI {
    componentLabel: string;
    componentName: string;
    kind: 'create-connection';
    suggestedName?: string;
}

interface SelectConnectionResultI {
    componentLabel: string;
    componentName: string;
    kind: 'select-connection';
}

interface SelectPropertyOptionResultI {
    componentName: string;
    kind: 'select-property-option';
    options: Array<{label: string; value: string}>;
    propertyName: string;
    truncated?: boolean;
}

interface AskUserQuestionResultI {
    awaitingAnswer?: boolean;
    kind: 'ask-user-question';
    questions: Array<{
        header?: string;
        multiSelect: boolean;
        options: Array<{description?: string; label: string}>;
        question: string;
    }>;
}

interface KnowledgeBaseCitationsResultI {
    hits: Array<{
        docId?: string;
        docTitle?: string;
        excerpt?: string;
        knowledgeBaseId?: string;
        knowledgeBaseName?: string;
        score?: number;
    }>;
    kind: 'knowledge-base-citations';
}

export function toToolResultDataPart(toolCallName: string, eventContent: string): ToolResultDataPartType | undefined {
    if (toolCallName === 'createConnection') {
        const parsed = parseJson<CreateConnectionResultI>(eventContent, 'createConnection result');

        if (!parsed || parsed.kind !== 'create-connection' || !parsed.componentName) {
            const errorMessage = !parsed
                ? 'createConnection returned an unparseable payload'
                : 'createConnection returned a malformed payload (missing kind or componentName)';

            return {errorMessage, ok: false, toolName: 'createConnection'};
        }

        return {
            data: {
                componentLabel: parsed.componentLabel,
                componentName: parsed.componentName,
                kind: parsed.kind,
                suggestedName: parsed.suggestedName,
            },
            ok: true,
            type: 'data-create-connection',
        };
    }

    if (toolCallName === 'selectConnection') {
        const parsed = parseJson<SelectConnectionResultI>(eventContent, 'selectConnection result');

        if (!parsed || parsed.kind !== 'select-connection' || !parsed.componentName) {
            const errorMessage = !parsed
                ? 'selectConnection returned an unparseable payload'
                : 'selectConnection returned a malformed payload (missing kind or componentName)';

            return {errorMessage, ok: false, toolName: 'selectConnection'};
        }

        return {
            data: {
                componentLabel: parsed.componentLabel,
                componentName: parsed.componentName,
                kind: parsed.kind,
            },
            ok: true,
            type: 'data-select-connection',
        };
    }

    if (toolCallName === 'selectPropertyOption' || toolCallName === 'selectTriggerPropertyOption') {
        const parsed = parseJson<SelectPropertyOptionResultI>(eventContent, 'selectPropertyOption result');

        if (!parsed || parsed.kind !== 'select-property-option' || !Array.isArray(parsed.options)) {
            const errorMessage = !parsed
                ? 'selectPropertyOption returned an unparseable payload'
                : 'selectPropertyOption returned a malformed payload (missing kind or options)';

            return {errorMessage, ok: false, toolName: toolCallName};
        }

        return {
            data: {
                componentName: parsed.componentName,
                kind: parsed.kind,
                options: parsed.options,
                propertyName: parsed.propertyName,
                truncated: parsed.truncated,
            },
            ok: true,
            type: 'data-select-property-option',
        };
    }

    if (toolCallName === 'queryKnowledgeBase') {
        const parsed = parseJson<KnowledgeBaseCitationsResultI>(eventContent, 'queryKnowledgeBase result');

        // Unlike the interactive tools above, a non-citation payload here is not an error worth surfacing:
        // the tool legitimately returns {"error": ...} for bad input, and empty hits mean "nothing to cite" —
        // in both cases the assistant's text answer flows through without a citations block.
        if (
            !parsed ||
            parsed.kind !== 'knowledge-base-citations' ||
            !Array.isArray(parsed.hits) ||
            parsed.hits.length === 0
        ) {
            return undefined;
        }

        return {
            data: {
                hits: parsed.hits,
                kind: parsed.kind,
            },
            ok: true,
            type: 'data-knowledge-base-citations',
        };
    }

    if (toolCallName === 'askUserQuestion') {
        return toAskUserQuestionDataPart(eventContent, toolCallName);
    }

    // Payload-kind fallback. A specialist subagent's question reaches the client as the *delegate* tool's result, so
    // the tool name is the specialist's (e.g. `mcp_agent`) and none of the branches above match. Keyed
    // on the payload's own `kind` so one client change serves every specialist, present and future.
    //
    // Scoped to kinds this file already handles: a blanket parse-and-sniff would let an unrelated tool that happens to
    // emit a `kind` field hijack a renderer.
    const fallbackKind = readPayloadKind(eventContent);

    if (fallbackKind === 'ask-user-question') {
        return toAskUserQuestionDataPart(eventContent, toolCallName);
    }

    return undefined;
}

function toAskUserQuestionDataPart(eventContent: string, toolName: string): ToolResultDataPartType {
    const parsed = parseJson<AskUserQuestionResultI>(eventContent, `${toolName} result`);

    if (!parsed || parsed.kind !== 'ask-user-question' || !Array.isArray(parsed.questions)) {
        const errorMessage = !parsed
            ? 'askUserQuestion returned an unparseable payload'
            : 'askUserQuestion returned a malformed payload (missing kind or questions)';

        return {errorMessage, ok: false, toolName};
    }

    return {
        data: {
            awaitingAnswer: parsed.awaitingAnswer,
            kind: parsed.kind,
            questions: parsed.questions,
        },
        ok: true,
        type: 'data-ask-user-question',
    };
}

/**
 * Reads a tool result's `kind` without logging. Deliberately not `parseJson`: this runs on *every* unmatched tool
 * result, and most of those are ordinary prose, so `parseJson`'s warning would fill the console with noise about
 * payloads that were never meant to be JSON. The cheap `{` guard skips the parse entirely for those.
 */
function readPayloadKind(eventContent: string): string | undefined {
    if (!eventContent.trimStart().startsWith('{')) {
        return undefined;
    }

    try {
        const parsed: unknown = JSON.parse(eventContent);

        if (typeof parsed !== 'object' || parsed === null) {
            return undefined;
        }

        const kind = (parsed as {kind?: unknown}).kind;

        return typeof kind === 'string' ? kind : undefined;
    } catch {
        return undefined;
    }
}
