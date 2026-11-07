import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {GraphTransitionType, UpdateWorkflowMutationType} from '@/shared/types';

import useWorkflowDataStore from '../../stores/useWorkflowDataStore';
import getRecursivelyUpdatedTasks from '../getRecursivelyUpdatedTasks';
import saveWorkflowDefinition from '../saveWorkflowDefinition';
import {TASK_DISPATCHER_CONFIG} from '../taskDispatcherConfig';
import {enqueuePendingSave, isWorkflowMutating} from '../workflowMutationGuard';

interface SaveGraphParametersProps {
    graphId: string;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    updater: (parameters: Record<string, unknown>) => Record<string, unknown>;
}

export function saveGraphParameters({graphId, updateWorkflowMutation, updater}: SaveGraphParametersProps): void {
    const {workflow} = useWorkflowDataStore.getState();

    if (!workflow.definition) {
        return;
    }

    if (workflow.id && isWorkflowMutating(workflow.id)) {
        enqueuePendingSave(workflow.id, () => saveGraphParameters({graphId, updateWorkflowMutation, updater}));

        return;
    }

    let workflowDefinition;

    try {
        workflowDefinition = JSON.parse(workflow.definition);
    } catch (error) {
        console.error('Failed to parse workflow definition:', error);

        return;
    }

    const tasks: Array<WorkflowTask> = workflowDefinition.tasks ?? [];

    const graphTask = TASK_DISPATCHER_CONFIG.graph.getTask({taskDispatcherId: graphId, tasks});

    if (!graphTask) {
        const missingGraphTaskMessage = `Graph task ${graphId} not found in workflow definition`;

        console.error(missingGraphTaskMessage);

        return;
    }

    const graphParameters = graphTask.parameters ?? {};

    const updatedGraphParameters = updater(graphParameters);

    if (updatedGraphParameters === graphParameters) {
        return;
    }

    const updatedGraphTask: WorkflowTask = {
        ...graphTask,
        parameters: updatedGraphParameters,
    };

    const updatedWorkflowTasks = getRecursivelyUpdatedTasks(tasks, updatedGraphTask);

    void saveWorkflowDefinition({
        updateWorkflowMutation,
        updatedWorkflowTasks,
    });
}

interface SaveGraphTransitionsProps {
    graphId: string;
    mutate: (transitions: Array<GraphTransitionType>) => Array<GraphTransitionType>;
    updateWorkflowMutation: UpdateWorkflowMutationType;
}

export function saveGraphTransitions({graphId, mutate, updateWorkflowMutation}: SaveGraphTransitionsProps): void {
    const updateTransitions = (parameters: Record<string, unknown>) => {
        const currentTransitions = (parameters.transitions as Array<GraphTransitionType> | undefined) ?? [];

        const mutatedTransitions = mutate(currentTransitions);

        if (mutatedTransitions === currentTransitions) {
            return parameters;
        }

        return {
            ...parameters,
            transitions: mutatedTransitions,
        };
    };

    saveGraphParameters({graphId, updateWorkflowMutation, updater: updateTransitions});
}
