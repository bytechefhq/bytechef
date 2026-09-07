export interface FromAiToggleResultI {
    savePayload: {fromAi: boolean; includeInMetadata: boolean; value: string} | null;
    value: string;
}

export function computeFromAiToggle({
    custom = false,
    fromAi,
    fromAiExpression,
    hasPath = true,
    hasWorkflowId = true,
}: {
    custom?: boolean;
    fromAi: boolean;
    fromAiExpression: string;
    hasPath?: boolean;
    hasWorkflowId?: boolean;
}): FromAiToggleResultI {
    const value = fromAiExpression;

    if (!hasPath || !hasWorkflowId) {
        return {savePayload: null, value};
    }

    return {
        savePayload: {fromAi, includeInMetadata: custom || fromAi, value},
        value,
    };
}
