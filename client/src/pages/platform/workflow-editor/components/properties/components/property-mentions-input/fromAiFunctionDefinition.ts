import {
    EvaluatorFunctionCategory,
    EvaluatorFunctionDefinition,
    EvaluatorFunctionType,
} from '@/shared/middleware/graphql';

// Synthetic catalog entry so `fromAi` appears in the function-autocomplete dropdown for tool
// properties. It is NOT a real evaluator function (the server catalog has no `fromAi`); it exists
// purely as a UI affordance and behaves like any other function when selected (inserts `fromAi()`).
export const FROM_AI_FUNCTION_DEFINITION: EvaluatorFunctionDefinition = {
    category: EvaluatorFunctionCategory.Utility,
    description: 'Let the AI model supply this value at runtime.',
    example: "=fromAi('name', 'STRING', {'required': true})",
    name: 'fromAi',
    parameters: [
        {
            description: 'Identifier the model sees for this value.',
            name: 'name',
            required: true,
            type: EvaluatorFunctionType.String,
        },
        {
            description: 'Value type, for example STRING.',
            name: 'type',
            required: true,
            type: EvaluatorFunctionType.String,
        },
        {
            description: 'Optional metadata: description, defaultValue, options, required.',
            name: 'options',
            required: false,
            type: EvaluatorFunctionType.Map,
        },
    ],
    returnType: EvaluatorFunctionType.String,
    title: 'fromAi',
};

// The function-suggestion catalog for the editor: the base evaluator definitions, plus the synthetic
// `fromAi` entry when the field is a tool property. fromAi is prepended so it surfaces first when the
// user types "fr...".
export function buildToolFunctionDefinitions(
    definitions: EvaluatorFunctionDefinition[],
    toolProperty: boolean
): EvaluatorFunctionDefinition[] {
    if (!toolProperty) {
        return definitions;
    }

    if (definitions.some((definition) => definition.name === 'fromAi')) {
        return definitions;
    }

    return [FROM_AI_FUNCTION_DEFINITION, ...definitions];
}
