import {ActionDefinition, Workflow} from '@/shared/middleware/platform/configuration';
import {ClickedOperationType, NodeDataType, PropertyAllType, UpdateWorkflowMutationType} from '@/shared/types';
import {QueryClient} from '@tanstack/react-query';
import {Node} from '@xyflow/react';
import {ComponentIcon} from 'lucide-react';
import InlineSVG from 'react-inlinesvg';

import {WorkflowTaskDataType} from '../stores/useWorkflowDataStore';
import getFormattedName from './getFormattedName';
import getParametersWithDefaultValues from './getParametersWithDefaultValues';
import handleComponentAddedSuccess from './handleComponentAddedSuccess';
import saveWorkflowDefinition from './saveWorkflowDefinition';

interface HandleConditionChildOperationClickProps {
    conditionId: string;
    currentNode?: NodeDataType;
    nodes: Array<Node>;
    operation: ClickedOperationType;
    operationDefinition: ActionDefinition;
    placeholderId: string;
    queryClient: QueryClient;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    workflow: Workflow & WorkflowTaskDataType;
}

export default function handleConditionChildOperationClick({
    conditionId,
    currentNode,
    nodes,
    operation,
    operationDefinition,
    placeholderId,
    queryClient,
    updateWorkflowMutation,
    workflow,
}: HandleConditionChildOperationClickProps) {
    const {componentLabel, componentName, icon, type, version} = operation;

    const workflowNodeName = getFormattedName(componentName!, nodes);

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
        queryClient,
        updateWorkflowMutation,
        workflow,
    });
}
