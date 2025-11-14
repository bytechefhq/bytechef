import {Job, TaskExecution, TriggerExecution} from '@/shared/middleware/automation/workflow/execution';

export interface TaskTreeNodeI {
    children: TaskTreeNodeI[];
    iterations?: TaskTreeNodeI[][];
    task: TaskExecution;
}

export const getTasksTree = (job: Job): TaskTreeNodeI[] => {
    if (!job?.taskExecutions) {
        return [];
    }

    const taskExecutions = job.taskExecutions;
    const tasksMap = new Map<string, TaskExecution[]>();

    taskExecutions.forEach((task) => {
        if (task.parentId) {
            const children = tasksMap.get(task.parentId) || [];

            children.push(task);

            tasksMap.set(task.parentId, children);
        }
    });

    const buildTasksTree = (task: TaskExecution): TaskTreeNodeI => {
        const matchingChildrenFromTasksMap = tasksMap.get(task.id || '') || [];

        const isLoop = typeof task.type === 'string' && task.type.toLowerCase().includes('loop');

        if (isLoop) {
            const iterations: TaskTreeNodeI[][] = [];
            let currentIteration: TaskTreeNodeI[] = [];
            let started = false;

            for (const child of matchingChildrenFromTasksMap) {
                if (started && child.taskNumber === 0) {
                    if (currentIteration.length) {
                        iterations.push(currentIteration);
                    }
                    currentIteration = [];
                }
                currentIteration.push(buildTasksTree(child));
                started = true;
            }

            if (currentIteration.length) {
                iterations.push(currentIteration);
            }

            return {children: [], iterations, task};
        }

        const children = matchingChildrenFromTasksMap.map((child) => buildTasksTree(child));

        return {children, task};
    };

    const topLevelTasks = taskExecutions.filter((task) => !task.parentId);

    return topLevelTasks.map((task) => buildTasksTree(task));
};

interface HandleTaskClickProps {
    setActiveTab: (value: 'input' | 'output' | 'error') => void;
    setSelectedItem: (taskExecution: TaskExecution | TriggerExecution) => void;
    taskExecution: TaskExecution | TriggerExecution;
}

export const handleTaskClick = ({setActiveTab, setSelectedItem, taskExecution}: HandleTaskClickProps) => {
    setActiveTab(taskExecution.error ? 'error' : 'input');
    setSelectedItem(taskExecution);
};

export const getFilteredOutput = (
    output: object | undefined,
    jobInputs: {[key: string]: object} | undefined,
    workflowTriggerName: string | undefined
): object | undefined => {
    let filteredOutput = output;

    if (jobInputs && Object.keys(jobInputs).length) {
        filteredOutput = Object.keys(jobInputs)
            .filter((key) => key === workflowTriggerName)
            .map((key) => jobInputs[key]);
    }

    return filteredOutput;
};

interface GetDisplayValueProps {
    job: Job;
    selectedItem: TaskExecution | TriggerExecution | undefined;
    tab: 'input' | 'output' | 'error';
    triggerExecution?: TriggerExecution;
}

export const getDisplayValue = ({job, selectedItem, tab, triggerExecution}: GetDisplayValueProps) => {
    if (!selectedItem) return undefined;

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

export const hasDialogContentValue = ({job, selectedItem, tab, triggerExecution}: GetDisplayValueProps): boolean => {
    if (!selectedItem) return false;

    if (tab === 'input') {
        return (
            selectedItem.input !== undefined &&
            (typeof selectedItem.input !== 'object' || Object.keys(selectedItem.input).length > 0)
        );
    }

    if (tab === 'error') {
        return selectedItem.error !== undefined;
    }

    if (tab === 'output') {
        const displayValue = getDisplayValue({job, selectedItem, tab, triggerExecution});
        return displayValue !== undefined;
    }

    return false;
};
