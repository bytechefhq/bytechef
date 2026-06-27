export const TOOL_PROPERTY_PLACEHOLDER = 'Click the AI button or write =fromAi(...)';

const DEFAULT_PLACEHOLDER = "Use '$' for data pills and '=' for an expression";

// Mirrors the Placeholder.configure logic in PropertyMentionsInputEditor. An explicit placeholder
// always wins. With expressions disabled there is no expression hint. Tool properties are effectively
// always expression fields (AI button or a hand-written =fromAi(...)), so the data-pill hint is
// replaced with a tools-specific message.
export function getMentionsInputPlaceholder({
    expressionEnabled,
    placeholder,
    toolProperty,
}: {
    expressionEnabled: boolean | undefined;
    placeholder?: string;
    toolProperty?: boolean;
}): string {
    if (placeholder) {
        return placeholder;
    }

    if (expressionEnabled === false) {
        return '';
    }

    if (toolProperty) {
        return TOOL_PROPERTY_PLACEHOLDER;
    }

    return DEFAULT_PLACEHOLDER;
}
