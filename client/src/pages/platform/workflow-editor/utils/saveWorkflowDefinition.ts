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
    nodeData?: NodeDataType;
    nodeIndex?: number;
    onSuccess?: () => void;
    placeholderId?: string;
    projectId: number;
    queryClient: QueryClient;
    taskDispatcherContext?: TaskDispatcherContextType;
    updateWorkflowMutation: UseMutationResult<void, Error, UpdateWorkflowRequestType, unknown>;
    updatedWorkflowTasks?: Array<WorkflowTask>;
}

export default async function saveWorkflowDefinition({
    decorative,
    nodeData,
    nodeIndex,
    onSuccess,
    placeholderId,
    projectId,
    queryClient,
    taskDispatcherContext,
    updateWorkflowMutation,
    updatedWorkflowTasks,
}: SaveWorkflowDefinitionProps) {
    const {workflow} = useWorkflowDataStore.getState();

    const workflowDefinition = JSON.parse(workflow.definition!);

    const workflowTasks: Array<WorkflowTask> = workflow.tasks ?? [];
    const workflowDefinitionTasks: Array<WorkflowTask> = workflowDefinition.tasks ?? [];

    const {clusterElements, componentName, description, label, metadata, name, parameters, taskDispatcher, trigger} =
        nodeData ?? {};

    let {operationName, type, version} = nodeData ?? {};

    if (trigger) {
        if (!type) {
            type = `${componentName}/v${version}/${operationName}`;
        }

        const newTrigger: WorkflowTrigger = {
            description,
            label,
            name: name!,
            parameters,
            type,
        };

        executeWorkflowMutation({
            definitionUpdate: {triggers: [newTrigger]},
            onSuccess,
            projectId,
            queryClient,
            updateWorkflowMutation,
            workflow,
            workflowDefinition,
        });

        return;
    }

    if (!operationName && !taskDispatcher && componentName) {
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
        name: name!,
        parameters,
        type: type ?? `${componentName}/v${version}/${operationName}`,
    };

    const existingWorkflowTask = workflowTasks?.find((task) => task.name === newTask.name);

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
        (!operationName || !differenceInParameters) &&
        !differenceInClusterElements &&
        !differenceInType &&
        !differenceInCaseCount &&
        !differenceInCaseKeys
    ) {
        return;
    }

    let updatedWorkflowDefinitionTasks = workflowDefinitionTasks;

    if (updatedWorkflowTasks) {
        updatedWorkflowDefinitionTasks = updatedWorkflowTasks;
    } else {
        if (existingWorkflowTask) {
            const existingTaskIndex = workflowDefinitionTasks?.findIndex(
                (task) => task.name === existingWorkflowTask.name
            );

            if (existingTaskIndex === undefined || existingTaskIndex === -1) {
                return;
            }

            let combinedParameters = {
                ...existingWorkflowTask.parameters,
                ...newTask.parameters,
            };

            if (existingWorkflowTask.type !== newTask.type) {
                delete updatedWorkflowDefinitionTasks[existingTaskIndex].parameters;

                combinedParameters = newTask.parameters ?? {};
            }

            if (existingWorkflowTask.type.split('/')[0] === 'aiAgent') {
                const aiAgentTask: WorkflowTask = {
                    ...newTask,
                    clusterElements: {
                        ...(newTask.clusterElements || {}),
                    },
                };

                updatedWorkflowDefinitionTasks[existingTaskIndex] = aiAgentTask;
            } else {
                const combinedTask: WorkflowTask = {
                    ...newTask,
                    parameters: combinedParameters,
                };

                updatedWorkflowDefinitionTasks = [
                    ...updatedWorkflowDefinitionTasks.slice(0, existingTaskIndex),
                    combinedTask,
                    ...updatedWorkflowDefinitionTasks.slice(existingTaskIndex + 1),
                ];
            }
        } else {
            updatedWorkflowDefinitionTasks = [...(workflowDefinitionTasks || [])];

            if (taskDispatcherContext?.taskDispatcherId) {
                updatedWorkflowDefinitionTasks = insertTaskDispatcherSubtask({
                    newTask,
                    placeholderId,
                    taskDispatcherContext,
                    tasks: updatedWorkflowDefinitionTasks,
                });
            } else if (nodeIndex !== undefined && nodeIndex > -1) {
                const tasksAfterCurrent = updatedWorkflowDefinitionTasks.slice(nodeIndex);

                updatedWorkflowDefinitionTasks = [
                    ...updatedWorkflowDefinitionTasks.slice(0, nodeIndex),
                    ...tasksAfterCurrent,
                ];

                updatedWorkflowDefinitionTasks.splice(nodeIndex, 0, newTask);
            } else {
                updatedWorkflowDefinitionTasks.push(newTask);
            }
        }
    }

    executeWorkflowMutation({
        definitionUpdate: {tasks: updatedWorkflowDefinitionTasks},
        onSuccess,
        projectId,
        queryClient,
        updateWorkflowMutation,
        workflow,
        workflowDefinition,
    });
}

interface ExecuteWorkflowMutationProps {
    workflow: Workflow;
    workflowDefinition: WorkflowDefinitionType;
    definitionUpdate: {
        tasks?: Array<WorkflowTask>;
        triggers?: Array<WorkflowTrigger>;
    };
    projectId: number;
    onSuccess?: () => void;
    queryClient: QueryClient;
    updateWorkflowMutation: UseMutationResult<void, Error, UpdateWorkflowRequestType, unknown>;
}

function executeWorkflowMutation({
    definitionUpdate,
    onSuccess,
    projectId,
    queryClient,
    updateWorkflowMutation,
    workflow,
    workflowDefinition,
}: ExecuteWorkflowMutationProps) {
    updateWorkflowMutation.mutate(
        {
            id: workflow.id!,
            workflow: {
                definition: JSON.stringify(
                    {
                        ...workflowDefinition,
                        ...definitionUpdate,
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
