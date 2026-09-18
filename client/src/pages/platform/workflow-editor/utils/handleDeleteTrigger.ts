import {SPACE} from '@/shared/constants';
import {Workflow} from '@/shared/middleware/platform/configuration';
import {UpdateWorkflowMutationType, WorkflowDefinitionType} from '@/shared/types';

import useWorkflowDataStore, {WorkflowDataType, setWorkflowWithoutHistory} from '../stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import {isWorkflowMutating, setWorkflowMutating} from './workflowMutationGuard';

interface HandleDeleteTriggerProps {
    cancelWorkflowQueries: () => void;
    invalidateWorkflowQueries: () => void;
    triggerName: string;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    workflow: Workflow & WorkflowDataType;
}

export default function handleDeleteTrigger({
    cancelWorkflowQueries,
    invalidateWorkflowQueries,
    triggerName,
    updateWorkflowMutation,
    workflow,
}: HandleDeleteTriggerProps) {
    if (!workflow?.definition) {
        return;
    }

    const workflowDefinition: WorkflowDefinitionType = JSON.parse(workflow.definition);

    const existingTriggers = workflowDefinition.triggers ?? [];

    if (existingTriggers.length <= 1) {
        return;
    }

    const updatedTriggers = existingTriggers.filter((trigger) => trigger.name !== triggerName);

    if (isWorkflowMutating(workflow.id!)) {
        return;
    }

    cancelWorkflowQueries();

    const updatedDefinition = JSON.stringify(
        {
            ...workflowDefinition,
            triggers: updatedTriggers,
        },
        null,
        SPACE
    );

    const previousWorkflow = workflow;

    const currentNode = useWorkflowNodeDetailsPanelStore.getState().currentNode;

    if (currentNode?.name === triggerName) {
        useWorkflowNodeDetailsPanelStore.getState().reset();
    }

    useWorkflowDataStore.getState().setWorkflow({
        ...workflow,
        definition: updatedDefinition,
        triggers: updatedTriggers,
    });

    setWorkflowMutating(workflow.id!, true);

    updateWorkflowMutation.mutate(
        {
            id: workflow.id!,
            workflow: {
                definition: updatedDefinition,
                version: workflow.version,
            },
        },
        {
            onError: () => {
                setWorkflowWithoutHistory(previousWorkflow);

                invalidateWorkflowQueries();
            },
            onSettled: () => {
                setWorkflowMutating(workflow.id!, false);

                invalidateWorkflowQueries();
            },
            onSuccess: (updatedWorkflow) => {
                const currentWorkflow = useWorkflowDataStore.getState().workflow;

                useWorkflowDataStore.getState().setWorkflow({
                    ...currentWorkflow,
                    version: updatedWorkflow.version,
                });
            },
        }
    );
}
