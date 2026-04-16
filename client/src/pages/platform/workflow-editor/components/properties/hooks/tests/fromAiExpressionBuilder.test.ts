import {describe, expect, it} from 'vitest';

/**
 * Replicates the fromAiExpression builder from useProperty.ts. The builder used
 * to embed any non-empty `defaultValue` into the resulting `=fromAi(...)` call,
 * which caused recursive nesting inside array/object items: their synthesized
 * `defaultValue` is the current stored value, so after saving a fromAi expression
 * the next click would escape-and-embed the previous output as a new defaultValue.
 *
 * The builder now skips expression-shaped defaults (anything starting with `=`),
 * so repeat clicks stay idempotent.
 */

interface BuildFromAiExpressionArgsI {
    arrayName?: string;
    defaultValue?: unknown;
    description?: string;
    formattedOptions?: Array<{value?: string | null}> | null;
    name?: string;
    required?: boolean;
    type?: string;
}

function buildFromAiExpression({
    arrayName,
    defaultValue,
    description,
    formattedOptions,
    name,
    required = false,
    type,
}: BuildFromAiExpressionArgsI): string {
    const mapEntries: string[] = [];

    if (description) {
        mapEntries.push(`'description': '${description.replace(/'/g, "''")}'`);
    }

    if (defaultValue !== '' && defaultValue !== null && defaultValue !== undefined) {
        const defaultValueString = String(defaultValue);

        if (!defaultValueString.startsWith('=')) {
            const escapedDefault = defaultValueString.replace(/'/g, "''");

            mapEntries.push(`'defaultValue': '${escapedDefault}'`);
        }
    }

    if (formattedOptions != null && formattedOptions.length > 0) {
        const optionValues = formattedOptions
            .map((option) => `'${String(option?.value ?? '').replace(/'/g, "''")}'`)
            .join(', ');

        mapEntries.push(`'options': {${optionValues}}`);
    }

    mapEntries.push(`'required': ${required}`);

    const qualifiedName = arrayName ? `${arrayName}_${name}` : name;

    return `=fromAi('${qualifiedName}', '${type}', {${mapEntries.join(', ')}})`;
}

describe('fromAiExpression builder', () => {
    it('produces the canonical expression for a simple property', () => {
        expect(buildFromAiExpression({name: 'lastname', required: false, type: 'STRING'})).toBe(
            "=fromAi('lastname', 'STRING', {'required': false})"
        );
    });

    it('includes literal default values', () => {
        expect(buildFromAiExpression({defaultValue: 'Smith', name: 'lastname', required: true, type: 'STRING'})).toBe(
            "=fromAi('lastname', 'STRING', {'defaultValue': 'Smith', 'required': true})"
        );
    });

    it('escapes single quotes inside literal defaults', () => {
        expect(buildFromAiExpression({defaultValue: "O'Hara", name: 'lastname', required: false, type: 'STRING'})).toBe(
            "=fromAi('lastname', 'STRING', {'defaultValue': 'O''Hara', 'required': false})"
        );
    });

    it('omits defaultValue for empty strings', () => {
        expect(buildFromAiExpression({defaultValue: '', name: 'lastname', required: false, type: 'STRING'})).toBe(
            "=fromAi('lastname', 'STRING', {'required': false})"
        );
    });

    describe('expression-shaped defaults', () => {
        it('does not embed a previous fromAi expression as defaultValue', () => {
            const priorExpression = "=fromAi('0', 'STRING', {'required': false})";

            expect(
                buildFromAiExpression({
                    defaultValue: priorExpression,
                    name: '0',
                    required: false,
                    type: 'STRING',
                })
            ).toBe("=fromAi('0', 'STRING', {'required': false})");
        });

        it('stays idempotent across repeated builds after a prior fromAi save', () => {
            const baseArgs = {name: '0', required: false, type: 'STRING'} as const;
            const first = buildFromAiExpression(baseArgs);

            // Simulates the re-render after save: array items synthesize defaultValue
            // from the stored parameter value, which is now the prior fromAi output.
            const second = buildFromAiExpression({...baseArgs, defaultValue: first});
            const third = buildFromAiExpression({...baseArgs, defaultValue: second});

            expect(second).toBe(first);
            expect(third).toBe(first);
        });

        it('skips any expression reference, not just fromAi (e.g. data pill)', () => {
            expect(
                buildFromAiExpression({
                    defaultValue: '=${trigger_1.firstName}',
                    name: 'firstName',
                    required: false,
                    type: 'STRING',
                })
            ).toBe("=fromAi('firstName', 'STRING', {'required': false})");
        });
    });

    describe('array item naming', () => {
        it('qualifies array item identifiers with the parent array name', () => {
            expect(
                buildFromAiExpression({
                    arrayName: 'messages',
                    name: '0',
                    required: false,
                    type: 'STRING',
                })
            ).toBe("=fromAi('messages_0', 'STRING', {'required': false})");
        });

        it('leaves non-array property names unchanged', () => {
            expect(
                buildFromAiExpression({
                    name: 'lastname',
                    required: false,
                    type: 'STRING',
                })
            ).toBe("=fromAi('lastname', 'STRING', {'required': false})");
        });
    });
});
