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

type BuildNodeDataType = {
    taskDispatcherContext: TaskDispatcherContextType;
    taskDispatcherId: string;
    baseNodeData: NodeDataType;
};

const TASK_DISPATCHER_CONFIG = {
    condition: {
        buildNodeData: ({baseNodeData, taskDispatcherContext, taskDispatcherId}: BuildNodeDataType): NodeDataType => {
            const newNodeData = {
                ...baseNodeData,
                conditionId: taskDispatcherId,
            };

            // Add condition-specific data if context exists
            if (taskDispatcherContext?.conditionId) {
                newNodeData.conditionData = {
                    conditionCase: taskDispatcherContext.conditionCase as string,
                    conditionId: taskDispatcherContext.conditionId as string,
                    index: taskDispatcherContext.index as number,
                };
            }

            return newNodeData;
        },
        getDispatcherId: (context: TaskDispatcherContextType) => context.conditionId,
    },
    loop: {
        buildNodeData: ({baseNodeData, taskDispatcherContext, taskDispatcherId}: BuildNodeDataType): NodeDataType => {
            const newNodeData = {
                ...baseNodeData,
                loopId: taskDispatcherId,
            };

            // Add loop-specific data if context exists
            if (taskDispatcherContext?.loopId) {
                newNodeData.loopData = {
                    index: taskDispatcherContext.index as number,
                    loopId: taskDispatcherContext.loopId as string,
                };
            }

            return newNodeData;
        },
        getDispatcherId: (context: TaskDispatcherContextType) => context.loopId,
    },
};

interface HandleTaskDispatcherSubtaskOperationClickProps {
    operation: ClickedOperationType;
    operationDefinition: ActionDefinition;
    placeholderId?: string;
    projectId: number;
    queryClient: QueryClient;
    taskDispatcherContext?: TaskDispatcherContextType;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    workflow: Workflow & WorkflowTaskDataType;
}

export default function handleTaskDispatcherSubtaskOperationClick({
    operation,
    operationDefinition,
    placeholderId,
    projectId,
    queryClient,
    taskDispatcherContext = {},
    updateWorkflowMutation,
    workflow,
}: HandleTaskDispatcherSubtaskOperationClickProps) {
    const taskDispatcherId = taskDispatcherContext.loopId || taskDispatcherContext.conditionId || '';

    const componentName = taskDispatcherId.split('_')[0] as keyof typeof TASK_DISPATCHER_CONFIG;

    const dispatcherConfig = TASK_DISPATCHER_CONFIG[componentName];

    if (!dispatcherConfig) {
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

    const newWorkflowNodeData = dispatcherConfig.buildNodeData({baseNodeData, taskDispatcherContext, taskDispatcherId});

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
