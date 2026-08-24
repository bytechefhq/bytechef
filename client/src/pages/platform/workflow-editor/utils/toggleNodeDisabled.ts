import {SPACE} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {UpdateWorkflowMutationType} from '@/shared/types';

import useWorkflowDataStore, {runWithoutHistory} from '../stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import {flattenDefinitionTasks, isWorkflowTask} from './flattenDefinitionTasks';
import {
    consumePendingDefinition,
    isWorkflowMutating,
    setPendingDefinition,
    setWorkflowMutating,
} from './workflowMutationGuard';

/**
 * Flips a task's `disabled` flag in place and returns the resulting state. `false` is
 * never a valid stored value: an enabled task simply has no `disabled` key at all, so
 * re-enabling deletes the key rather than writing `false`.
 */
function flipDisabled(task: WorkflowTask): boolean {
    if (task.disabled) {
        delete task.disabled;

        return false;
    }

    task.disabled = true;

    return true;
}

/**
 * Returns a copy of `target` carrying the new `disabled` state, keeping the "`false` is
 * never stored" rule: enabling removes the key instead of writing `false`.
 */
function withDisabled<T extends object>(target: T, disabled: boolean): T {
    const updatedTarget = {...target} as Record<string, unknown>;

    if (disabled) {
        updatedTarget.disabled = true;
    } else {
        delete updatedTarget.disabled;
    }

    return updatedTarget as T;
}

/**
 * Applies `definition` to the store together with EVERY other surface that mirrors a
 * task's `disabled` flag: `workflow.tasks`, the React Flow `nodes[].data`, and the
 * details panel's `currentNode`.
 *
 * Patching `workflow.definition` alone (the shape `saveStickyNotes` uses, which is
 * correct there because sticky notes live in `metadata.ui` and touch no task) leaves the
 * other three stale, and a later panel save rebuilds the whole task from `currentNode`
 * with `disabled: undefined` -- silently re-enabling a node the user disabled. The
 * per-surface patching mirrors `useWorkflowDataStore`'s `updateWorkflowNodeParameters`.
 *
 * Flags are re-derived from the definition rather than threaded from the toggle, so a
 * revert (or a queued definition carrying several toggles) resyncs correctly too.
 * `flattenDefinitionTasks` is the same walk that builds `workflow.tasks` in the first
 * place, so the names line up by construction. Surfaces already holding the right value
 * are returned untouched, keeping node identities stable for React Flow.
 */
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

/**
 * Depth-first search for `workflowNodeName` across a list of tasks and,
 * for each task considered, its `pre`/`post`/`finalize` hook lists and its
 * `parameters` (where task dispatchers nest subtasks). Mutates the matching
 * task in place and stops as soon as a match is found. Returns the task's new
 * `disabled` state, or `undefined` when no task matched.
 */
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

        for (const nestedTasks of [task.pre, task.post, task.finalize]) {
            const hookOutcome = toggleInTasks(nestedTasks, workflowNodeName);

            if (hookOutcome !== undefined) {
                return hookOutcome;
            }
        }

        if (task.parameters) {
            const parameterOutcome = toggleInValue(task.parameters, workflowNodeName);

            if (parameterOutcome !== undefined) {
                return parameterOutcome;
            }
        }
    }

    return undefined;
}

/**
 * Walks an arbitrary task-dispatcher parameter value looking for nested
 * tasks: a list of tasks (e.g. `condition.caseTrue`), a single task object
 * (e.g. `each.iteratee`), a list of objects each carrying their own `tasks`
 * (e.g. `branch.cases`), or a list of lists (e.g. `fork-join.branches`) --
 * recursing through plain objects/arrays otherwise. Returns the matched task's
 * new `disabled` state, or `undefined` when no task matched.
 */
function toggleInValue(value: unknown, workflowNodeName: string): boolean | undefined {
    if (Array.isArray(value)) {
        if (value.some((item) => isWorkflowTask(item))) {
            return toggleInTasks(value, workflowNodeName);
        }

        for (const item of value) {
            const itemOutcome = toggleInValue(item, workflowNodeName);

            if (itemOutcome !== undefined) {
                return itemOutcome;
            }
        }

        return undefined;
    }

    if (value && typeof value === 'object') {
        if (isWorkflowTask(value)) {
            return toggleInTasks([value], workflowNodeName);
        }

        for (const nestedValue of Object.values(value)) {
            const nestedOutcome = toggleInValue(nestedValue, workflowNodeName);

            if (nestedOutcome !== undefined) {
                return nestedOutcome;
            }
        }

        return undefined;
    }

    return undefined;
}

interface ToggleNodeDisabledProps {
    updateWorkflowMutation: UpdateWorkflowMutationType;
    workflowNodeName: string;
}

/**
 * Single funnel for the disable/enable toggle: locates the named task
 * anywhere in the definition (top level, nested task-dispatcher lists,
 * `cases[].tasks`, single-map subtasks, `pre`/`post`/`finalize`), flips its
 * `disabled` flag, and persists it honoring the pending-definition queue so
 * overlapping saves are never lost.
 *
 * The persistence funnel (pending-definition queue, revert-on-error) mirrors
 * `saveStickyNotes`. The STORE update deliberately does NOT: sticky notes live in
 * `metadata.ui` and touch no task, so patching `workflow.definition` alone is correct
 * there. This toggle mutates a task, so it mirrors `updateWorkflowNodeParameters`
 * instead and keeps `workflow.tasks`, `nodes[].data` and the details panel's
 * `currentNode` in step with the definition. Leaving any of those stale lets a later
 * panel save (which rebuilds the task from `currentNode`) silently drop the flag.
 */
export function toggleNodeDisabled({updateWorkflowMutation, workflowNodeName}: ToggleNodeDisabledProps): void {
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

    const disabled = toggleInTasks(workflowDefinition.tasks, workflowNodeName);

    if (disabled === undefined) {
        return;
    }

    const updatedDefinition = JSON.stringify(workflowDefinition, null, SPACE);

    if (updatedDefinition === workflow.definition) {
        return;
    }

    const previousDefinition = workflow.definition;

    // Recorded as an undo step by the temporal store's definition equality
    applyDefinitionWithDisabledFlags(updatedDefinition);

    if (isWorkflowMutating(workflow.id!)) {
        // Queue the definition so it can be sent when the current mutation settles
        setPendingDefinition(workflow.id!, updatedDefinition);

        return;
    }

    fireToggleNodeDisabledMutation({
        definition: updatedDefinition,
        previousDefinition,
        updateWorkflowMutation,
        version: workflow.version,
        workflowId: workflow.id!,
    });
}

interface FireToggleNodeDisabledMutationProps {
    definition: string;
    previousDefinition: string;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    version?: number;
    workflowId: string;
}

function fireToggleNodeDisabledMutation({
    definition,
    previousDefinition,
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
                // Reverting a failed toggle save must not become an undo step
                runWithoutHistory(() => {
                    applyDefinitionWithDisabledFlags(previousDefinition);
                });
            },
            onSettled: () => {
                setWorkflowMutating(workflowId, false);

                // If another toggle save was skipped while this mutation was
                // in flight, fire it now with the latest queued definition.
                const pendingDefinition = consumePendingDefinition(workflowId);

                if (pendingDefinition) {
                    const currentWorkflow = useWorkflowDataStore.getState().workflow;

                    fireToggleNodeDisabledMutation({
                        definition: pendingDefinition,
                        previousDefinition: currentWorkflow.definition!,
                        updateWorkflowMutation,
                        version: currentWorkflow.version,
                        workflowId,
                    });
                }
            },
            onSuccess: (updatedWorkflow) => {
                const currentWorkflow = useWorkflowDataStore.getState().workflow;

                useWorkflowDataStore.getState().setWorkflow({
                    ...currentWorkflow,
                    version: updatedWorkflow.version,
                });
            },
        }
    );
}
