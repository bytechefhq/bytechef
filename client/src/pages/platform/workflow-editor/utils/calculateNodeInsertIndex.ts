import useWorkflowDataStore from '../stores/useWorkflowDataStore';

export default function calculateNodeInsertIndex(targetId: string): number {
    const workflow = useWorkflowDataStore.getState().workflow;

    const nextTaskIndex = workflow.tasks?.findIndex((task) => task.name === targetId) ?? 0;

    const conditionTasks =
        workflow.tasks?.slice(0, nextTaskIndex).filter((task) => task?.type.includes('condition/')) || [];

    const loopTasks = workflow.tasks?.slice(0, nextTaskIndex).filter((task) => task?.type.includes('loop/')) || [];

    const branchTasks = workflow.tasks?.slice(0, nextTaskIndex).filter((task) => task?.type.includes('branch/')) || [];

    let tasksInConditions = 0;
    let tasksInLoops = 0;
    let tasksInBranches = 0;

    if (conditionTasks.length) {
        tasksInConditions = conditionTasks.reduce((count, conditionTask) => {
            const caseTrueTasks = conditionTask.parameters?.caseTrue?.length || 0;
            const caseFalseTasks = conditionTask.parameters?.caseFalse?.length || 0;

            return count + caseTrueTasks + caseFalseTasks;
        }, 0);
    }

    if (loopTasks.length) {
        tasksInLoops = loopTasks.reduce((count, loopTask) => count + loopTask.parameters?.iteratee?.length || 0, 0);
    }

    if (branchTasks.length) {
        tasksInBranches = branchTasks.reduce((count, branchTask) => {
            const caseKeys = ['default', Object.keys(branchTask.parameters?.cases || {})];

            return count + caseKeys.length;
        }, 0);
    }

    return nextTaskIndex - tasksInConditions - tasksInLoops - tasksInBranches;
}
