import useWorkflowIssuesSweep from '@/pages/platform/workflow-editor/hooks/useWorkflowIssuesSweep';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowIssuesStore from '@/pages/platform/workflow-editor/stores/useWorkflowIssuesStore';
import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

describe('useWorkflowIssuesSweep', () => {
    beforeEach(() => {
        useWorkflowIssuesStore.setState({sweepIssues: []});
    });

    it('reports nothing for a workflow whose references all resolve', () => {
        useWorkflowDataStore.setState({
            workflow: {
                inputs: [],
                tasks: [{label: 'One', name: 'task_1', parameters: {}, type: 'acme/v1/one'}],
                triggers: [],
            },
        } as never);

        renderHook(() => useWorkflowIssuesSweep());

        expect(useWorkflowIssuesStore.getState().sweepIssues).toEqual([]);
    });

    it('reports a reference to a node the workflow does not have', () => {
        useWorkflowDataStore.setState({
            workflow: {
                inputs: [],
                tasks: [{label: 'One', name: 'task_1', parameters: {value: '${missing_1.id}'}, type: 'acme/v1/one'}],
                triggers: [],
            },
        } as never);

        renderHook(() => useWorkflowIssuesSweep());

        const sweepIssues = useWorkflowIssuesStore.getState().sweepIssues;

        expect(sweepIssues).toHaveLength(1);
        expect(sweepIssues[0].nodeName).toBe('task_1');
        expect(sweepIssues[0].kind).toBe('BROKEN_REFERENCE');
    });

    it('reports a duplicate node name', () => {
        useWorkflowDataStore.setState({
            workflow: {
                inputs: [],
                tasks: [
                    {label: 'One', name: 'task_1', parameters: {}, type: 'acme/v1/one'},
                    {label: 'Two', name: 'task_1', parameters: {}, type: 'acme/v1/two'},
                ],
                triggers: [],
            },
        } as never);

        renderHook(() => useWorkflowIssuesSweep());

        expect(useWorkflowIssuesStore.getState().sweepIssues[0].kind).toBe('DUPLICATE_NODE_NAME');
    });
});
