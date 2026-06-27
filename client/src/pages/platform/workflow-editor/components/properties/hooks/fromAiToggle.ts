export interface FromAiToggleResultI {
    savePayload: {fromAi: boolean; includeInMetadata: boolean; value: string} | null;
    value: string;
}

// Single source of truth for what the fromAi toggle does to the field value and the save payload.
// Toggling ON locks the field to the fromAi expression. Toggling OFF ("Customize AI generation")
// keeps the SAME expression but as an editable value so the user can tweak it — fromAi:false removes
// the path from the fromAi metadata while the value stays equal to the expression.
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
