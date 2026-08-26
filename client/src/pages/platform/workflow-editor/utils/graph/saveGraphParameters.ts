import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {GraphTransitionType, UpdateWorkflowMutationType} from '@/shared/types';

import useWorkflowDataStore from '../../stores/useWorkflowDataStore';
import getRecursivelyUpdatedTasks from '../getRecursivelyUpdatedTasks';
import saveWorkflowDefinition from '../saveWorkflowDefinition';
import {TASK_DISPATCHER_CONFIG} from '../taskDispatcherConfig';

/**
 * Applies `updater` to a `graph/v1` task's `parameters` and persists the result. Reads the live
 * definition from the workflow data store (rather than taking it as a parameter) to match every
 * other save* helper's call convention, so callers stay one-liners at the many places the canvas
 * mutates graph parameters (add/remove/reorder a transition, move a member, etc.).
 */
export function saveGraphParameters(
    graphId: string,
    updater: (parameters: Record<string, unknown>) => Record<string, unknown>,
    updateWorkflowMutation: UpdateWorkflowMutationType
): void {
    const {workflow} = useWorkflowDataStore.getState();

    if (!workflow.definition) {
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
        console.error(`Graph task ${graphId} not found in workflow definition`);

        return;
    }

    const updatedGraphTask: WorkflowTask = {
        ...graphTask,
        parameters: updater(graphTask.parameters ?? {}),
    };

    const updatedWorkflowTasks = getRecursivelyUpdatedTasks(tasks, updatedGraphTask);

    saveWorkflowDefinition({
        updateWorkflowMutation,
        updatedWorkflowTasks,
    });
}

/**
 * `saveGraphParameters` narrowed to the transitions list: applies `mutate` to
 * `parameters.transitions` and leaves every other graph parameter untouched.
 *
 * Every transition edit has this exact shape — repoint, delete and reorder from the edge popover
 * and the Transitions panel, and delete from the canvas key handler — so the "spread the rest,
 * replace transitions, default a missing list to empty" step lives here once rather than at each
 * call site, where forgetting the spread would silently drop `startNode` and `maxTransitions`.
 */
export function saveGraphTransitions(
    graphId: string,
    mutate: (transitions: Array<GraphTransitionType>) => Array<GraphTransitionType>,
    updateWorkflowMutation: UpdateWorkflowMutationType
): void {
    saveGraphParameters(
        graphId,
        (parameters) => ({
            ...parameters,
            transitions: mutate((parameters.transitions as Array<GraphTransitionType> | undefined) ?? []),
        }),
        updateWorkflowMutation
    );
}
