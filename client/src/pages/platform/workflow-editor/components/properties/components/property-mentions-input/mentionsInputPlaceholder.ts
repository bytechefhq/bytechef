export const TOOL_PROPERTY_PLACEHOLDER = 'Click the AI button or write =fromAi(...)';

export const FORMULA_MODE_PLACEHOLDER = "e.g. concat(firstName, ' ', lastName)";

export const TOOL_PROPERTY_FORMULA_MODE_PLACEHOLDER = "e.g. fromAi('name', 'STRING')";

const DEFAULT_PLACEHOLDER = "Use '$' for data pills and '=' for an expression";

interface MentionsInputPlaceholderProps {
    expressionEnabled: boolean | undefined;
    formulaMode?: boolean;
    placeholder?: string;
    toolProperty?: boolean;
}

export function getMentionsInputPlaceholder({
    expressionEnabled,
    formulaMode,
    placeholder,
    toolProperty,
}: MentionsInputPlaceholderProps): string {
    if (placeholder) {
        return placeholder;
    }

    if (expressionEnabled === false) {
        return '';
    }

    if (formulaMode) {
        return toolProperty ? TOOL_PROPERTY_FORMULA_MODE_PLACEHOLDER : FORMULA_MODE_PLACEHOLDER;
    }

    if (toolProperty) {
        return TOOL_PROPERTY_PLACEHOLDER;
    }

    return DEFAULT_PLACEHOLDER;
}
