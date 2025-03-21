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

const fallbackIcon = <ComponentIcon className="size-9 text-gray-700" />;

const TASK_DISPATCHER_CONFIG = {
    condition: {
        getInitialParameters: (properties: Array<PropertyAllType>) => ({
            ...getParametersWithDefaultValues({properties}),
            caseFalse: [],
            caseTrue: [],
        }),
    },
    loop: {
        getInitialParameters: (properties: Array<PropertyAllType>) => ({
            ...getParametersWithDefaultValues({properties}),
        }),
    },
};

interface HandleTaskDispatcherClickProps {
    clickedItem: ClickedDefinitionType;
    edge?: boolean;
    projectId: number;
    queryClient: QueryClient;
    sourceNodeId?: string;
    taskDispatcherContext?: TaskDispatcherContextType;
    taskDispatcherName: keyof typeof TASK_DISPATCHER_CONFIG;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    workflow: Workflow & WorkflowTaskDataType;
}

export default async function handleTaskDispatcherClick({
    clickedItem,
    edge,
    projectId,
    queryClient,
    sourceNodeId,
    taskDispatcherContext,
    taskDispatcherName,
    updateWorkflowMutation,
    workflow,
}: HandleTaskDispatcherClickProps) {
    const config = TASK_DISPATCHER_CONFIG[taskDispatcherName];

    if (!config) {
        console.error(`Unknown task dispatcher type: ${taskDispatcherName}`);

        return;
    }

    const taskDispatcherDefinition = await queryClient.fetchQuery({
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

    const newNodeData: NodeDataType = {
        ...taskDispatcherDefinition,
        componentName: clickedItem.name,
        icon: clickedItem.icon ? <InlineSVG className="size-9 text-gray-700" src={clickedItem.icon} /> : fallbackIcon,
        label: clickedItem?.title,
        name: workflowNodeName,
        taskDispatcher: true,
        type: `${taskDispatcherDefinition.name}/v${taskDispatcherDefinition.version}`,
        workflowNodeName,
    };

    if (taskDispatcherContext) {
        if (taskDispatcherContext.conditionId) {
            newNodeData.conditionData = {
                conditionCase: taskDispatcherContext.conditionCase as 'caseTrue' | 'caseFalse',
                conditionId: taskDispatcherContext.conditionId as string,
                index: (taskDispatcherContext.index ?? 0) as number,
            };
        } else if (taskDispatcherContext.loopId) {
            newNodeData.loopData = {
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
            ...newNodeData,
            parameters: config.getInitialParameters(taskDispatcherDefinition?.properties as Array<PropertyAllType>),
            workflowNodeName,
        },
        nodeIndex,
        onSuccess: () =>
            handleComponentAddedSuccess({
                nodeData: newNodeData,
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
