import {describe, expect, it} from 'vitest';

import collectClusterElementsSignature from './collectClusterElementsSignature';

describe('collectClusterElementsSignature', () => {
    it('returns an empty string for primitives and empty values', () => {
        expect(collectClusterElementsSignature(undefined)).toBe('');
        expect(collectClusterElementsSignature(null)).toBe('');
        expect(collectClusterElementsSignature('text')).toBe('');
        expect(collectClusterElementsSignature(5)).toBe('');
    });

    it('describes an element by name, type, label and position', () => {
        expect(
            collectClusterElementsSignature({
                label: 'Model',
                metadata: {ui: {nodePosition: {x: 10, y: 20}}},
                name: 'model_1',
                type: 'openAi/v1/model',
            })
        ).toBe('model_1:openAi/v1/model:Model@10,20');
    });

    it('omits a missing label and position', () => {
        expect(collectClusterElementsSignature({name: 'model_1', type: 'openAi/v1/model'})).toBe(
            'model_1:openAi/v1/model:@'
        );
    });

    it('includes a zero position', () => {
        expect(
            collectClusterElementsSignature({
                metadata: {ui: {nodePosition: {x: 0, y: 0}}},
                name: 'model_1',
                type: 'openAi/v1/model',
            })
        ).toBe('model_1:openAi/v1/model:@0,0');
    });

    it('ignores a non-string label', () => {
        expect(collectClusterElementsSignature({label: 3, name: 'model_1', type: 'openAi/v1/model'})).toBe(
            'model_1:openAi/v1/model:@'
        );
    });

    it('appends nested cluster elements in brackets', () => {
        expect(
            collectClusterElementsSignature({
                clusterElements: {model: {name: 'model_2', type: 'openAi/v1/model'}},
                name: 'agent_1',
                type: 'aiAgent/v1/tool',
            })
        ).toBe('agent_1:aiAgent/v1/tool:@[model=model_2:openAi/v1/model:@]');
    });

    it('omits brackets when nested cluster elements are empty', () => {
        expect(collectClusterElementsSignature({clusterElements: {}, name: 'agent_1', type: 'aiAgent/v1/tool'})).toBe(
            'agent_1:aiAgent/v1/tool:@'
        );
    });

    it('joins array items with commas', () => {
        expect(
            collectClusterElementsSignature([
                {name: 'tool_1', type: 'a/v1/b'},
                {name: 'tool_2', type: 'a/v1/c'},
            ])
        ).toBe('tool_1:a/v1/b:@,tool_2:a/v1/c:@');
    });

    it('sorts record keys so the signature does not depend on key order', () => {
        const firstOrder = {
            model: {name: 'model_1', type: 'a/v1/model'},
            tools: [{name: 'tool_1', type: 'a/v1/tool'}],
        };
        const secondOrder = {
            tools: [{name: 'tool_1', type: 'a/v1/tool'}],
            // eslint-disable-next-line sort-keys
            model: {name: 'model_1', type: 'a/v1/model'},
        };

        expect(collectClusterElementsSignature(firstOrder)).toBe('model=model_1:a/v1/model:@|tools=tool_1:a/v1/tool:@');
        expect(collectClusterElementsSignature(secondOrder)).toBe(collectClusterElementsSignature(firstOrder));
    });

    it('changes when a nested position changes', () => {
        const before = {
            tools: [{metadata: {ui: {nodePosition: {x: 1, y: 1}}}, name: 'tool_1', type: 'a/v1/tool'}],
        };
        const after = {
            tools: [{metadata: {ui: {nodePosition: {x: 2, y: 1}}}, name: 'tool_1', type: 'a/v1/tool'}],
        };

        expect(collectClusterElementsSignature(before)).not.toBe(collectClusterElementsSignature(after));
    });

    it('treats a record without a string name and type as a container', () => {
        expect(collectClusterElementsSignature({name: 'x', type: 3})).toBe('name=|type=');
    });
});
