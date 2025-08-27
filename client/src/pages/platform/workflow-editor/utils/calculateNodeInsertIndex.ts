import {BranchCaseType} from '@/shared/types';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';

export default function calculateNodeInsertIndex(targetId: string): number {
    const workflow = useWorkflowDataStore.getState().workflow;
    const {tasks} = workflow;

    const nextTaskIndex = tasks?.findIndex((task) => task.name === targetId) ?? 0;

    const conditionTasks = tasks?.slice(0, nextTaskIndex).filter((task) => task?.type.includes('condition/')) || [];
    const loopTasks = tasks?.slice(0, nextTaskIndex).filter((task) => task?.type.includes('loop/')) || [];
    const branchTasks = tasks?.slice(0, nextTaskIndex).filter((task) => task?.type.includes('branch/')) || [];
    const eachTasks = tasks?.slice(0, nextTaskIndex).filter((task) => task?.type.includes('each/')) || [];
    const forkJoinTasks = tasks?.slice(0, nextTaskIndex).filter((task) => task?.type.includes('fork-join/')) || [];

    let tasksInConditions = 0;
    let tasksInLoops = 0;
    let tasksInBranches = 0;
    let tasksInEach = 0;
    let tasksInForkJoins = 0;

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

    return nextTaskIndex - tasksInConditions - tasksInLoops - tasksInBranches - tasksInEach - tasksInForkJoins;
}
