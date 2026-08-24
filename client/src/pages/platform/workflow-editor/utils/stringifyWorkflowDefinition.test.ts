import {describe, expect, it} from 'vitest';

import stringifyWorkflowDefinition from './stringifyWorkflowDefinition';

describe('stringifyWorkflowDefinition', () => {
    it('should keep a map shaped connections declaration', () => {
        const definition = {
            tasks: [
                {
                    connections: {gm: {componentName: 'googleMail', componentVersion: 1}},
                    name: 'script_1',
                    type: 'script/v1/javascript',
                },
            ],
        };

        expect(JSON.parse(stringifyWorkflowDefinition(definition))).toEqual(definition);
    });

    it('should drop an array shaped connections declaration from a task', () => {
        const definition = {
            tasks: [{connections: [], name: 'condition_1', parameters: {}, type: 'condition/v1'}],
        };

        expect(JSON.parse(stringifyWorkflowDefinition(definition))).toEqual({
            tasks: [{name: 'condition_1', parameters: {}, type: 'condition/v1'}],
        });
    });

    it('should drop an array shaped connections declaration from a nested subtask', () => {
        const definition = {
            tasks: [
                {
                    name: 'condition_1',
                    parameters: {
                        caseTrue: [
                            {
                                connections: [],
                                finalize: [],
                                name: 'condition_2',
                                parameters: {caseTrue: []},
                                post: [],
                                pre: [],
                                type: 'condition/v1',
                            },
                        ],
                    },
                    type: 'condition/v1',
                },
            ],
        };

        const result = JSON.parse(stringifyWorkflowDefinition(definition));

        expect(result.tasks[0].parameters.caseTrue[0]).toEqual({
            finalize: [],
            name: 'condition_2',
            parameters: {caseTrue: []},
            post: [],
            pre: [],
            type: 'condition/v1',
        });
    });

    it('should drop a non-empty array shaped connections declaration', () => {
        const definition = {
            tasks: [
                {
                    connections: [
                        {
                            componentName: 'googleMail',
                            componentVersion: 1,
                            key: 'gm',
                            required: false,
                            workflowNodeName: 'script_1',
                        },
                    ],
                    name: 'script_1',
                    type: 'script/v1/javascript',
                },
            ],
        };

        expect(JSON.parse(stringifyWorkflowDefinition(definition))).toEqual({
            tasks: [{name: 'script_1', type: 'script/v1/javascript'}],
        });
    });

    it('should leave a connections array that is component parameter data untouched', () => {
        const definition = {
            tasks: [
                {
                    name: 'httpClient_1',
                    parameters: {body: {connections: ['a', 'b']}},
                    type: 'httpClient/v1/post',
                },
            ],
        };

        expect(JSON.parse(stringifyWorkflowDefinition(definition))).toEqual(definition);
    });

    it('should indent with the shared workflow definition spacing', () => {
        expect(stringifyWorkflowDefinition({tasks: []})).toBe('{\n    "tasks": []\n}');
    });
});
