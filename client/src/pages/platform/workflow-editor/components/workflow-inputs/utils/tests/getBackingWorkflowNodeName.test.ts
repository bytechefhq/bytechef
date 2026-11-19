import {getBackingWorkflowNodeName} from '@/pages/platform/workflow-editor/components/workflow-inputs/utils/getBackingWorkflowNodeName';
import {Workflow} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

const workflow = {
    definition: JSON.stringify({
        tasks: [{name: 'sendMessage_1', type: 'slack/v2/sendMessage'}],
        triggers: [{name: 'newMessage_1', type: 'slack/v2/newMessage'}],
    }),
} as Workflow;

describe('getBackingWorkflowNodeName', () => {
    it('returns the first task node using the component', () => {
        expect(getBackingWorkflowNodeName(workflow, 'slack', 2)).toBe('sendMessage_1');
    });

    it('falls back to a trigger node when no task uses the component', () => {
        const triggerOnly = {
            definition: JSON.stringify({tasks: [], triggers: [{name: 'newMessage_1', type: 'slack/v2/newMessage'}]}),
        } as Workflow;

        expect(getBackingWorkflowNodeName(triggerOnly, 'slack', 2)).toBe('newMessage_1');
    });

    it('returns undefined when no node uses the component', () => {
        expect(getBackingWorkflowNodeName(workflow, 'googleSheets', 1)).toBeUndefined();
    });

    it('returns undefined for a missing component name, version or definition', () => {
        expect(getBackingWorkflowNodeName(workflow, undefined, 2)).toBeUndefined();
        expect(getBackingWorkflowNodeName(workflow, 'slack', undefined)).toBeUndefined();
        expect(getBackingWorkflowNodeName({} as Workflow, 'slack', 2)).toBeUndefined();
    });

    it('finds a node nested in a task dispatcher', () => {
        const nestedWorkflow = {
            definition: JSON.stringify({
                tasks: [
                    {
                        name: 'loop_1',
                        parameters: {
                            iteratee: [
                                {
                                    name: 'parallel_1',
                                    parameters: {tasks: [{name: 'sendMessage_1', type: 'slack/v2/sendMessage'}]},
                                    type: 'parallel/v1',
                                },
                            ],
                        },
                        type: 'loop/v1',
                    },
                ],
                triggers: [],
            }),
        } as Workflow;

        expect(getBackingWorkflowNodeName(nestedWorkflow, 'slack', 2)).toBe('sendMessage_1');
    });

    it('matches the node of the requested component version', () => {
        const twoVersionsWorkflow = {
            definition: JSON.stringify({
                tasks: [
                    {name: 'sendMessage_1', type: 'slack/v2/sendMessage'},
                    {name: 'sendMessage_2', type: 'slack/v1/sendMessage'},
                ],
            }),
        } as Workflow;

        expect(getBackingWorkflowNodeName(twoVersionsWorkflow, 'slack', 1)).toBe('sendMessage_2');
        expect(getBackingWorkflowNodeName(twoVersionsWorkflow, 'slack', 2)).toBe('sendMessage_1');
        expect(getBackingWorkflowNodeName(twoVersionsWorkflow, 'slack', 3)).toBeUndefined();
    });
});
