import {beforeEach, describe, expect, it} from 'vitest';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import getFormattedName from './getFormattedName';

function setWorkflowState(definition: Record<string, unknown>, nodeNames: string[] = []) {
    useWorkflowDataStore.setState({
        nodes: nodeNames.map((name) => ({data: {name}, id: name, position: {x: 0, y: 0}})),
        workflow: {definition: JSON.stringify(definition), id: 'workflow-1'},
    } as unknown as Parameters<typeof useWorkflowDataStore.setState>[0]);
}

describe('getFormattedName', () => {
    beforeEach(() => {
        setWorkflowState({tasks: [], triggers: []});
    });

    it('numbers the first node of a component 1', () => {
        expect(getFormattedName('httpClient')).toBe('httpClient_1');
    });

    it('camelCases a hyphenated component name', () => {
        expect(getFormattedName('fork-join')).toBe('forkJoin_1');
        expect(getFormattedName('on-error')).toBe('onError_1');
    });

    it('numbers past an existing camelCased dispatcher node', () => {
        setWorkflowState(
            {tasks: [{name: 'forkJoin_1', parameters: {branches: []}, type: 'fork-join/v1'}], triggers: []},
            ['forkJoin_1']
        );

        expect(getFormattedName('fork-join')).toBe('forkJoin_2');
    });

    it('ignores a renamed node whose name does not end in a number', () => {
        setWorkflowState({tasks: [], triggers: []}, ['dataStorageResetCounter']);

        expect(getFormattedName('dataStorage')).toBe('dataStorage_1');
    });

    it('numbers past the numbered nodes when a renamed node of the component exists', () => {
        setWorkflowState({tasks: [], triggers: []}, ['dataStorage_2', 'dataStorageResetCounter']);

        expect(getFormattedName('dataStorage')).toBe('dataStorage_3');
    });

    it('ignores a numbered name that only contains the prefix', () => {
        setWorkflowState({tasks: [], triggers: []}, ['olddataStorage_7']);

        expect(getFormattedName('dataStorage')).toBe('dataStorage_1');
    });

    it('ignores a renamed cluster element and a reserved name without a number', () => {
        setWorkflowState({
            tasks: [
                {
                    clusterElements: {tools: [{name: 'httpClientLookup', type: 'httpClient/v1/get'}]},
                    name: 'aiAgent_1',
                    type: 'aiAgent/v1/chat',
                },
            ],
            triggers: [],
        });

        expect(getFormattedName('httpClient', new Set(['httpClientDraft', 'httpClient_4']))).toBe('httpClient_5');
    });
});
