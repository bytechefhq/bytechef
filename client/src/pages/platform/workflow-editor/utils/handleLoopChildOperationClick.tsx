import {Workflow} from '@/shared/middleware/automation/configuration';
import {ActionDefinition} from '@/shared/middleware/platform/configuration';
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

interface HandleLoopChildOperationClickProps {
    currentNode?: NodeDataType;
    loopId: string;
    operation: ClickedOperationType;
    operationDefinition: ActionDefinition;
    placeholderId?: string;
    projectId: number;
    queryClient: QueryClient;
    taskDispatcherContext?: TaskDispatcherContextType;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    workflow: Workflow & WorkflowTaskDataType;
}

export default function handleLoopChildOperationClick({
    loopId,
    operation,
    operationDefinition,
    placeholderId,
    projectId,
    queryClient,
    taskDispatcherContext,
    updateWorkflowMutation,
    workflow,
}: HandleLoopChildOperationClickProps) {
    const {componentLabel, componentName, icon, type, version} = operation;

    const workflowNodeName = getFormattedName(componentName!);

    const newWorkflowNodeData: NodeDataType = {
        componentName: componentName,
        icon: icon && <InlineSVG className="size-9" loader={<ComponentIcon className="size-9" />} src={icon} />,
        label: componentLabel,
        loopId,
        name: workflowNodeName,
        type: type,
        version,
        workflowNodeName,
    };

    if (taskDispatcherContext?.loopId) {
        newWorkflowNodeData.loopData = {
            index: taskDispatcherContext.index as number,
            loopId: taskDispatcherContext.loopId as string,
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
