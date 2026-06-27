import {EvaluatorFunctionDefinition} from '@/shared/middleware/graphql';
import {describe, expect, it} from 'vitest';

import {FROM_AI_FUNCTION_DEFINITION, buildToolFunctionDefinitions} from '../fromAiFunctionDefinition';

const baseDefinition = {
    category: 'STRING',
    description: 'Concatenates.',
    example: '=concat(a, b)',
    name: 'concat',
    parameters: [],
    returnType: 'STRING',
    title: 'concat',
} as unknown as EvaluatorFunctionDefinition;

describe('buildToolFunctionDefinitions', () => {
    it('returns the base catalog unchanged for non-tool properties', () => {
        const result = buildToolFunctionDefinitions([baseDefinition], false);

        expect(result).toEqual([baseDefinition]);
        expect(result.some((definition) => definition.name === 'fromAi')).toBe(false);
    });

    it('prepends the fromAi definition for tool properties', () => {
        const result = buildToolFunctionDefinitions([baseDefinition], true);

        expect(result[0]).toBe(FROM_AI_FUNCTION_DEFINITION);
        expect(result).toHaveLength(2);
    });

    it('does not duplicate fromAi when it is already present', () => {
        const result = buildToolFunctionDefinitions([FROM_AI_FUNCTION_DEFINITION, baseDefinition], true);

        expect(result.filter((definition) => definition.name === 'fromAi')).toHaveLength(1);
    });
});

describe('FROM_AI_FUNCTION_DEFINITION', () => {
    it('is named fromAi and returns a string', () => {
        expect(FROM_AI_FUNCTION_DEFINITION.name).toBe('fromAi');
        expect(FROM_AI_FUNCTION_DEFINITION.returnType).toBe('STRING');
    });
});
