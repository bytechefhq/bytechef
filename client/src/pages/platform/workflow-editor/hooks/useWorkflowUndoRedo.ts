import {useCallback} from 'react';
import {useShallow} from 'zustand/react/shallow';

import {useWorkflowEditor} from '../providers/workflowEditorProvider';
import useWorkflowDataStore, {
    setWorkflowWithoutHistory,
    useWorkflowTemporalStore,
} from '../stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import {isWorkflowMutating, setWorkflowMutating} from '../utils/workflowMutationGuard';

interface UseWorkflowUndoRedoReturnI {
    canRedo: boolean;
    canUndo: boolean;
    handleRedo: () => void;
    handleUndo: () => void;
}

export default function useWorkflowUndoRedo(): UseWorkflowUndoRedoReturnI {
    const incrementLayoutResetCounter = useWorkflowDataStore((state) => state.incrementLayoutResetCounter);

    const {reset} = useWorkflowNodeDetailsPanelStore(
        useShallow((state) => ({
            reset: state.reset,
        }))
    );

    const canUndo = useWorkflowTemporalStore((state) => state.pastStates.length > 0);
    const canRedo = useWorkflowTemporalStore((state) => state.futureStates.length > 0);

    const {invalidateWorkflowQueries, updateWorkflowMutation} = useWorkflowEditor();

    const isMutating = updateWorkflowMutation?.isPending ?? false;

    const persistTimeTravel = useCallback(
        (previousVersion: number | undefined) => {
            const {workflow} = useWorkflowDataStore.getState();

            if (!workflow.id || workflow.definition === undefined || !updateWorkflowMutation) {
                return;
            }

            const definition = workflow.definition;
            const workflowId = workflow.id;

            setWorkflowMutating(workflowId, true);

            updateWorkflowMutation.mutate(
                {
                    id: workflowId,
                    workflow: {
                        definition,
                        version: previousVersion,
                    },
                },
                {
                    onError: (error) => {
                        console.error('Failed to persist undo/redo:', error);

                        invalidateWorkflowQueries?.();
                    },
                    onSettled: () => {
                        setWorkflowMutating(workflowId, false);
                    },
                    onSuccess: (updatedWorkflow) => {
                        const currentWorkflow = useWorkflowDataStore.getState().workflow;

                        setWorkflowWithoutHistory({
                            ...currentWorkflow,
                            version: updatedWorkflow.version,
                        });

                        incrementLayoutResetCounter();
                    },
                }
            );
        },
        [incrementLayoutResetCounter, invalidateWorkflowQueries, updateWorkflowMutation]
    );

    const handleUndo = useCallback(() => {
        const {workflow} = useWorkflowDataStore.getState();

        if (isWorkflowMutating(workflow.id)) {
            return;
        }

        const previousVersion = workflow.version;

        useWorkflowDataStore.temporal.getState().undo();

        reset();

        persistTimeTravel(previousVersion);
    }, [persistTimeTravel, reset]);

    const handleRedo = useCallback(() => {
        const {workflow} = useWorkflowDataStore.getState();

        if (isWorkflowMutating(workflow.id)) {
            return;
        }

        const previousVersion = workflow.version;

        useWorkflowDataStore.temporal.getState().redo();

        reset();

        persistTimeTravel(previousVersion);
    }, [persistTimeTravel, reset]);

    return {
        canRedo: canRedo && !isMutating,
        canUndo: canUndo && !isMutating,
        handleRedo,
        handleUndo,
    };
}
