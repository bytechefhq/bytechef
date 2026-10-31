import {Workflow} from '@/shared/middleware/automation/configuration';
import {renderHook, waitFor} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {
    WorkflowExecutionOverlayContext,
    WorkflowExecutionOverlayI,
} from '../../providers/workflowExecutionOverlayContext';
import useWorkflowDataStore from '../../stores/useWorkflowDataStore';
import useLayout from '../useLayout';

const {clusterRootIdsArguments} = vi.hoisted(() => ({clusterRootIdsArguments: [] as string[][]}));

vi.mock('../../../cluster-element-editor/hooks/useClusterElementNodes', () => ({
    default: (clusterRootIds: string[]) => {
        clusterRootIdsArguments.push(clusterRootIds);

        return {definitionsReady: true, edgesByRootId: {}, nodesByRootId: {}};
    },
}));

const READ_ONLY_WORKFLOW = {
    definition: JSON.stringify({
        tasks: [{name: 'var_1', type: 'var/v1/set'}],
        triggers: [{name: 'trigger_1', type: 'manual/v1/manual'}],
    }),
    id: 'workflow-1',
    tasks: [{name: 'var_1', type: 'var/v1/set'}],
    triggers: [{name: 'trigger_1', type: 'manual/v1/manual'}],
} as Workflow;

function wrapper({children}: {children: ReactNode}) {
    return (
        <WorkflowExecutionOverlayContext.Provider value={{} as WorkflowExecutionOverlayI}>
            {children}
        </WorkflowExecutionOverlayContext.Provider>
    );
}

describe('useLayout on a read-only canvas', () => {
    beforeEach(() => {
        clusterRootIdsArguments.length = 0;

        useWorkflowDataStore.setState({edges: [], isWorkflowLoaded: true, layoutResetCounter: 0, nodes: []});
    });

    it('draws the read-only workflow even when the editor store still holds another workflow', async () => {
        useWorkflowDataStore.setState({
            workflow: {
                ...useWorkflowDataStore.getState().workflow,
                tasks: [{name: 'otherWorkflowTask_1', type: 'var/v1/set'}],
                triggers: [{name: 'otherWorkflowTrigger_1', type: 'manual/v1/manual'}],
            },
        });

        renderHook(
            () =>
                useLayout({
                    canvasHeight: 800,
                    canvasWidth: 1200,
                    componentDefinitions: [],
                    readOnlyWorkflow: READ_ONLY_WORKFLOW,
                    taskDispatcherDefinitions: [],
                }),
            {wrapper}
        );

        await waitFor(
            () => {
                const nodeIds = useWorkflowDataStore.getState().nodes.map((node) => node.id);

                expect(nodeIds).toContain('var_1');
                expect(nodeIds).toContain('trigger_1');
                expect(nodeIds).not.toContain('otherWorkflowTask_1');
                expect(nodeIds).not.toContain('otherWorkflowTrigger_1');
            },
            {interval: 5, timeout: 5000}
        );
    });

    it('keeps the framed root ids when the read-only workflow is a new object with the same tasks', () => {
        const {rerender} = renderHook(
            () =>
                useLayout({
                    canvasHeight: 800,
                    canvasWidth: 1200,
                    componentDefinitions: [],
                    readOnlyWorkflow: {...READ_ONLY_WORKFLOW},
                    taskDispatcherDefinitions: [],
                }),
            {wrapper}
        );

        rerender();
        rerender();

        expect(new Set(clusterRootIdsArguments).size).toBe(1);
    });
});
