import {Workflow} from '@/shared/middleware/automation/configuration';
import {renderHook} from '@testing-library/react';
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
