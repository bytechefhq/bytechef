import {Workflow} from '@/shared/middleware/platform/configuration';
import {UpdateWorkflowMutationType, WorkflowDefinitionType} from '@/shared/types';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import stringifyWorkflowDefinition from './stringifyWorkflowDefinition';
import {drainPendingSaves, enqueuePendingSave, isWorkflowMutating, setWorkflowMutating} from './workflowMutationGuard';

interface SaveWorkflowDefinitionUpdateProps {
    onError?: (error: Error) => void;
    onSuccess?: (updatedWorkflow: Workflow) => void;
    updateDefinition: (workflowDefinition: WorkflowDefinitionType) => WorkflowDefinitionType | undefined;
    updateWorkflowMutation: UpdateWorkflowMutationType;
}

export default function saveWorkflowDefinitionUpdate(props: SaveWorkflowDefinitionUpdateProps): void {
    const {onError, onSuccess, updateDefinition, updateWorkflowMutation} = props;

    const {workflow} = useWorkflowDataStore.getState();

    if (!workflow.id || !workflow.definition) {
        return;
    }

    if (isWorkflowMutating(workflow.id)) {
        enqueuePendingSave(workflow.id, () => saveWorkflowDefinitionUpdate(props));

        return;
    }

    let workflowDefinition: WorkflowDefinitionType;

    try {
        workflowDefinition = JSON.parse(workflow.definition);
    } catch (error) {
        console.error('Failed to parse workflow definition:', error);

        return;
    }

    const updatedWorkflowDefinition = updateDefinition(workflowDefinition);

    if (!updatedWorkflowDefinition) {
        return;
    }

    const definition = stringifyWorkflowDefinition(updatedWorkflowDefinition);
    const workflowId = workflow.id;

    setWorkflowMutating(workflowId, true);

    updateWorkflowMutation.mutate(
        {
            id: workflowId,
            workflow: {
                definition,
                version: workflow.version,
            },
        },
        {
            onError: (error) => {
                console.error('Failed to save workflow definition:', error);

                if (onError) {
                    onError(error);
                }
            },
            onSettled: () => {
                setWorkflowMutating(workflowId, false);

                drainPendingSaves(workflowId);
            },
            onSuccess: (updatedWorkflow) => {
                useWorkflowDataStore.getState().setWorkflow({
                    ...updatedWorkflow,
                    definition,
                });

                if (onSuccess) {
                    onSuccess(updatedWorkflow);
                }
            },
        }
    );
}
