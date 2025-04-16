import {
    ComponentDefinitionApi,
    Workflow,
    WorkflowTask,
    WorkflowTrigger,
} from '@/shared/middleware/platform/configuration';
import {ProjectWorkflowKeys} from '@/shared/queries/automation/projectWorkflows.queries';
import {ComponentDefinitionKeys} from '@/shared/queries/platform/componentDefinitions.queries';
import {BranchCaseType, NodeDataType, TaskDispatcherContextType, WorkflowDefinitionType} from '@/shared/types';
import {QueryClient, UseMutationResult} from '@tanstack/react-query';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import insertTaskDispatcherSubtask from './insertTaskDispatcherSubtask';

const SPACE = 4;

type UpdateWorkflowRequestType = {
    id: string;
    workflow: Workflow;
};

interface SaveWorkflowDefinitionProps {
    decorative?: boolean;
    nodeData: NodeDataType;
    nodeIndex?: number;
    onSuccess?: () => void;
    placeholderId?: string;
    projectId: number;
    queryClient: QueryClient;
    subtask?: boolean;
    taskDispatcherContext?: TaskDispatcherContextType;
    updateWorkflowMutation: UseMutationResult<void, Error, UpdateWorkflowRequestType, unknown>;
}

export default async function saveWorkflowDefinition({
    decorative,
    nodeData,
    nodeIndex,
    onSuccess,
    placeholderId,
    projectId,
    queryClient,
    subtask,
    taskDispatcherContext,
    updateWorkflowMutation,
}: SaveWorkflowDefinitionProps) {
    const {workflow} = useWorkflowDataStore.getState();

    const workflowDefinition: WorkflowDefinitionType = JSON.parse(workflow.definition!);

    const {
        clusterElements,
        componentName,
        connections,
        description,
        label,
        metadata,
        name,
        parameters,
        taskDispatcher,
        trigger,
    } = nodeData;

    let {operationName, type, version} = nodeData;

    if (trigger) {
        if (!type) {
            type = `${componentName}/v${version}/${operationName}`;
        }

        const newTrigger: WorkflowTrigger = {
            connections,
            description,
            label,
            name,
            parameters,
            type,
        };

        updateWorkflowMutation.mutate(
            {
                id: workflow.id!,
                workflow: {
                    definition: JSON.stringify(
                        {
                            ...workflowDefinition,
                            triggers: [newTrigger],
                        },
                        null,
                        SPACE
                    ),
                    version: workflow.version,
                },
            },
            {
                onSuccess,
            }
        );

        return;
    }

    if (!operationName && !taskDispatcher) {
        const newNodeComponentDefinition = await queryClient.fetchQuery({
            queryFn: () =>
                new ComponentDefinitionApi().getComponentDefinition({componentName, componentVersion: version!}),
            queryKey: ComponentDefinitionKeys.componentDefinition({componentName, componentVersion: version!}),
        });

        if (!version) {
            version = newNodeComponentDefinition?.version;
        }

        if (!newNodeComponentDefinition) {
            return;
        }

        operationName = newNodeComponentDefinition.actions?.[0].name;
    }

    if (!type && !trigger) {
        if (taskDispatcher) {
            type = `${componentName}/v${version}`;
        } else {
            type = `${componentName}/v${version}/${operationName}`;
        }
    }

    const newTask: WorkflowTask = {
        clusterElements,
        description,
        label,
        metadata,
        name,
        parameters,
        type: type ?? `${componentName}/v${version}/${operationName}`,
    };

    const existingWorkflowTask = workflowDefinition.tasks?.find((task) => task.name === newTask.name);

    const differenceInCaseCount =
        existingWorkflowTask &&
        componentName === 'branch' &&
        (existingWorkflowTask?.parameters?.cases as BranchCaseType[])?.length !== newTask.parameters?.cases.length;

    const differenceInCaseKeys =
        existingWorkflowTask &&
        componentName === 'branch' &&
        (existingWorkflowTask?.parameters?.cases as BranchCaseType[])?.some((caseItem, index) => {
            const newCaseItem = newTask.parameters?.cases?.[index];

            return caseItem.key !== newCaseItem?.key;
        });

    const differenceInParameters =
        existingWorkflowTask?.parameters &&
        JSON.stringify(existingWorkflowTask.parameters) !== JSON.stringify(newTask.parameters);

    const differenceInType = existingWorkflowTask?.type !== newTask.type;

    const differenceInClusterElements =
        JSON.stringify(existingWorkflowTask?.clusterElements) !== JSON.stringify(newTask.clusterElements);

    if (
        existingWorkflowTask &&
        !decorative &&
        !subtask &&
        (!operationName || !differenceInParameters) &&
        !differenceInClusterElements &&
        !differenceInType &&
        !differenceInCaseCount &&
        !differenceInCaseKeys
    ) {
        return;
    }

    let tasks: WorkflowTask[];

    if (existingWorkflowTask) {
        const existingTaskIndex = workflowDefinition.tasks?.findIndex(
            (task) => task.name === existingWorkflowTask.name
        );

        if (existingTaskIndex === undefined) {
            return;
        }

        tasks = [...(workflowDefinition.tasks || [])];

        if (existingWorkflowTask.type !== newTask.type) {
            delete tasks[existingTaskIndex].parameters;
        }

        const combinedParameters = {
            ...existingWorkflowTask.parameters,
            ...newTask.parameters,
        };

        const combinedTask: WorkflowTask = {
            ...existingWorkflowTask,
            ...newTask,
            parameters: combinedParameters,
        };

        tasks[existingTaskIndex] = combinedTask;
    } else {
        tasks = [...(workflowDefinition.tasks || [])];

        if (taskDispatcherContext?.taskDispatcherId) {
            tasks = insertTaskDispatcherSubtask({
                newTask,
                placeholderId,
                taskDispatcherContext,
                tasks,
            });
        } else if (nodeIndex !== undefined && nodeIndex > -1) {
            const tasksAfterCurrent = tasks.slice(nodeIndex);

            tasks = [...tasks.slice(0, nodeIndex), ...tasksAfterCurrent];

            tasks.splice(nodeIndex, 0, newTask);
        } else {
            tasks.push(newTask);
        }
    }

    updateWorkflowMutation.mutate(
        {
            id: workflow.id!,
            workflow: {
                definition: JSON.stringify(
                    {
                        ...workflowDefinition,
                        tasks,
                    },
                    null,
                    SPACE
                ),
                version: workflow.version,
            },
        },
        {
            onSuccess: () => {
                if (onSuccess) {
                    onSuccess();
                }

                queryClient.invalidateQueries({queryKey: ProjectWorkflowKeys.projectWorkflows(projectId)});

                queryClient.invalidateQueries({queryKey: ProjectWorkflowKeys.workflows});
            },
        }
    );
}
