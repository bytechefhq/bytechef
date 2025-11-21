import {Job, TaskExecution, TriggerExecution} from '@/shared/middleware/automation/workflow/execution';
import {WorkflowTestExecution} from '@/shared/middleware/platform/workflow/test';

export interface TaskTreeItemProps {
    children: TaskTreeItemProps[];
    iterations?: TaskTreeItemProps[][];
    task: TaskExecution;
}

export const getTasksTree = (job: Job): TaskTreeItemProps[] => {
    if (!job?.taskExecutions) {
        return [];
    }

    const taskExecutions = job.taskExecutions;

    const tasksMap = taskExecutions.reduce((map, task) => {
        if (task.parentId) {
            const existing = map.get(task.parentId) || [];

            return new Map(map).set(task.parentId, [...existing, task]);
        }

        return map;
    }, new Map<string, TaskExecution[]>());

    const buildTasksTree = (task: TaskExecution): TaskTreeItemProps => {
        const matchingChildrenFromTasksMap = tasksMap.get(task.id || '') || [];
        const isLoopTask = typeof task.type === 'string' && task.type.toLowerCase().includes('loop');

        if (isLoopTask) {
            const iterationItems: TaskTreeItemProps[][] = [];
            let currentIterationItems: TaskTreeItemProps[] = [];
            let previousTaskNumber: number | undefined;

            matchingChildrenFromTasksMap.forEach((child) => {
                if (child.taskNumber === 0 && previousTaskNumber !== undefined && currentIterationItems.length > 0) {
                    iterationItems.push([...currentIterationItems]);

                    currentIterationItems = [];
                }

                currentIterationItems = [...currentIterationItems, buildTasksTree(child)];
                previousTaskNumber = child.taskNumber;
            });

            if (currentIterationItems.length > 0) {
                iterationItems.push([...currentIterationItems]);
            }

            return {children: [], iterations: iterationItems, task};
        }

        const children = matchingChildrenFromTasksMap.map((child) => buildTasksTree(child));

        return {children, task};
    };

    const topLevelTasks = taskExecutions.filter((task) => !task.parentId);

    return topLevelTasks.map((task) => buildTasksTree(task));
};

export const getFilteredOutput = (
    output: object | undefined,
    jobInputs: {[key: string]: object} | undefined,
    workflowTriggerName: string | undefined
): object | undefined => {
    if (jobInputs && workflowTriggerName && jobInputs[workflowTriggerName]) {
        return jobInputs[workflowTriggerName];
    }
    return output;
};

export const getInitialSelectedItem = (
    workflowTestExecution?: WorkflowTestExecution
): TaskExecution | TriggerExecution | undefined => {
    if (!workflowTestExecution) return undefined;

    if (workflowTestExecution.triggerExecution && 'workflowTrigger' in workflowTestExecution.triggerExecution) {
        return workflowTestExecution.triggerExecution as TriggerExecution;
    }

    if (
        workflowTestExecution.job?.taskExecutions?.[0] &&
        'workflowTask' in workflowTestExecution.job.taskExecutions[0]
    ) {
        return workflowTestExecution.job.taskExecutions[0] as TaskExecution;
    }

    return undefined;
};

interface GetDisplayValueProps {
    job: Job;
    selectedItem: TaskExecution | TriggerExecution | undefined;
    tab: 'input' | 'output' | 'error';
    triggerExecution?: TriggerExecution;
}

export const getDisplayValue = ({job, selectedItem, tab, triggerExecution}: GetDisplayValueProps) => {
    if (!selectedItem) {
        return undefined;
    }

    if (tab === 'input') {
        return selectedItem.input;
    }

    if (tab === 'error') {
        return selectedItem.error;
    }

    if (selectedItem.id === triggerExecution?.id && job.inputs && triggerExecution?.workflowTrigger?.name) {
        return getFilteredOutput(selectedItem.output, job.inputs, triggerExecution?.workflowTrigger?.name);
    }

    return selectedItem.output;
};

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export const hasValue = (value: any): boolean => {
    if (value === undefined || value === null) {
        return false;
    }

    if (typeof value !== 'object') {
        return true;
    }

    if (Array.isArray(value)) {
        return value.length > 0;
    }

    return Object.keys(value).length > 0;
};

export const hasDialogContentValue = ({job, selectedItem, tab, triggerExecution}: GetDisplayValueProps): boolean => {
    if (!selectedItem) {
        return false;
    }

    if (tab === 'input') {
        return hasValue(selectedItem.input);
    }

    if (tab === 'error') {
        return hasValue(selectedItem.error);
    }

    if (tab === 'output') {
        const displayValue = getDisplayValue({job, selectedItem, tab, triggerExecution});

        return hasValue(displayValue);
    }

    return false;
};
