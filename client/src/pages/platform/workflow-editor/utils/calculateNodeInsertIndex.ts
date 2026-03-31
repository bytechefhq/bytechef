import {ON_ERROR_WIRE_KEY_ERROR_BRANCH, ON_ERROR_WIRE_KEY_MAIN_BRANCH} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {BranchCaseType} from '@/shared/types';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';

export function calculateNodeInsertIndexFromTasks(targetId: string, tasks: WorkflowTask[]): number {
    const nextTaskIndex = tasks.findIndex((task) => task.name === targetId);

    const conditionTasks = tasks.slice(0, nextTaskIndex).filter((task) => task?.type.includes('condition/')) || [];
    const loopTasks = tasks.slice(0, nextTaskIndex).filter((task) => task?.type.includes('loop/')) || [];
    const branchTasks = tasks.slice(0, nextTaskIndex).filter((task) => task?.type.includes('branch/')) || [];
    const eachTasks = tasks.slice(0, nextTaskIndex).filter((task) => task?.type.includes('each/')) || [];
    const forkJoinTasks = tasks.slice(0, nextTaskIndex).filter((task) => task?.type.includes('fork-join/')) || [];
    const mapTasks = tasks.slice(0, nextTaskIndex).filter((task) => task?.type.includes('map/')) || [];
    const onErrorTasks = tasks.slice(0, nextTaskIndex).filter((task) => task?.type.split('/')[0] === 'on-error') || [];

    let tasksInConditions = 0;
    let tasksInLoops = 0;
    let tasksInBranches = 0;
    let tasksInEach = 0;
    let tasksInForkJoins = 0;
    let tasksInMaps = 0;
    let tasksInOnError = 0;

    if (conditionTasks.length) {
        tasksInConditions = conditionTasks.reduce((count, conditionTask) => {
            const caseTrueTasks = conditionTask.parameters?.caseTrue?.length || 0;
            const caseFalseTasks = conditionTask.parameters?.caseFalse?.length || 0;

            return count + caseTrueTasks + caseFalseTasks;
        }, 0);
    }

    if (loopTasks.length) {
        tasksInLoops = loopTasks.reduce((count, loopTask) => count + (loopTask.parameters?.iteratee?.length || 0), 0);
    }

    if (branchTasks.length) {
        tasksInBranches = branchTasks.reduce((count, branchTask) => {
            const defaultTasks = branchTask.parameters?.default?.length || 0;

            const caseTasks = (branchTask.parameters?.cases || []).reduce(
                (caseCount: number, caseItem: BranchCaseType) => caseCount + (caseItem.tasks?.length || 0),
                0
            );

            return count + defaultTasks + caseTasks;
        }, 0);
    }

    if (eachTasks.length) {
        tasksInEach = eachTasks.reduce((count, eachTask) => {
            if (eachTask.parameters?.iteratee) {
                count += 1;
            }

            return count;
        }, 0);
    }

    if (forkJoinTasks.length) {
        tasksInForkJoins = forkJoinTasks.reduce(
            (count, forkJoinTask) => count + (forkJoinTask.parameters?.branches.flat().length || 0),
            0
        );
    }

    if (mapTasks.length) {
        tasksInMaps = mapTasks.reduce((count, mapTask) => count + (mapTask.parameters?.iteratee?.length || 0), 0);
    }

    if (onErrorTasks.length) {
        tasksInOnError = onErrorTasks.reduce((count, onErrorTask) => {
            const mainCount = Array.isArray(onErrorTask.parameters?.[ON_ERROR_WIRE_KEY_MAIN_BRANCH])
                ? onErrorTask.parameters[ON_ERROR_WIRE_KEY_MAIN_BRANCH].length
                : 0;
            const errorCount = Array.isArray(onErrorTask.parameters?.[ON_ERROR_WIRE_KEY_ERROR_BRANCH])
                ? onErrorTask.parameters[ON_ERROR_WIRE_KEY_ERROR_BRANCH].length
                : 0;

            return count + mainCount + errorCount;
        }, 0);
    }

    return (
        nextTaskIndex -
        tasksInConditions -
        tasksInLoops -
        tasksInBranches -
        tasksInEach -
        tasksInForkJoins -
        tasksInMaps -
        tasksInOnError
    );
}

/**
 * Calculates the insertion index for a new node before the target task.
 * Uses the workflow definition's top-level task array (the actual array
 * that will be spliced into) rather than the flat runtime task array,
 * which avoids fragile nested subtask counting.
 */
export default function calculateNodeInsertIndex(targetId: string): number {
    const workflow = useWorkflowDataStore.getState().workflow;

    if (workflow.definition) {
        try {
            const definition = JSON.parse(workflow.definition);
            const definitionTasks: WorkflowTask[] = definition.tasks ?? [];
            const targetIndex = definitionTasks.findIndex((task) => task.name === targetId);

            if (targetIndex !== -1) {
                return targetIndex;
            }
        } catch {
            // Fall through to flat task calculation
        }
    }

    const {tasks} = workflow;

    if (!tasks) {
        return 0;
    }

    return calculateNodeInsertIndexFromTasks(targetId, tasks);
}
