import {SPACE} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {BranchCaseType, NodeDataType, UpdateWorkflowMutationType} from '@/shared/types';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import {clearTaskPositions} from './clearAllNodePositions';
import {isWorkflowMutating, setWorkflowMutating} from './workflowMutationGuard';

interface RemoveWorkflowNodePositionProps {
    incrementLayoutResetCounter: () => void;
    invalidateWorkflowQueries: () => void;
    nodeName: string;
    updateWorkflowMutation: UpdateWorkflowMutationType;
}

export function clearSingleTaskPosition(tasks: WorkflowTask[], nodeName: string): WorkflowTask[] {
    if (!Array.isArray(tasks)) {
        return tasks;
    }

    return tasks.map((task) => {
        let updatedTask = {...task};

        const isMatch = task.name === nodeName;

        if (isMatch && task.metadata?.ui?.nodePosition) {
            updatedTask.metadata = {
                ...task.metadata,
                ui: {
                    ...task.metadata.ui,
                    nodePosition: undefined,
                },
            };
        }

        // For the matched task dispatcher, clear ALL child positions;
        // for non-matching tasks, continue searching by name
        const clearChildren = isMatch
            ? clearTaskPositions
            : (childTasks: WorkflowTask[]) => clearSingleTaskPosition(childTasks, nodeName);

        if (updatedTask.parameters?.caseTrue) {
            updatedTask = {
                ...updatedTask,
                parameters: {
                    ...updatedTask.parameters,
                    caseTrue: clearChildren(updatedTask.parameters.caseTrue as WorkflowTask[]),
                },
            };
        }

        if (updatedTask.parameters?.caseFalse) {
            updatedTask = {
                ...updatedTask,
                parameters: {
                    ...updatedTask.parameters,
                    caseFalse: clearChildren(updatedTask.parameters.caseFalse as WorkflowTask[]),
                },
            };
        }

        if (updatedTask.parameters?.iteratee) {
            if (Array.isArray(updatedTask.parameters.iteratee)) {
                updatedTask = {
                    ...updatedTask,
                    parameters: {
                        ...updatedTask.parameters,
                        iteratee: clearChildren(updatedTask.parameters.iteratee as WorkflowTask[]),
                    },
                };
            }
        }

        if (updatedTask.parameters?.cases) {
            const updatedCases = (updatedTask.parameters.cases as BranchCaseType[]).map((caseItem) => ({
                ...caseItem,
                tasks: caseItem.tasks ? clearChildren(caseItem.tasks) : caseItem.tasks,
            }));

            updatedTask = {
                ...updatedTask,
                parameters: {
                    ...updatedTask.parameters,
                    cases: updatedCases,
                },
            };

            if (updatedTask.parameters?.default) {
                updatedTask = {
                    ...updatedTask,
                    parameters: {
                        ...updatedTask.parameters,
                        default: clearChildren(updatedTask.parameters?.default as WorkflowTask[]),
                    },
                };
            }
        }

        if (updatedTask.parameters?.tasks) {
            updatedTask = {
                ...updatedTask,
                parameters: {
                    ...updatedTask.parameters,
                    tasks: clearChildren(updatedTask.parameters.tasks as WorkflowTask[]),
                },
            };
        }

        if (updatedTask.parameters?.branches) {
            const updatedBranches = (updatedTask.parameters.branches as WorkflowTask[][]).map((branch) =>
                clearChildren(branch)
            );

            updatedTask = {
                ...updatedTask,
                parameters: {
                    ...updatedTask.parameters,
                    branches: updatedBranches,
                },
            };
        }

        return updatedTask;
    });
}

export default function removeWorkflowNodePosition({
    incrementLayoutResetCounter,
    invalidateWorkflowQueries,
    nodeName,
    updateWorkflowMutation,
}: RemoveWorkflowNodePositionProps) {
    const {workflow} = useWorkflowDataStore.getState();

    if (!workflow.definition) {
        return;
    }

    const workflowDefinition = JSON.parse(workflow.definition);

    if (workflowDefinition.triggers?.[0]?.name === nodeName) {
        workflowDefinition.triggers[0] = {
            ...workflowDefinition.triggers[0],
            metadata: {
                ...workflowDefinition.triggers[0].metadata,
                ui: {
                    ...workflowDefinition.triggers[0].metadata?.ui,
                    nodePosition: undefined,
                },
            },
        };
    }

    if (workflowDefinition.tasks) {
        workflowDefinition.tasks = clearSingleTaskPosition(workflowDefinition.tasks, nodeName);
    }

    const updatedDefinition = JSON.stringify(workflowDefinition, null, SPACE);

    const updatedTasks = workflow.tasks
        ? clearSingleTaskPosition(workflow.tasks as WorkflowTask[], nodeName)
        : workflow.tasks;

    const updatedTriggers = workflow.triggers?.map((trigger) => {
        if (trigger.name === nodeName && trigger.metadata?.ui?.nodePosition) {
            return {
                ...trigger,
                metadata: {
                    ...trigger.metadata,
                    ui: {
                        ...trigger.metadata.ui,
                        nodePosition: undefined,
                    },
                },
            };
        }

        return trigger;
    });

    const {nodes, setNodes} = useWorkflowDataStore.getState();

    const previousWorkflow = workflow;
    const previousNodes = nodes;

    const updatedNodes = nodes.map((node) => {
        if (node.id === nodeName) {
            return {
                ...node,
                data: {
                    ...node.data,
                    metadata: {
                        ...(node.data as NodeDataType).metadata,
                        ui: {
                            ...(node.data as NodeDataType).metadata?.ui,
                            nodePosition: undefined,
                        },
                    },
                },
            };
        }

        return node;
    });

    setNodes(updatedNodes);

    useWorkflowDataStore.getState().setWorkflow({
        ...workflow,
        definition: updatedDefinition,
        tasks: updatedTasks,
        triggers: updatedTriggers,
    });

    incrementLayoutResetCounter();

    if (isWorkflowMutating(workflow.id!)) {
        return;
    }

    setWorkflowMutating(workflow.id!, true);

    updateWorkflowMutation.mutate(
        {
            id: workflow.id!,
            workflow: {
                definition: updatedDefinition,
                version: workflow.version,
            },
        },
        {
            onError: () => {
                setNodes(previousNodes);

                useWorkflowDataStore.getState().setWorkflow(previousWorkflow);

                incrementLayoutResetCounter();
            },
            onSettled: () => {
                setWorkflowMutating(workflow.id!, false);
            },
            onSuccess: (updatedWorkflow) => {
                const currentWorkflow = useWorkflowDataStore.getState().workflow;

                useWorkflowDataStore.getState().setWorkflow({
                    ...currentWorkflow,
                    version: updatedWorkflow.version,
                });

                invalidateWorkflowQueries();
            },
        }
    );
}
