/**
 * Surface-agnostic mapper for interactive tool-result payloads, reusable across any chat surface (AI Hub,
 * Copilot, etc.). Returns:
 *   - `{ok: true, data, type}` — payload parsed and validated successfully.
 *   - `{ok: false, errorMessage, toolName}` — payload unparseable or failed kind/shape validation.
 *   - `undefined` — `toolCallName` is not one of the interactive tool names handled here.
 */

export type ToolResultDataPartType =
    | {data: Record<string, unknown>; ok: true; type: string}
    | {errorMessage: string; ok: false; toolName: string};

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

    if (toolCallName === 'askUserQuestion') {
        const parsed = parseJson<AskUserQuestionResultI>(eventContent, 'askUserQuestion result');

        if (!parsed || parsed.kind !== 'ask-user-question' || !Array.isArray(parsed.questions)) {
            const errorMessage = !parsed
                ? 'askUserQuestion returned an unparseable payload'
                : 'askUserQuestion returned a malformed payload (missing kind or questions)';

            return {errorMessage, ok: false, toolName: 'askUserQuestion'};
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

    return undefined;
}
