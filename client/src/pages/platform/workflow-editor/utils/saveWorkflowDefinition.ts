import {ROOT_CLUSTER_ELEMENT_NAMES} from '@/shared/constants';
import {Workflow, WorkflowTask, WorkflowTrigger} from '@/shared/middleware/platform/configuration';
import {ProjectWorkflowKeys} from '@/shared/queries/automation/projectWorkflows.queries';
import {IntegrationWorkflowKeys} from '@/shared/queries/embedded/integrationWorkflows.queries';
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
    integrationId?: number;
    nodeData?: NodeDataType;
    nodeIndex?: number;
    onSuccess?: () => void;
    placeholderId?: string;
    projectId?: number;
    queryClient: QueryClient;
    taskDispatcherContext?: TaskDispatcherContextType;
    updateWorkflowMutation: UseMutationResult<void, Error, UpdateWorkflowRequestType, unknown>;
    updatedWorkflowTasks?: Array<WorkflowTask>;
}

export default async function saveWorkflowDefinition({
    decorative,
    integrationId,
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

    const {
        clusterElements,
        componentName,
        description,
        label,
        metadata,
        name,
        operationName,
        parameters,
        taskDispatcher,
        trigger,
        version,
    } = nodeData ?? {};

    let {type} = nodeData ?? {};

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
            onSuccess: () => {
                if (onSuccess) {
                    onSuccess();
                }

                if (projectId) {
                    queryClient.invalidateQueries({queryKey: ProjectWorkflowKeys.projectWorkflows(projectId)});
                    queryClient.invalidateQueries({queryKey: ProjectWorkflowKeys.workflows});
                } else if (integrationId) {
                    queryClient.invalidateQueries({
                        queryKey: IntegrationWorkflowKeys.integrationWorkflows(integrationId),
                    });
                }
            },
            updateWorkflowMutation,
            workflow,
            workflowDefinition,
        });

        return;
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

            if (ROOT_CLUSTER_ELEMENT_NAMES.includes(existingWorkflowTask.type.split('/')[0])) {
                const rootClusterElementTask: WorkflowTask = {
                    ...newTask,
                    clusterElements: {
                        ...(newTask.clusterElements || {}),
                    },
                };

                updatedWorkflowDefinitionTasks[existingTaskIndex] = rootClusterElementTask;
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
    onSuccess?: () => void;
    updateWorkflowMutation: UseMutationResult<void, Error, UpdateWorkflowRequestType, unknown>;
}

function executeWorkflowMutation({
    definitionUpdate,
    onSuccess,
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
            },
        }
    );
}
