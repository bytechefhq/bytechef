import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {TaskDispatcherContextType} from '@/shared/types';

import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import getRecursivelyUpdatedTasks from './getRecursivelyUpdatedTasks';
import {applyGraphMemberInsertion} from './graph/graphMemberInsertion';
import {TASK_DISPATCHER_CONFIG} from './taskDispatcherConfig';

interface InsertTaskDispatcherSubtaskProps {
    newTask: WorkflowTask;
    placeholderId?: string;
    taskDispatcherContext: TaskDispatcherContextType;
    tasks: Array<WorkflowTask>;
}

/**
 * Insert a new task into the task dispatcher subtask list.
 */
export default function insertTaskDispatcherSubtask({
    newTask,
    placeholderId,
    taskDispatcherContext,
    tasks,
}: InsertTaskDispatcherSubtaskProps): Array<WorkflowTask> {
    const taskDispatcherId = taskDispatcherContext.taskDispatcherId;

    const componentName = taskDispatcherId?.split('_')[0] as keyof typeof TASK_DISPATCHER_CONFIG;

    const config = TASK_DISPATCHER_CONFIG[componentName];

    if (!config) {
        console.error(`Unknown task dispatcher type: ${componentName}`);

        return tasks;
    }

    const {extractContextFromPlaceholder, getSubtasks, getTask, initializeParameters, updateTaskParameters} = config;

    let targetTaskDispatcher = tasks.find((task) => task.name === taskDispatcherId);

    if (!targetTaskDispatcher) {
        targetTaskDispatcher = getTask({taskDispatcherId, tasks});
    }

    if (!targetTaskDispatcher) {
        return tasks;
    }

    if (!targetTaskDispatcher.parameters) {
        targetTaskDispatcher.parameters = initializeParameters();
    }

    let context: TaskDispatcherContextType = {...taskDispatcherContext};

    if (placeholderId && context.index === 0) {
        if (componentName === 'parallel') {
            context = {
                ...context,
                index: targetTaskDispatcher.parameters?.tasks?.length,
            };
        } else if (componentName === 'graph') {
            // Mirrors the `parallel` branch above: `graph`'s single add-node placeholder is a
            // bare `<graphId>-graph-placeholder` carrying no index at all, so
            // `getContextFromPlaceholderNode`'s graph branch pins `context.index` to 0 (the
            // generic trailing-segment parse would yield NaN) and the real append position is
            // read straight off the CURRENT `nodes` length here. That pinned 0 is what makes
            // this branch reachable — if it ever stops being set, adding a graph node silently
            // becomes `splice(NaN, 0, task)`.
            context = {
                ...context,
                index: targetTaskDispatcher.parameters?.nodes?.length,
            };
        } else if (componentName === 'each') {
            context = {
                ...context,
                index: 0,
            };
        } else {
            const placeholderContext = extractContextFromPlaceholder(placeholderId);

            context = {...context, ...placeholderContext};
        }
    }

    if (componentName === 'each') {
        const updatedTaskDispatcherTask = {
            ...targetTaskDispatcher,
            parameters: {
                ...targetTaskDispatcher.parameters,
                iteratee: newTask,
            },
        };

        return getRecursivelyUpdatedTasks(tasks, updatedTaskDispatcherTask);
    }

    const subtasks = getSubtasks({context, task: targetTaskDispatcher});

    let updatedSubtasks: Array<WorkflowTask>;

    if (context.index === undefined || context.index === -1 || typeof context.index !== 'number') {
        updatedSubtasks = [...subtasks, newTask];
    } else {
        updatedSubtasks = [...subtasks];

        updatedSubtasks.splice(context.index, 0, newTask);

        // A graph member is a free-form frame child, placed in both dimensions by the user and
        // never laid out by dagre, so it has no main axis to give up. Clearing one would leave a
        // half-defined `{[crossAxis]: n, [mainAxis]: undefined}` that still reads as a pinned
        // position downstream (`containsNodePosition`), crosses into frame-child coordinates as
        // NaN, and is no longer repairable by `placeGraphMembers` — whose "unplaced" test is the
        // presence of `nodePosition`, which the half-cleared object satisfies.
        if (componentName !== 'graph') {
            clearMainAxisAfterInsertionPoint(updatedSubtasks, context.index);
        }
    }

    let updatedTaskDispatcherTask = updateTaskParameters({context, task: targetTaskDispatcher, updatedSubtasks});

    if (componentName === 'graph') {
        updatedTaskDispatcherTask = applyGraphMemberInsertion({
            nodes: useWorkflowDataStore.getState().nodes,
            pendingConnection: consumeGraphPendingConnection(targetTaskDispatcher.name),
            previousGraphTask: targetTaskDispatcher,
            updatedGraphTask: updatedTaskDispatcherTask,
        });
    }

    return getRecursivelyUpdatedTasks(tasks, updatedTaskDispatcherTask);
}

/**
 * Clears the main-axis of the saved positions of the subtasks after the insertion point, in place,
 * so dagre can shift them along the chain to make room — while preserving the cross-axis the user
 * customized. Only meaningful for dispatchers whose subtasks dagre actually lays out along a chain.
 */
function clearMainAxisAfterInsertionPoint(subtasks: Array<WorkflowTask>, insertionIndex: number): void {
    const direction = useLayoutDirectionStore.getState().layoutDirection;
    const mainAxis = direction === 'TB' ? 'y' : 'x';
    const crossAxis = direction === 'TB' ? 'x' : 'y';

    for (let subtaskIndex = insertionIndex + 1; subtaskIndex < subtasks.length; subtaskIndex++) {
        const subtask = subtasks[subtaskIndex];

        if (subtask.metadata?.ui?.nodePosition) {
            const savedCrossValue = subtask.metadata.ui.nodePosition[crossAxis];

            subtasks[subtaskIndex] = {
                ...subtask,
                metadata: {
                    ...subtask.metadata,
                    ui: {
                        ...subtask.metadata.ui,
                        nodePosition: {
                            [crossAxis]: savedCrossValue,
                            [mainAxis]: undefined,
                        } as {x: number; y: number},
                    },
                },
            };
        }
    }
}

/**
 * Takes the pending connection raised for `graphId`, clearing it so the next add starts clean.
 *
 * A pending connection belongs to exactly one graph — a release over another graph's frame raised
 * its own — so one raised elsewhere is left in place rather than consumed here.
 */
function consumeGraphPendingConnection(graphId: string) {
    const {graphPendingConnection, setGraphPendingConnection} = useWorkflowEditorStore.getState();

    if (graphPendingConnection?.graphId !== graphId) {
        return undefined;
    }

    setGraphPendingConnection(undefined);

    return graphPendingConnection;
}
