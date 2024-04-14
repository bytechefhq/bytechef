import {UpdateWorkflowRequest, WorkflowModel} from '@/middleware/automation/configuration';
import {ComponentType} from '@/types/types';
import {UseMutationResult} from '@tanstack/react-query';

import {WorkflowTaskDataType} from '../stores/useWorkflowDataStore';
import saveWorkflowDefinition from './saveWorkflowDefinition';

export default function saveProperty(
    parameters: object,
    setComponentData: (componentData: Array<ComponentType>) => void,
    currentComponentData: ComponentType,
    otherComponentData: Array<ComponentType>,
    updateWorkflowMutation: UseMutationResult<WorkflowModel, Error, UpdateWorkflowRequest, unknown>,
    name: string,
    workflow: WorkflowModel & WorkflowTaskDataType
) {
    if (!currentComponentData || !updateWorkflowMutation || !name) {
        return;
    }

    const {actionName, componentName, workflowNodeName} = currentComponentData;

    saveWorkflowDefinition(
        {
            actionName,
            componentName,
            name: workflowNodeName,
            parameters,
        },
        workflow,
        updateWorkflowMutation,
        undefined,
        (workflow) => {
            let parameters;

            for (const trigger of workflow.triggers ?? []) {
                if (trigger.name === workflowNodeName) {
                    parameters = trigger.parameters;

                    break;
                }
            }

            if (!parameters) {
                for (const task of workflow.tasks ?? []) {
                    if (task.name === workflowNodeName) {
                        parameters = task.parameters;

                        break;
                    }
                }
            }

            setComponentData([
                ...otherComponentData,
                {
                    ...currentComponentData,
                    parameters,
                },
            ]);
        }
    );
}
