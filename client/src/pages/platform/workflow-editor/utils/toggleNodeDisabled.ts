import {SPACE} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {WorkflowNodeOutputKeys} from '@/shared/queries/platform/workflowNodeOutputs.queries';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {QueryClient} from '@tanstack/react-query';

import useWorkflowDataStore, {runWithoutHistory} from '../stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import {flattenDefinitionTasks, isWorkflowTask} from './flattenDefinitionTasks';
import {
    consumePendingDefinition,
    drainPendingSaves,
    enqueuePendingSave,
    isWorkflowMutating,
    setWorkflowMutating,
} from './workflowMutationGuard';

function flipDisabled(task: WorkflowTask): boolean {
    if (task.disabled) {
        delete task.disabled;

        return false;
    }

    task.disabled = true;

    return true;
}

function withDisabled<T extends object>(target: T, disabled: boolean): T {
    const updatedTarget = {...target} as Record<string, unknown>;

    if (disabled) {
        updatedTarget.disabled = true;
    } else {
        delete updatedTarget.disabled;
    }

    return updatedTarget as T;
}

function applyDefinitionWithDisabledFlags(definition: string): void {
    let disabledFlagsByTaskName: Map<string, boolean>;

    try {
        const parsedDefinition = JSON.parse(definition);

        disabledFlagsByTaskName = new Map(
            flattenDefinitionTasks(parsedDefinition.tasks ?? []).map((task) => [task.name, !!task.disabled])
        );
    } catch (error) {
        console.error('Failed to parse workflow definition:', error);

        return;
    }

    useWorkflowDataStore.setState((state) => {
        const updatedTasks = state.workflow.tasks?.map((task) => {
            const disabled = disabledFlagsByTaskName.get(task.name);

            return disabled === undefined || !!task.disabled === disabled ? task : withDisabled(task, disabled);
        });

        const updatedNodes = state.nodes.map((node) => {
            const disabled = disabledFlagsByTaskName.get(node.id);

            return disabled === undefined || !!node.data.disabled === disabled
                ? node
                : {...node, data: withDisabled(node.data, disabled)};
        });

        return {
            nodes: updatedNodes,
            workflow: {
                ...state.workflow,
                definition,
                tasks: updatedTasks,
            },
        };
    });

    useWorkflowNodeDetailsPanelStore.getState().setCurrentNode((currentNode) => {
        if (!currentNode) {
            return currentNode;
        }

        const disabled = disabledFlagsByTaskName.get(currentNode.workflowNodeName);

        return disabled === undefined || !!currentNode.disabled === disabled
            ? currentNode
            : withDisabled(currentNode, disabled);
    });
}

function toggleInFirstMatch(
    values: Array<unknown>,
    toggle: (value: unknown) => boolean | undefined
): boolean | undefined {
    for (const value of values) {
        const outcome = toggle(value);

        if (outcome !== undefined) {
            return outcome;
        }
    }

    return undefined;
}

function toggleInTasks(tasks: unknown, workflowNodeName: string): boolean | undefined {
    if (!Array.isArray(tasks)) {
        return undefined;
    }

    for (const task of tasks) {
        if (!isWorkflowTask(task)) {
            continue;
        }

        if (task.name === workflowNodeName) {
            return flipDisabled(task);
        }

        const outcome =
            toggleInFirstMatch([task.pre, task.post, task.finalize], (hookTasks) =>
                toggleInTasks(hookTasks, workflowNodeName)
            ) ?? toggleInValue(task.parameters, workflowNodeName);

        if (outcome !== undefined) {
            return outcome;
        }
    }

    return undefined;
}

function toggleInValue(value: unknown, workflowNodeName: string): boolean | undefined {
    if (Array.isArray(value)) {
        if (value.some((item) => isWorkflowTask(item))) {
            return toggleInTasks(value, workflowNodeName);
        }

        return toggleInFirstMatch(value, (item) => toggleInValue(item, workflowNodeName));
    }

    if (!value || typeof value !== 'object') {
        return undefined;
    }

    if (isWorkflowTask(value)) {
        return toggleInTasks([value], workflowNodeName);
    }

    return toggleInFirstMatch(Object.values(value), (nestedValue) => toggleInValue(nestedValue, workflowNodeName));
}

interface ToggleNodeDisabledProps {
    queryClient: QueryClient;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    workflowNodeName: string;
}

export function toggleNodeDisabled(props: ToggleNodeDisabledProps): void {
    const {queryClient, updateWorkflowMutation, workflowNodeName} = props;

    const {workflow} = useWorkflowDataStore.getState();

    if (!workflow.definition) {
        return;
    }

    if (workflow.id && isWorkflowMutating(workflow.id)) {
        enqueuePendingSave(workflow.id, () => {
            toggleNodeDisabled(props);
        });

        return;
    }

    let workflowDefinition;

    try {
        workflowDefinition = JSON.parse(workflow.definition);
    } catch (error) {
        console.error('Failed to parse workflow definition:', error);

        return;
    }

    const disabled = toggleInTasks(workflowDefinition.tasks, workflowNodeName);

    if (disabled === undefined) {
        return;
    }

    const updatedDefinition = JSON.stringify(workflowDefinition, null, SPACE);

    if (updatedDefinition === workflow.definition) {
        return;
    }

    const previousDefinition = workflow.definition;

    applyDefinitionWithDisabledFlags(updatedDefinition);

    fireToggleNodeDisabledMutation({
        definition: updatedDefinition,
        previousDefinition,
        queryClient,
        updateWorkflowMutation,
        version: workflow.version,
        workflowId: workflow.id!,
    });
}

interface FireToggleNodeDisabledMutationProps {
    definition: string;
    previousDefinition: string;
    queryClient: QueryClient;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    version?: number;
    workflowId: string;
}

function fireToggleNodeDisabledMutation({
    definition,
    previousDefinition,
    queryClient,
    updateWorkflowMutation,
    version,
    workflowId,
}: FireToggleNodeDisabledMutationProps) {
    setWorkflowMutating(workflowId, true);

    updateWorkflowMutation.mutate(
        {
            id: workflowId,
            workflow: {
                definition,
                version,
            },
        },
        {
            onError: () => {
                runWithoutHistory(() => {
                    applyDefinitionWithDisabledFlags(previousDefinition);
                });
            },
            onSettled: () => {
                setWorkflowMutating(workflowId, false);

                const pendingDefinition = consumePendingDefinition(workflowId);

                if (pendingDefinition) {
                    const currentWorkflow = useWorkflowDataStore.getState().workflow;

                    fireToggleNodeDisabledMutation({
                        definition: pendingDefinition,
                        previousDefinition: currentWorkflow.definition!,
                        queryClient,
                        updateWorkflowMutation,
                        version: currentWorkflow.version,
                        workflowId,
                    });
                } else {
                    drainPendingSaves(workflowId);
                }
            },
            onSuccess: (updatedWorkflow) => {
                const currentWorkflow = useWorkflowDataStore.getState().workflow;

                useWorkflowDataStore.getState().setWorkflow({
                    ...currentWorkflow,
                    version: updatedWorkflow.version,
                });

                void queryClient.invalidateQueries({
                    queryKey: [...WorkflowNodeOutputKeys.workflowNodeOutputs, workflowId],
                });
            },
        }
    );
}
