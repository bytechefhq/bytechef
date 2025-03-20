import {ActionDefinition, Workflow} from '@/shared/middleware/platform/configuration';
import {
    ClickedOperationType,
    NodeDataType,
    PropertyAllType,
    TaskDispatcherContextType,
    UpdateWorkflowMutationType,
} from '@/shared/types';
import {QueryClient} from '@tanstack/react-query';
import {ComponentIcon} from 'lucide-react';
import InlineSVG from 'react-inlinesvg';

import {WorkflowTaskDataType} from '../stores/useWorkflowDataStore';
import getFormattedName from './getFormattedName';
import getParametersWithDefaultValues from './getParametersWithDefaultValues';
import handleComponentAddedSuccess from './handleComponentAddedSuccess';
import saveWorkflowDefinition from './saveWorkflowDefinition';

interface HandleConditionChildOperationClickProps {
    conditionId: string;
    operation: ClickedOperationType;
    operationDefinition: ActionDefinition;
    placeholderId?: string;
    projectId: number;
    queryClient: QueryClient;
    taskDispatcherContext?: TaskDispatcherContextType;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    workflow: Workflow & WorkflowTaskDataType;
}

export default function handleConditionChildOperationClick({
    conditionId,
    operation,
    operationDefinition,
    placeholderId,
    projectId,
    queryClient,
    taskDispatcherContext,
    updateWorkflowMutation,
    workflow,
}: HandleConditionChildOperationClickProps) {
    const {componentLabel, componentName, icon, type, version} = operation;

    const workflowNodeName = getFormattedName(componentName!);

    const newWorkflowNodeData: NodeDataType = {
        componentName,
        conditionId,
        icon: icon && <InlineSVG className="size-9" loader={<ComponentIcon className="size-9" />} src={icon} />,
        label: componentLabel,
        name: workflowNodeName,
        type,
        version,
        workflowNodeName,
    };

    if (taskDispatcherContext?.conditionId) {
        newWorkflowNodeData.conditionData = {
            conditionCase: taskDispatcherContext.conditionCase as string,
            conditionId: taskDispatcherContext.conditionId as string,
            index: taskDispatcherContext.index as number,
        };
    }

    const taskAfterCurrentIndex = workflow.tasks?.length;

    saveWorkflowDefinition({
        nodeData: {
            ...newWorkflowNodeData,
            parameters: getParametersWithDefaultValues({
                properties: operationDefinition?.properties as Array<PropertyAllType>,
            }),
        },
        nodeIndex: taskAfterCurrentIndex,
        onSuccess: () =>
            handleComponentAddedSuccess({
                nodeData: newWorkflowNodeData,
                queryClient,
                workflow,
            }),
        placeholderId,
        projectId,
        queryClient,
        taskDispatcherContext,
        updateWorkflowMutation,
    });
}
