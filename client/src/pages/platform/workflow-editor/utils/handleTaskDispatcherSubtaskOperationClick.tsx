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

import {WorkflowDataType} from '../stores/useWorkflowDataStore';
import getFormattedName from './getFormattedName';
import getParametersWithDefaultValues from './getParametersWithDefaultValues';
import handleComponentAddedSuccess from './handleComponentAddedSuccess';
import saveWorkflowDefinition from './saveWorkflowDefinition';
import {TASK_DISPATCHER_CONFIG} from './taskDispatcherConfig';

interface HandleTaskDispatcherSubtaskOperationClickProps {
    invalidateWorkflowQueries: () => void;
    operation: ClickedOperationType;
    operationDefinition: ActionDefinition;
    placeholderId?: string;
    queryClient: QueryClient;
    taskDispatcherContext?: TaskDispatcherContextType;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    workflow: Workflow & WorkflowDataType;
}

export default function handleTaskDispatcherSubtaskOperationClick({
    invalidateWorkflowQueries,
    operation,
    operationDefinition,
    placeholderId,
    queryClient,
    taskDispatcherContext,
    updateWorkflowMutation,
    workflow,
}: HandleTaskDispatcherSubtaskOperationClickProps) {
    if (!taskDispatcherContext) {
        console.error('Task dispatcher context is required to add a subtask');

        return;
    }

    const taskDispatcherId = taskDispatcherContext!.taskDispatcherId;

    const componentName = taskDispatcherId.split('_')[0] as keyof typeof TASK_DISPATCHER_CONFIG;

    const taskDispatcherConfig = TASK_DISPATCHER_CONFIG[componentName];

    if (!taskDispatcherConfig) {
        console.error(`Unknown task dispatcher type: ${componentName}`);

        return;
    }

    const {componentLabel, icon, type, version} = operation;

    const workflowNodeName = getFormattedName(operation.componentName!);

    const baseNodeData: NodeDataType = {
        componentName: operation.componentName,
        icon: icon && <InlineSVG className="size-9" loader={<ComponentIcon className="size-9" />} src={icon} />,
        label: componentLabel,
        name: workflowNodeName,
        type,
        version,
        workflowNodeName,
    };

    const newWorkflowNodeData = taskDispatcherConfig.buildNodeData({
        baseNodeData,
        taskDispatcherContext,
        taskDispatcherId,
    });

    const taskAfterCurrentIndex = workflow.tasks?.length;

    saveWorkflowDefinition({
        invalidateWorkflowQueries,
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
        taskDispatcherContext,
        updateWorkflowMutation,
    });
}
