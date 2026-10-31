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

interface ClusterElementCallValuesI {
    input?: unknown;
    isLoading: boolean;
    output?: unknown;
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

export default function useClusterElementCallValues({
    callExecution,
    isEditorEnvironment,
    jobId,
}: UseClusterElementCallValuesProps): ClusterElementCallValuesI {
    const hasOwnValues = callExecution?.input !== undefined || callExecution?.output !== undefined;
    const enabled = !!callExecution && !hasOwnValues && !!jobId;

    const queryVariables = {
        filter: callExecution
            ? {
                  clusterElementCallId: callExecution.clusterElementCallId,
                  searchText: TOOL_CALL_LOG_ENTRY_TEXT,
                  taskExecutionId: callExecution.parentTaskExecutionId,
              }
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

    const toolCallLogEntries = useMemo(
        () =>
            (logEntries ?? [])
                .map((logEntry) => parseToolCallLogEntry(logEntry.message))
                .filter((entry): entry is ToolCallLogEntryI => entry !== undefined),
        [logEntries]
    );

    const inputEntry = toolCallLogEntries.findLast((entry) => entry.inputs != null);
    const outputEntry = toolCallLogEntries.findLast((entry) => entry.output != null);

    if (!callExecution || hasOwnValues) {
        return {input: callExecution?.input, isLoading: false, output: callExecution?.output};
    }

    return {
        input: inputEntry?.inputs,
        isLoading,
        output: parseValue(outputEntry?.output),
    };
}
