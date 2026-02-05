import {ScrollArea, ScrollBar} from '@/components/ui/scroll-area';
import {LogEntry, LogLevel, useEditorJobFileLogsQuery, useJobFileLogsQuery} from '@/shared/middleware/graphql';
import {AlertCircleIcon, AlertTriangleIcon, BugIcon, InfoIcon, MessageSquareIcon} from 'lucide-react';
import {useMemo, useState} from 'react';

interface WorkflowExecutionLogsContentProps {
    isEditorEnvironment?: boolean;
    jobId: string;
    taskExecutionId?: string;
}

const LOG_LEVEL_BADGE_CONFIG = {
    [LogLevel.Trace]: {
        className: 'bg-gray-100 text-gray-600',
        icon: <MessageSquareIcon className="size-3" />,
    },
    [LogLevel.Debug]: {
        className: 'bg-purple-100 text-purple-600',
        icon: <BugIcon className="size-3" />,
    },
    [LogLevel.Info]: {
        className: 'bg-blue-100 text-blue-600',
        icon: <InfoIcon className="size-3" />,
    },
    [LogLevel.Warn]: {
        className: 'bg-yellow-100 text-yellow-600',
        icon: <AlertTriangleIcon className="size-3" />,
    },
    [LogLevel.Error]: {
        className: 'bg-red-100 text-red-600',
        icon: <AlertCircleIcon className="size-3" />,
    },
};

const LogLevelBadge = ({level}: {level: LogLevel}) => {
    const {className, icon} = LOG_LEVEL_BADGE_CONFIG[level] || LOG_LEVEL_BADGE_CONFIG[LogLevel.Info];

    return (
        <span className={`inline-flex items-center gap-1 rounded px-1.5 py-0.5 text-xs font-medium ${className}`}>
            {icon}

            {level}
        </span>
    );
};

const LogEntryRow = ({entry}: {entry: LogEntry}) => {
    const [isExpanded, setIsExpanded] = useState(false);
    const hasError = entry.exceptionType || entry.exceptionMessage || entry.stackTrace;

    return (
        <div
            className={`border-b border-stroke-neutral-secondary p-2 ${hasError ? 'cursor-pointer hover:bg-surface-neutral-secondary' : ''}`}
            onClick={() => hasError && setIsExpanded(!isExpanded)}
        >
            <div className="flex items-start gap-2">
                <span className="shrink-0 text-xs text-muted-foreground">
                    {new Date(entry.timestamp).toLocaleTimeString()}
                </span>

                <LogLevelBadge level={entry.level} />

                <span className="shrink-0 rounded bg-surface-neutral-secondary px-1.5 py-0.5 text-xs font-medium text-muted-foreground">
                    {entry.componentName}

                    {entry.componentOperationName && `:${entry.componentOperationName}`}
                </span>

                <span className="flex-1 text-sm">{entry.message}</span>
            </div>

            {isExpanded && hasError && (
                <div className="mt-2 space-y-2 rounded bg-surface-neutral-secondary p-2">
                    {entry.exceptionType && (
                        <div>
                            <span className="text-xs font-semibold text-muted-foreground">Exception: </span>

                            <span className="text-xs text-content-destructive-primary">{entry.exceptionType}</span>
                        </div>
                    )}

                    {entry.exceptionMessage && (
                        <div>
                            <span className="text-xs font-semibold text-muted-foreground">Message: </span>

                            <span className="text-xs">{entry.exceptionMessage}</span>
                        </div>
                    )}

                    {entry.stackTrace && (
                        <div>
                            <span className="text-xs font-semibold text-muted-foreground">Stack Trace:</span>

                            <pre className="mt-1 overflow-x-auto whitespace-pre-wrap text-xs text-muted-foreground">
                                {entry.stackTrace}
                            </pre>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
};

const WorkflowExecutionLogsContent = ({
    isEditorEnvironment,
    jobId,
    taskExecutionId,
}: WorkflowExecutionLogsContentProps) => {
    const [page] = useState(0);
    const [size] = useState(100);

    const {
        data: productionLogsData,
        error: productionError,
        isLoading: isProductionLoading,
    } = useJobFileLogsQuery(
        {
            filter: taskExecutionId ? {taskExecutionId} : null,
            jobId,
            page,
            size,
        },
        {
            enabled: !isEditorEnvironment,
        }
    );

    const {
        data: editorLogsData,
        error: editorError,
        isLoading: isEditorLoading,
    } = useEditorJobFileLogsQuery(
        {
            filter: taskExecutionId ? {taskExecutionId} : null,
            jobId,
            page,
            size,
        },
        {
            enabled: isEditorEnvironment === true,
        }
    );

    const isLoading = isEditorEnvironment ? isEditorLoading : isProductionLoading;
    const error = isEditorEnvironment ? editorError : productionError;
    const logsData = isEditorEnvironment ? editorLogsData?.editorJobFileLogs : productionLogsData?.jobFileLogs;

    const logs = useMemo(() => logsData?.content || [], [logsData]);

    if (isLoading) {
        return (
            <div className="flex items-center justify-center p-4">
                <span className="text-sm text-muted-foreground">Loading logs...</span>
            </div>
        );
    }

    if (error) {
        return (
            <div className="flex items-center justify-center p-4">
                <span className="text-sm text-content-destructive-primary">Failed to load logs</span>
            </div>
        );
    }

    if (logs.length === 0) {
        return (
            <div className="flex items-center justify-center p-4">
                <span className="text-sm text-muted-foreground">No logs available</span>
            </div>
        );
    }

    return (
        <ScrollArea className="h-full">
            <div className="divide-y divide-stroke-neutral-secondary">
                {logs.map((logEntry, index) => (
                    <LogEntryRow entry={logEntry} key={`${logEntry.timestamp}-${index}`} />
                ))}
            </div>

            <ScrollBar orientation="horizontal" />

            <ScrollBar orientation="vertical" />
        </ScrollArea>
    );
};

export default WorkflowExecutionLogsContent;
