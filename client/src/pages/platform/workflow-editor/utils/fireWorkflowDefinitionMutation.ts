import {UpdateWorkflowMutationType} from '@/shared/types';

import useWorkflowDataStore, {runWithoutHistory} from '../stores/useWorkflowDataStore';
import {consumePendingDefinition, drainPendingSaves, setWorkflowMutating} from './workflowMutationGuard';

interface FireWorkflowDefinitionMutationProps {
    definition: string;
    previousDefinition: string;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    version?: number;
    workflowId: string;
}

export default function fireWorkflowDefinitionMutation({
    definition,
    previousDefinition,
    updateWorkflowMutation,
    version,
    workflowId,
}: FireWorkflowDefinitionMutationProps) {
    setWorkflowMutating(workflowId, true);

    let settledDefinition = previousDefinition;

    updateWorkflowMutation.mutate(
        {
            id: workflowId,
            workflow: {
                definition,
                version,
            },
        },
        {
            onError: () => {
                if (useWorkflowDataStore.getState().workflow.definition !== definition) {
                    return;
                }

                runWithoutHistory(() => {
                    useWorkflowDataStore.setState((state) => ({
                        workflow: {
                            ...state.workflow,
                            definition: previousDefinition,
                        },
                    }));
                });
            },
            onSettled: () => {
                setWorkflowMutating(workflowId, false);

                const pendingDefinition = consumePendingDefinition(workflowId);

                if (pendingDefinition) {
                    const currentWorkflow = useWorkflowDataStore.getState().workflow;

                    fireWorkflowDefinitionMutation({
                        definition: pendingDefinition,
                        previousDefinition: settledDefinition,
                        updateWorkflowMutation,
                        version: currentWorkflow.version,
                        workflowId,
                    });
                } else {
                    drainPendingSaves(workflowId);
                }
            },
            onSuccess: (updatedWorkflow) => {
                settledDefinition = definition;

                const currentWorkflow = useWorkflowDataStore.getState().workflow;

                useWorkflowDataStore.getState().setWorkflow({
                    ...currentWorkflow,
                    version: updatedWorkflow.version,
                });
            },
        }
    );
}
