import {ActionDefinition, Workflow} from '@/shared/middleware/platform/configuration';
import {ClickedOperationType, PropertyAllType, UpdateWorkflowMutationType} from '@/shared/types';
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
    placeholderId: string;
    projectId: number;
    queryClient: QueryClient;
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
    updateWorkflowMutation,
    workflow,
}: HandleConditionChildOperationClickProps) {
    const {componentLabel, componentName, icon, type, version} = operation;

    const workflowNodeName = getFormattedName(componentName!);

    const newWorkflowNodeData = {
        componentName: componentName,
        conditionId,
        icon: icon && <InlineSVG className="size-9" loader={<ComponentIcon className="size-9" />} src={icon} />,
        label: componentLabel,
        name: workflowNodeName,
        type: type,
        version,
        workflowNodeName,
    };

    const taskAfterCurrentIndex = workflow.tasks?.length;

    saveWorkflowDefinition({
        conditionId,
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
        updateWorkflowMutation,
    });
}
