import {BranchCaseType} from '@/shared/types';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';

export default function calculateNodeInsertIndex(targetId: string): number {
    const workflow = useWorkflowDataStore.getState().workflow;

    const nextTaskIndex = workflow.tasks?.findIndex((task) => task.name === targetId) ?? 0;

    const conditionTasks =
        workflow.tasks?.slice(0, nextTaskIndex).filter((task) => task?.type.includes('condition/')) || [];

    const loopTasks = workflow.tasks?.slice(0, nextTaskIndex).filter((task) => task?.type.includes('loop/')) || [];

    const branchTasks = workflow.tasks?.slice(0, nextTaskIndex).filter((task) => task?.type.includes('branch/')) || [];

    const eachTasks = workflow.tasks?.slice(0, nextTaskIndex).filter((task) => task?.type.includes('each/')) || [];

    let tasksInConditions = 0;
    let tasksInLoops = 0;
    let tasksInBranches = 0;
    let tasksInEach = 0;

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

    return nextTaskIndex - tasksInConditions - tasksInLoops - tasksInBranches - tasksInEach;
}
