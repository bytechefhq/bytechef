type ExecutionKindType = 'call' | 'task' | 'trigger';

interface SelectableExecutionI {
    id?: string;
}

const getExecutionKind = (execution: SelectableExecutionI): ExecutionKindType => {
    if ('clusterElementCallId' in execution) {
        return 'call';
    }

    return 'workflowTrigger' in execution ? 'trigger' : 'task';
};

export const getExecutionSelectionKey = (execution: SelectableExecutionI | undefined): string =>
    execution?.id ? `${getExecutionKind(execution)}:${execution.id}` : '';

export const isSameExecution = (
    firstExecution: SelectableExecutionI | undefined,
    secondExecution: SelectableExecutionI | undefined
): boolean => {
    const firstKey = getExecutionSelectionKey(firstExecution);

    return firstKey !== '' && firstKey === getExecutionSelectionKey(secondExecution);
};
