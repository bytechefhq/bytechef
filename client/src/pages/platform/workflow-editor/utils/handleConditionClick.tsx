import {TaskDispatcherDefinitionApi, Workflow} from '@/shared/middleware/platform/configuration';
import {TaskDispatcherKeys} from '@/shared/queries/platform/taskDispatcherDefinitions.queries';
import {
    ClickedDefinitionType,
    NodeDataType,
    PropertyAllType,
    TaskDispatcherContextType,
    UpdateWorkflowMutationType,
} from '@/shared/types';
import {Component1Icon} from '@radix-ui/react-icons';
import {QueryClient} from '@tanstack/react-query';
import InlineSVG from 'react-inlinesvg';

import {WorkflowTaskDataType} from '../stores/useWorkflowDataStore';
import getFormattedName from './getFormattedName';
import getParametersWithDefaultValues from './getParametersWithDefaultValues';
import handleComponentAddedSuccess from './handleComponentAddedSuccess';
import saveWorkflowDefinition from './saveWorkflowDefinition';

interface HandleConditionClickProps {
    clickedItem: ClickedDefinitionType;
    edge?: boolean;
    projectId: number;
    queryClient: QueryClient;
    sourceNodeId?: string;
    taskDispatcherContext?: TaskDispatcherContextType;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    workflow: Workflow & WorkflowTaskDataType;
}

export default async function handleConditionClick({
    clickedItem,
    edge,
    projectId,
    queryClient,
    sourceNodeId,
    taskDispatcherContext,
    updateWorkflowMutation,
    workflow,
}: HandleConditionClickProps) {
    const conditionDefinition = await queryClient.fetchQuery({
        queryFn: () =>
            new TaskDispatcherDefinitionApi().getTaskDispatcherDefinition({
                taskDispatcherName: clickedItem.name,
                taskDispatcherVersion: clickedItem.version,
            }),
        queryKey: TaskDispatcherKeys.taskDispatcherDefinition({
            taskDispatcherName: clickedItem.name,
            taskDispatcherVersion: clickedItem.version,
        }),
    });

    const workflowNodeName = getFormattedName(clickedItem.name!);

    const newConditionNodeData: NodeDataType = {
        ...conditionDefinition,
        componentName: clickedItem.name,
        icon: (
            <>
                {clickedItem.icon ? (
                    <InlineSVG className="size-9 text-gray-700" src={clickedItem.icon} />
                ) : (
                    <Component1Icon className="size-9 text-gray-700" />
                )}
            </>
        ),
        label: clickedItem?.title,
        name: workflowNodeName,
        taskDispatcher: true,
        type: `${conditionDefinition.name}/v${conditionDefinition.version}`,
        workflowNodeName,
    };

    // Handle adding a taskDispatcher to a condition node
    if (taskDispatcherContext) {
        if (taskDispatcherContext.conditionId) {
            newConditionNodeData.conditionData = {
                conditionCase: taskDispatcherContext.conditionCase as 'caseTrue' | 'caseFalse',
                conditionId: taskDispatcherContext.conditionId as string,
                index: (taskDispatcherContext.index ?? 0) as number,
            };
        } else if (taskDispatcherContext.loopId) {
            newConditionNodeData.loopData = {
                index: (taskDispatcherContext.index ?? 0) as number,
                loopId: taskDispatcherContext.loopId as string,
            };
        }
    }

    const hasTaskDispatcherId = Object.entries(taskDispatcherContext ?? {}).some(
        ([key, value]) => key.endsWith('Id') && !!value
    );

    let nodeIndex = workflow.tasks?.length;

    if (hasTaskDispatcherId) {
        nodeIndex = taskDispatcherContext?.index;
    }

    if (edge) {
        nodeIndex = (workflow.tasks?.findIndex((task) => task.name === sourceNodeId) ?? 0) + 1;
    }

    saveWorkflowDefinition({
        nodeData: {
            ...newConditionNodeData,
            parameters: {
                ...getParametersWithDefaultValues({
                    properties: conditionDefinition?.properties as Array<PropertyAllType>,
                }),
                caseFalse: [],
                caseTrue: [],
            },
            workflowNodeName,
        },
        nodeIndex,
        onSuccess: () =>
            handleComponentAddedSuccess({
                nodeData: newConditionNodeData,
                queryClient,
                workflow,
            }),
        placeholderId: hasTaskDispatcherId ? undefined : sourceNodeId,
        projectId,
        queryClient,
        taskDispatcherContext,
        updateWorkflowMutation,
    });
}
