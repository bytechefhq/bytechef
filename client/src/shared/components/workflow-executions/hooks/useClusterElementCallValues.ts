import {ClusterElementCallTaskExecutionI} from '@/shared/components/workflow-executions/util/toClusterElementCallTaskExecutions';
import {useEditorJobFileLogsQuery, useJobFileLogsQuery} from '@/shared/middleware/graphql';
import {useMemo} from 'react';

const LOGS_PAGE_SIZE = 100;

const TOOL_CALL_LOG_ENTRY_TEXT = '"toolName":';

interface ToolCallLogEntryI {
    inputs?: unknown;
    output?: unknown;
    toolName: string;
}

interface UseClusterElementCallValuesProps {
    callExecution?: ClusterElementCallTaskExecutionI;
    isEditorEnvironment?: boolean;
    jobId?: string;
}

interface UseToolCallLogEntriesProps {
    clusterElementCallId?: string;
    enabled: boolean;
    isEditorEnvironment?: boolean;
    jobId?: string;
    taskExecutionId?: string;
}

interface ClusterElementCallValuesI {
    input?: unknown;
    isLoading: boolean;
    output?: unknown;
}

interface ToolCallLogEntriesI {
    entries: ToolCallLogEntryI[];
    isLoaded: boolean;
    isLoading: boolean;
}

const parseValue = (value: unknown) => {
    if (typeof value !== 'string') {
        return value;
    }

    try {
        return JSON.parse(value);
    } catch {
        return value;
    }
};

const parseToolCallLogEntry = (message: string): ToolCallLogEntryI | undefined => {
    try {
        const parsed = JSON.parse(message);

        if (
            parsed &&
            typeof parsed === 'object' &&
            typeof parsed.toolName === 'string' &&
            ('inputs' in parsed || 'output' in parsed)
        ) {
            return parsed as ToolCallLogEntryI;
        }
    } catch {
        return undefined;
    }

    return undefined;
};

const findInput = (entries: ToolCallLogEntryI[]) => entries.findLast((entry) => entry.inputs != null)?.inputs;

const findOutput = (entries: ToolCallLogEntryI[]) => entries.findLast((entry) => entry.output != null)?.output;

const getPrompt = (inputs: unknown) =>
    inputs && typeof inputs === 'object' && 'prompt' in inputs ? {prompt: inputs.prompt} : inputs;

function useToolCallLogEntries({
    clusterElementCallId,
    enabled,
    isEditorEnvironment,
    jobId,
    taskExecutionId,
}: UseToolCallLogEntriesProps): ToolCallLogEntriesI {
    const queryVariables = {
        filter: clusterElementCallId
            ? {clusterElementCallId, searchText: TOOL_CALL_LOG_ENTRY_TEXT, taskExecutionId}
            : null,
        jobId: jobId || '',
        page: 0,
        size: LOGS_PAGE_SIZE,
    };

    const productionQuery = useJobFileLogsQuery(queryVariables, {enabled: enabled && !isEditorEnvironment});

    const editorQuery = useEditorJobFileLogsQuery(queryVariables, {enabled: enabled && isEditorEnvironment === true});

    const logEntries = isEditorEnvironment
        ? editorQuery.data?.editorJobFileLogs.content
        : productionQuery.data?.jobFileLogs.content;

    const isLoading = enabled && (isEditorEnvironment ? editorQuery.isLoading : productionQuery.isLoading);

    const entries = useMemo(
        () =>
            (logEntries ?? [])
                .map((logEntry) => parseToolCallLogEntry(logEntry.message))
                .filter((entry): entry is ToolCallLogEntryI => entry !== undefined),
        [logEntries]
    );

    return {entries, isLoaded: enabled && logEntries !== undefined, isLoading};
}

export default function useClusterElementCallValues({
    callExecution,
    isEditorEnvironment,
    jobId,
}: UseClusterElementCallValuesProps): ClusterElementCallValuesI {
    const hasOwnValues = callExecution?.input !== undefined || callExecution?.output !== undefined;
    const enabled = !!callExecution && !hasOwnValues && !!jobId;

    const ownEntries = useToolCallLogEntries({
        clusterElementCallId: callExecution?.clusterElementCallId,
        enabled,
        isEditorEnvironment,
        jobId,
        taskExecutionId: callExecution?.parentTaskExecutionId,
    });

    const subagentTaskCallId = callExecution?.subagentTaskCallId;
    const readsTaskCall = ownEntries.isLoaded && ownEntries.entries.length === 0 && !!subagentTaskCallId;

    const taskCallEntries = useToolCallLogEntries({
        clusterElementCallId: subagentTaskCallId,
        enabled: readsTaskCall,
        isEditorEnvironment,
        jobId,
        taskExecutionId: callExecution?.parentTaskExecutionId,
    });

    if (!callExecution || hasOwnValues) {
        return {input: callExecution?.input, isLoading: false, output: callExecution?.output};
    }

    if (readsTaskCall) {
        return {
            input: getPrompt(findInput(taskCallEntries.entries)),
            isLoading: taskCallEntries.isLoading,
            output: parseValue(findOutput(taskCallEntries.entries)),
        };
    }

    return {
        input: findInput(ownEntries.entries),
        isLoading: ownEntries.isLoading,
        output: parseValue(findOutput(ownEntries.entries)),
    };
}
