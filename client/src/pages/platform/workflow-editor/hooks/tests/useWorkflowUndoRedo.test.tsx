import {act, renderHook} from '@testing-library/react';
import {ReactNode} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {WorkflowEditorProvider, WorkflowEditorStateI} from '../../providers/workflowEditorProvider';
import useLayoutDirectionStore from '../../stores/useLayoutDirectionStore';
import useWorkflowDataStore, {setWorkflowWithoutHistory} from '../../stores/useWorkflowDataStore';
import {extractLayoutDirection} from '../../utils/layoutDirectionDefinitionUtils';
import {clearAllWorkflowMutations} from '../../utils/workflowMutationGuard';
import useWorkflowUndoRedo from '../useWorkflowUndoRedo';

const makeDefinition = (taskNames: Array<string>, layoutDirection?: string) =>
    JSON.stringify({
        label: 'Workflow',
        tasks: taskNames.map((name) => ({name, type: 'component/v1/action'})),
        ...(layoutDirection ? {metadata: {ui: {layoutDirection}}} : {}),
    });

describe('useWorkflowUndoRedo', () => {
    const mutateMock = vi.fn();

    const wrapper = ({children}: {children: ReactNode}) => (
        <WorkflowEditorProvider
            value={
                {
                    invalidateWorkflowQueries: vi.fn(),
                    updateWorkflowMutation: {isPending: false, mutate: mutateMock},
                } as unknown as WorkflowEditorStateI
            }
        >
            {children}
        </WorkflowEditorProvider>
    );

    const commitHistoryEntry = () => vi.advanceTimersByTime(200);

    beforeEach(() => {
        vi.useFakeTimers();

        clearAllWorkflowMutations();
        mutateMock.mockReset();

        useLayoutDirectionStore.setState({
            currentWorkflowUuid: 'workflow-uuid-1',
            directionsByWorkflowUuid: {},
            layoutDirection: 'TB',
        });

        setWorkflowWithoutHistory(
            {
                ...useWorkflowDataStore.getState().workflow,
                definition: makeDefinition(['task_1']),
                id: 'workflow_1',
                version: 1,
            },
            {clearHistory: true}
        );

        useWorkflowDataStore.setState((state) => ({
            workflow: {...state.workflow, definition: makeDefinition(['task_1', 'task_2'])},
        }));

        commitHistoryEntry();
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it('keeps the current layout direction when undoing to a snapshot saved before it was chosen', () => {
        useLayoutDirectionStore.setState({layoutDirection: 'LR'});

        useWorkflowDataStore.setState((state) => ({
            workflow: {...state.workflow, definition: makeDefinition(['task_1', 'task_2'], 'LR')},
        }));

        commitHistoryEntry();

        const {result} = renderHook(() => useWorkflowUndoRedo(), {wrapper});

        act(() => {
            result.current.handleUndo();
        });

        const storedDefinition = useWorkflowDataStore.getState().workflow.definition!;

        expect(JSON.parse(storedDefinition).tasks.map((task: {name: string}) => task.name)).toEqual([
            'task_1',
            'task_2',
        ]);
        expect(extractLayoutDirection(storedDefinition)).toBe('LR');
        expect(mutateMock).toHaveBeenCalledTimes(1);
        expect(mutateMock.mock.calls[0][0].workflow.definition).toBe(storedDefinition);
    });

    it('stamps the current direction onto an undone snapshot that has none', () => {
        useLayoutDirectionStore.setState({layoutDirection: 'LR'});

        const {result} = renderHook(() => useWorkflowUndoRedo(), {wrapper});

        act(() => {
            result.current.handleUndo();
        });

        const storedDefinition = useWorkflowDataStore.getState().workflow.definition!;

        expect(JSON.parse(storedDefinition).tasks).toHaveLength(1);
        expect(extractLayoutDirection(storedDefinition)).toBe('LR');
        expect(mutateMock.mock.calls[0][0].workflow.definition).toBe(storedDefinition);
    });

    it('persists the undone snapshot unchanged when it already matches the direction', () => {
        const {result} = renderHook(() => useWorkflowUndoRedo(), {wrapper});

        act(() => {
            result.current.handleUndo();
        });

        expect(useWorkflowDataStore.getState().workflow.definition).toBe(makeDefinition(['task_1']));
        expect(mutateMock.mock.calls[0][0].workflow.definition).toBe(makeDefinition(['task_1']));
    });
});
