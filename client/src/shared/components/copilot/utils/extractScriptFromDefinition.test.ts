import {describe, expect, it} from 'vitest';

import {extractScriptFromDefinition} from './extractScriptFromDefinition';

describe('extractScriptFromDefinition', () => {
    it('returns the full script for a top-level task by node name', () => {
        const definition = JSON.stringify({
            tasks: [
                {name: 'other_1', parameters: {script: 'return 1;'}},
                {name: 'script_1', parameters: {script: 'var a = 1;\nreturn a;'}},
            ],
        });

        expect(extractScriptFromDefinition(definition, 'script_1')).toBe('var a = 1;\nreturn a;');
    });

    it('finds a script nested inside a dispatcher task', () => {
        const definition = JSON.stringify({
            tasks: [
                {
                    name: 'condition_1',
                    parameters: {
                        caseTrue: [{name: 'script_1', parameters: {script: 'return true;'}}],
                    },
                },
            ],
        });

        expect(extractScriptFromDefinition(definition, 'script_1')).toBe('return true;');
    });

    it('returns null when the node is not present', () => {
        const definition = JSON.stringify({tasks: [{name: 'script_1', parameters: {script: 'x'}}]});

        expect(extractScriptFromDefinition(definition, 'missing_1')).toBeNull();
    });

    it('returns null when the matched node has no script parameter', () => {
        const definition = JSON.stringify({tasks: [{name: 'script_1', parameters: {language: 'javascript'}}]});

        expect(extractScriptFromDefinition(definition, 'script_1')).toBeNull();
    });

    it('returns null when the definition is not valid JSON', () => {
        expect(extractScriptFromDefinition('not json {', 'script_1')).toBeNull();
    });
});
