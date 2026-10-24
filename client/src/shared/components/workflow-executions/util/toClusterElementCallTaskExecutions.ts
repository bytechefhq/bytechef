import {
    ClusterElementCallRecord,
    ClusterElementCallRecordStatusEnum,
    TaskExecution,
    TaskExecutionStatusEnum,
    TriggerExecution,
} from '@/shared/middleware/platform/workflow/execution';

export interface ClusterElementCallTaskExecutionI extends TaskExecution {
    clusterElementCallId: string;
    clusterElementCallNote?: string;
    parentTaskExecutionId: string;
}

const WAITING_NOTE = 'Waiting for a response';
const STILL_RUNNING_NOTE = 'Still running when the agent finished';

const parseValue = (value?: string) => {
    if (value === undefined) {
        return undefined;
    }

    try {
        return JSON.parse(value);
    } catch {
        return value;
    }
};

const getTitle = (record: ClusterElementCallRecord) => record.toolName ?? record.workflowNodeName;

const getCallNumbers = (records: ClusterElementCallRecord[]) => {
    const callCountByTitle = new Map<string | undefined, number>();

    records.forEach((record) => {
        const title = getTitle(record);

        callCountByTitle.set(title, (callCountByTitle.get(title) ?? 0) + 1);
    });

    const nextNumberByTitle = new Map<string | undefined, number>();
    const numberByRecordId = new Map<string, number>();

    records
        .map((record, index) => ({index, record}))
        .sort(
            (first, second) =>
                (first.record.startDate?.getTime() ?? 0) - (second.record.startDate?.getTime() ?? 0) ||
                first.index - second.index
        )
        .forEach(({record}) => {
            const title = getTitle(record);
            const nextNumber = (nextNumberByTitle.get(title) ?? 0) + 1;

            nextNumberByTitle.set(title, nextNumber);

            if ((callCountByTitle.get(title) ?? 0) > 1) {
                numberByRecordId.set(record.id, nextNumber);
            }
        });

    return numberByRecordId;
};

const getNote = (record: ClusterElementCallRecord, taskStatus: TaskExecutionStatusEnum) => {
    if (record.status === ClusterElementCallRecordStatusEnum.Suspended) {
        return WAITING_NOTE;
    }

    if (
        record.status === ClusterElementCallRecordStatusEnum.Started &&
        taskStatus !== TaskExecutionStatusEnum.Started
    ) {
        return STILL_RUNNING_NOTE;
    }

    return undefined;
};

const getStatus = (status: ClusterElementCallRecordStatusEnum): TaskExecutionStatusEnum => {
    if (status === ClusterElementCallRecordStatusEnum.Completed) {
        return TaskExecutionStatusEnum.Completed;
    }

    if (status === ClusterElementCallRecordStatusEnum.Failed) {
        return TaskExecutionStatusEnum.Failed;
    }

    return TaskExecutionStatusEnum.Started;
};

export const isClusterElementCallTaskExecution = (
    execution: TaskExecution | TriggerExecution
): execution is ClusterElementCallTaskExecutionI => 'clusterElementCallId' in execution;

export default function toClusterElementCallTaskExecutions(
    taskExecution: TaskExecution
): ClusterElementCallTaskExecutionI[] {
    const records = taskExecution.clusterElementCallRecords ?? [];

    if (records.length === 0) {
        return [];
    }

    const callNumbers = getCallNumbers(records);
    const executionById = new Map<string, ClusterElementCallTaskExecutionI>();

    records.forEach((record) => {
        const callNumber = callNumbers.get(record.id);
        const baseTitle = getTitle(record);
        const title = callNumber === undefined ? baseTitle : `${baseTitle} #${callNumber}`;

        executionById.set(record.id, {
            clusterElementCallId: record.id,
            clusterElementCallNote: getNote(record, taskExecution.status),
            endDate: record.endDate,
            error: record.error === undefined ? undefined : {message: record.error},
            icon: `/icons/${record.componentName}.svg`,
            id: record.id,
            input: parseValue(record.input),
            jobId: taskExecution.jobId,
            output: parseValue(record.output),
            parentTaskExecutionId: taskExecution.id as string,
            priority: taskExecution.priority,
            startDate: record.startDate as Date,
            status: getStatus(record.status),
            title,
            workflowTask: {label: title, name: record.workflowNodeName},
        } as ClusterElementCallTaskExecutionI);
    });

    const topLevelExecutions: ClusterElementCallTaskExecutionI[] = [];

    records.forEach((record) => {
        const execution = executionById.get(record.id)!;
        const parentExecution = record.parentId ? executionById.get(record.parentId) : undefined;

        if (parentExecution) {
            parentExecution.children = [...(parentExecution.children ?? []), execution];
        } else {
            topLevelExecutions.push(execution);
        }
    });

    return topLevelExecutions;
}
