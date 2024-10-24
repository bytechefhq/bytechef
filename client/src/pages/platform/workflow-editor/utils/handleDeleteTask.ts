import {CONDITION_CASE_FALSE, CONDITION_CASE_TRUE, SPACE} from '@/shared/constants';
import {Workflow, WorkflowTask} from '@/shared/middleware/automation/configuration';
import {WorkflowNodeOutputKeys} from '@/shared/queries/platform/workflowNodeOutputs.queries';
import {
    ComponentType,
    ConditionTaskDispatcherType,
    NodeType,
    WorkflowDefinitionType,
    WorkflowTaskType,
} from '@/shared/types';
import {QueryClient, UseMutationResult} from '@tanstack/react-query';
import {Node, NodeProps} from 'reactflow';

import {WorkflowTaskDataType} from '../stores/useWorkflowDataStore';

interface HandleDeleteTaskProps {
    componentNames: Array<string>;
    currentComponent?: ComponentType;
    currentNode?: NodeType;
    data: NodeProps['data'];
    getNode: (id: string) => Node | undefined;
    id: string;
    queryClient: QueryClient;
    setCurrentComponent: (component: ComponentType | undefined) => void;
    setCurrentNode: (node: NodeType | undefined) => void;
    setWorkflow: (workflow: Workflow & WorkflowTaskDataType) => void;
    updateWorkflowMutation: UseMutationResult<Workflow, unknown, {id: string; workflow: Workflow}>;
    workflow: Workflow & WorkflowTaskDataType;
}

export default function handleDeleteTask({
    componentNames,
    currentComponent,
    currentNode,
    data,
    getNode,
    id,
    queryClient,
    setCurrentComponent,
    setCurrentNode,
    setWorkflow,
    updateWorkflowMutation,
    workflow,
}: HandleDeleteTaskProps) {
    const node = getNode(id);

    if (!node || !workflow?.definition) {
        return;
    }

    if (!workflow?.definition) {
        return;
    }

    const workflowDefinition: WorkflowDefinitionType = JSON.parse(workflow?.definition);

    const {tasks: workflowTasks} = workflowDefinition;

    if (!workflowTasks) {
        return;
    }

    let updatedTasks = workflowTasks;

    if (data.conditionData) {
        updatedTasks = workflowTasks.map((task: WorkflowTaskType) => {
            if (task.name !== data.conditionData.conditionId) {
                return task;
            }

            const {conditionCase} = data.conditionData;

            let {caseFalse, caseTrue} = (task as ConditionTaskDispatcherType).parameters;

            if (conditionCase === CONDITION_CASE_TRUE) {
                caseTrue = caseTrue.filter((childTask) => childTask.name !== data.name);
            } else if (conditionCase === CONDITION_CASE_FALSE) {
                caseFalse = caseFalse.filter((childTask) => childTask.name !== data.name);
            }

            return {
                ...task,
                parameters: {
                    ...task.parameters,
                    caseFalse,
                    caseTrue,
                },
            };
        });
    } else {
        updatedTasks = workflowTasks.filter((task: WorkflowTask) => task.name !== data.name);
    }

    updateWorkflowMutation.mutate(
        {
            id: workflow.id!,
            workflow: {
                definition: JSON.stringify(
                    {
                        ...workflowDefinition,
                        tasks: updatedTasks,
                    },
                    null,
                    SPACE
                ),
                version: workflow.version,
            },
        },
        {
            onSuccess: () => {
                queryClient.invalidateQueries({
                    queryKey: WorkflowNodeOutputKeys.filteredPreviousWorkflowNodeOutputs({
                        id: workflow.id!,
                        lastWorkflowNodeName: currentNode?.name,
                    }),
                });

                setWorkflow({
                    ...workflow,
                    componentNames: componentNames.filter((componentName) => componentName !== data.componentName),
                    tasks: updatedTasks,
                });

                if (currentNode?.name === data.name) {
                    setCurrentNode(undefined);
                }

                if (currentComponent?.workflowNodeName === data.name) {
                    setCurrentComponent(undefined);
                }
            },
        }
    );
}
