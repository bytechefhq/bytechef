import {Job, TaskExecution, TriggerExecution} from '@/shared/middleware/automation/workflow/execution';
import {WorkflowTestExecution} from '@/shared/middleware/platform/workflow/test';
import {TabValueType} from '@/shared/types';

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

export const getErrorItem = (
    workflowTestExecution?: WorkflowTestExecution
): TaskExecution | TriggerExecution | undefined => {
    if (!workflowTestExecution) return undefined;

    if (workflowTestExecution.triggerExecution && workflowTestExecution.triggerExecution.error) {
        return workflowTestExecution.triggerExecution;
    }

    const failedTask = workflowTestExecution.job?.taskExecutions?.find((task) => task.error && 'workflowTask' in task);

    if (failedTask) {
        return failedTask;
    }

    return undefined;
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
    tab: TabValueType;
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

    // Logs tab has its own display mechanism, no dialog content
    if (tab === 'logs') {
        return false;
    }

    return false;
};
