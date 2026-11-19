import {getWorkflowComponentVersions} from '@/pages/platform/workflow-editor/components/workflow-inputs/utils/getWorkflowComponentVersions';
import {Workflow} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

describe('getWorkflowComponentVersions', () => {
    it('maps component names to versions from tasks and triggers', () => {
        const workflow = {
            definition: JSON.stringify({
                tasks: [{name: 'sendMessage_1', type: 'slack/v2/sendMessage'}],
                triggers: [{name: 'newMessage_1', type: 'webhook/v1/newMessage'}],
            }),
        } as Workflow;

        expect(getWorkflowComponentVersions(workflow)).toEqual({slack: 2, webhook: 1});
    });

    it('returns an empty object when definition is missing', () => {
        const workflow = {} as Workflow;

        expect(getWorkflowComponentVersions(workflow)).toEqual({});
    });

    it('returns an empty object when definition is unparseable', () => {
        const workflow = {definition: 'not json'} as Workflow;

        expect(getWorkflowComponentVersions(workflow)).toEqual({});
    });

    it('skips node types without a v-prefixed version token', () => {
        const workflow = {
            definition: JSON.stringify({
                tasks: [
                    {name: 'sendMessage_1', type: 'slack/2/sendMessage'},
                    {name: 'logger_1', type: 'logger'},
                ],
            }),
        } as Workflow;

        expect(getWorkflowComponentVersions(workflow)).toEqual({});
    });

    it('skips node types with a non-numeric version token', () => {
        const workflow = {
            definition: JSON.stringify({
                tasks: [{name: 'sendMessage_1', type: 'slack/vX/sendMessage'}],
            }),
        } as Workflow;

        expect(getWorkflowComponentVersions(workflow)).toEqual({});
    });

    it('skips nodes without a type', () => {
        const workflow = {
            definition: JSON.stringify({
                tasks: [{name: 'noType'}, {name: 'sendMessage_1', type: 'slack/v3/sendMessage'}],
            }),
        } as Workflow;

        expect(getWorkflowComponentVersions(workflow)).toEqual({slack: 3});
    });

    it('returns an empty object when tasks is not an array', () => {
        const workflow = {
            definition: JSON.stringify({tasks: {name: 'sendMessage_1', type: 'slack/v2/sendMessage'}}),
        } as Workflow;

        expect(getWorkflowComponentVersions(workflow)).toEqual({});
    });

    it('collects versions from tasks nested in task dispatcher parameters', () => {
        const workflow = {
            definition: JSON.stringify({
                tasks: [
                    {
                        name: 'loop_1',
                        parameters: {
                            iteratee: [
                                {
                                    name: 'condition_1',
                                    parameters: {
                                        caseFalse: [],
                                        caseTrue: [{name: 'sendMessage_1', type: 'slack/v2/sendMessage'}],
                                    },
                                    type: 'condition/v1',
                                },
                            ],
                        },
                        type: 'loop/v1',
                    },
                    {
                        name: 'branch_1',
                        parameters: {
                            cases: [{key: 'first', tasks: [{name: 'getRows_1', type: 'googleSheets/v1/getRows'}]}],
                        },
                        type: 'branch/v1',
                    },
                ],
            }),
        } as Workflow;

        expect(getWorkflowComponentVersions(workflow)).toEqual({
            branch: 1,
            condition: 1,
            googleSheets: 1,
            loop: 1,
            slack: 2,
        });
    });

    it('keeps the highest version when a component is used in more than one version', () => {
        const workflow = {
            definition: JSON.stringify({
                tasks: [
                    {name: 'sendMessage_1', type: 'slack/v2/sendMessage'},
                    {name: 'sendMessage_2', type: 'slack/v1/sendMessage'},
                ],
            }),
        } as Workflow;

        expect(getWorkflowComponentVersions(workflow)).toEqual({slack: 2});
    });
});
