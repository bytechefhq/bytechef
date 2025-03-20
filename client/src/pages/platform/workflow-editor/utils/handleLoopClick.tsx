import {Workflow} from '@/shared/middleware/automation/configuration';
import {TaskDispatcherDefinitionApi} from '@/shared/middleware/platform/configuration';
import {TaskDispatcherKeys} from '@/shared/queries/platform/taskDispatcherDefinitions.queries';
import {
    ClickedDefinitionType,
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

interface HandleLoopClickProps {
    clickedItem: ClickedDefinitionType;
    edge?: boolean;
    projectId: number;
    queryClient: QueryClient;
    sourceNodeId?: string;
    taskDispatcherContext?: TaskDispatcherContextType;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    workflow: Workflow & WorkflowTaskDataType;
}

export default async function handleLoopClick({
    clickedItem,
    edge,
    projectId,
    queryClient,
    sourceNodeId,
    taskDispatcherContext,
    updateWorkflowMutation,
    workflow,
}: HandleLoopClickProps) {
    const loopDefinition = await queryClient.fetchQuery({
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

    const newLoopNodeData: NodeDataType = {
        ...loopDefinition,
        componentName: clickedItem.name,
        icon: (
            <>
                {clickedItem.icon ? (
                    <InlineSVG className="size-9 text-gray-700" src={clickedItem.icon} />
                ) : (
                    <ComponentIcon className="size-9 text-gray-700" />
                )}
            </>
        ),
        label: clickedItem?.title,
        name: workflowNodeName,
        taskDispatcher: true,
        type: `${loopDefinition.name}/v${loopDefinition.version}`,
        workflowNodeName,
    };

    // Handle adding a taskDispatcher to a loop node
    if (taskDispatcherContext) {
        if (taskDispatcherContext.conditionId) {
            newLoopNodeData.conditionData = {
                conditionCase: taskDispatcherContext.conditionCase as 'caseTrue' | 'caseFalse',
                conditionId: taskDispatcherContext.conditionId as string,
                index: (taskDispatcherContext.index ?? 0) as number,
            };
        } else if (taskDispatcherContext.loopId) {
            newLoopNodeData.loopData = {
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
            ...newLoopNodeData,
            parameters: {
                ...getParametersWithDefaultValues({
                    properties: loopDefinition?.properties as Array<PropertyAllType>,
                }),
            },
        },
        nodeIndex,
        onSuccess: () =>
            handleComponentAddedSuccess({
                nodeData: newLoopNodeData,
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
