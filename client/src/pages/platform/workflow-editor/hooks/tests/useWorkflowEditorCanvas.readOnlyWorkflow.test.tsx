import {Workflow} from '@/shared/middleware/automation/configuration';
import {renderHook} from '@testing-library/react';
import {ReactFlowProvider} from '@xyflow/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowDataStore from '../../stores/useWorkflowDataStore';
import useWorkflowEditorCanvas from '../useWorkflowEditorCanvas';

const {useLayoutMock} = vi.hoisted(() => ({
    useLayoutMock: vi.fn<(props: {readOnlyWorkflow?: Workflow}) => {autoPlacedGraphPositionsRef: {current: object}}>(
        () => ({autoPlacedGraphPositionsRef: {current: {}}})
    ),
}));

vi.mock('../useLayout', () => ({default: useLayoutMock}));

vi.mock('../useHandleDrop', () => ({
    default: () => [vi.fn(), vi.fn(), vi.fn(), vi.fn()],
}));

vi.mock('../useStickyNotes', () => ({
    default: () => ({handleAddStickyNote: vi.fn()}),
}));

vi.mock('../../providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({updateWorkflowMutation: undefined}),
}));

const READ_ONLY_WORKFLOW = {
    definition: JSON.stringify({tasks: [{name: 'var_1', type: 'var/v1/set'}]}),
    id: 'executed-workflow',
    tasks: [{name: 'var_1', type: 'var/v1/set'}],
    triggers: [{name: 'trigger_1', type: 'manual/v1/manual'}],
} as Workflow;

function renderReadOnlyCanvas() {
    return renderHook(
        () =>
            useWorkflowEditorCanvas({
                componentDefinitions: [],
                readOnlyWorkflow: READ_ONLY_WORKFLOW,
                taskDispatcherDefinitions: [],
            }),
        {wrapper: ({children}: {children: ReactNode}) => <ReactFlowProvider>{children}</ReactFlowProvider>}
    );
}

function lastLaidOutReadOnlyWorkflow(): Workflow | undefined {
    return useLayoutMock.mock.lastCall?.[0].readOnlyWorkflow;
}

describe('useWorkflowEditorCanvas on a read-only canvas', () => {
    beforeEach(() => {
        useLayoutMock.mockClear();

        useWorkflowDataStore.getState().reset();
    });

    it('lays out the read-only workflow when no editor workflow is loaded', () => {
        renderReadOnlyCanvas();

        expect(lastLaidOutReadOnlyWorkflow()).toMatchObject({id: 'executed-workflow', tasks: READ_ONLY_WORKFLOW.tasks});
    });

    it('lays out the read-only workflow, not the editor workflow loaded in the store', () => {
        useWorkflowDataStore.setState({
            isWorkflowLoaded: true,
            workflow: {
                id: 'editor-workflow',
                nodeNames: ['trigger_1', 'draftTask_1'],
                tasks: [{name: 'draftTask_1', type: 'var/v1/set'}],
                triggers: [{name: 'trigger_1', type: 'manual/v1/manual'}],
                workflowUuid: 'editor-workflow-uuid',
            },
        });

        renderReadOnlyCanvas();

        expect(lastLaidOutReadOnlyWorkflow()).toMatchObject({id: 'executed-workflow', tasks: READ_ONLY_WORKFLOW.tasks});
    });

    it('passes the same read-only workflow object across renders, so the layout does not rerun', () => {
        const {rerender} = renderReadOnlyCanvas();

        const firstReadOnlyWorkflow = lastLaidOutReadOnlyWorkflow();

        rerender();

        expect(lastLaidOutReadOnlyWorkflow()).toBe(firstReadOnlyWorkflow);
    });
});
