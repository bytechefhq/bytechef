import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {ScrollArea, ScrollBar} from '@/components/ui/scroll-area';
import JsonView from '@/shared/components/JsonView';
import {
    type EditorJobFileLogsQuery,
    type JobFileLogsQuery,
    LogEntry,
    LogLevel,
    type TriggerExecutionFileLogsQuery,
    useEditorJobFileLogsQuery,
    useJobFileLogsQuery,
    useTriggerExecutionFileLogsQuery,
} from '@/shared/middleware/graphql';
import {
    AlertCircleIcon,
    AlertTriangleIcon,
    BugIcon,
    ChevronsDownUpIcon,
    ChevronsUpDownIcon,
    InfoIcon,
    MessageSquareIcon,
} from 'lucide-react';
import {useMemo, useState} from 'react';
import {createPortal} from 'react-dom';
import {twMerge} from 'tailwind-merge';

interface WorkflowExecutionLogsContentProps {
    isEditorEnvironment?: boolean;
    jobId?: string;
    taskExecutionId?: string;
    toolbarContainer?: HTMLElement | null;
    triggerExecutionId?: string;
}

type LogsPageType = JobFileLogsQuery['jobFileLogs'];

interface LogsQueryResultI<TData> {
    data?: TData;
    error: unknown;
    isLoading: boolean;
}

interface SelectedLogsQueryI {
    error: unknown;
    isLoading: boolean;
    logsData?: LogsPageType;
}

interface SelectLogsQueryProps {
    editorQuery: LogsQueryResultI<EditorJobFileLogsQuery>;
    isEditorEnvironment?: boolean;
    isTriggerLogs: boolean;
    productionQuery: LogsQueryResultI<JobFileLogsQuery>;
    triggerQuery: LogsQueryResultI<TriggerExecutionFileLogsQuery>;
}

/**
 * A row's logs come from exactly one of the three stores, so the active query decides the loading, error and content
 * of the panel together. Reading them from separate conditionals would let the panel show one query's spinner beside
 * another's entries.
 */
const selectLogsQuery = ({
    editorQuery,
    isEditorEnvironment,
    isTriggerLogs,
    productionQuery,
    triggerQuery,
}: SelectLogsQueryProps): SelectedLogsQueryI => {
    if (isEditorEnvironment) {
        return {
            error: editorQuery.error,
            isLoading: editorQuery.isLoading,
            logsData: editorQuery.data?.editorJobFileLogs,
        };
    }

    if (isTriggerLogs) {
        return {
            error: triggerQuery.error,
            isLoading: triggerQuery.isLoading,
            logsData: triggerQuery.data?.triggerExecutionFileLogs,
        };
    }

    return {
        error: productionQuery.error,
        isLoading: productionQuery.isLoading,
        logsData: productionQuery.data?.jobFileLogs,
    };
};

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

const LOG_LEVELS = [LogLevel.Trace, LogLevel.Debug, LogLevel.Info, LogLevel.Warn, LogLevel.Error];

const LogLevelBadge = ({level}: {level: LogLevel}) => {
    const {className, icon} = LOG_LEVEL_BADGE_CONFIG[level] || LOG_LEVEL_BADGE_CONFIG[LogLevel.Info];

    return (
        <Badge
            className={twMerge('min-w-24 border-0', className)}
            icon={icon}
            label={level}
            styleType="secondary-filled"
        />
    );
};

const tryParseJson = (message: string): object | null => {
    try {
        const parsed = JSON.parse(message);

        if (typeof parsed === 'object' && parsed !== null) {
            return parsed;
        }

        return null;
    } catch {
        return null;
    }
};

const LogEntryMessage = ({collapsed, message}: {collapsed?: boolean; message: string}) => {
    const parsedJson = useMemo(() => tryParseJson(message), [message]);

    if (parsedJson) {
        return (
            <div className="flex-1 overflow-x-auto text-nowrap">
                <JsonView
                    collapsed={collapsed}
                    fallback={<span className="text-sm">{message}</span>}
                    name={false}
                    src={parsedJson}
                />
            </div>
        );
    }

    return <span className="min-w-0 flex-1 text-sm break-words">{message}</span>;
};

interface LogEntryRowProps {
    collapsed?: boolean;
    entry: LogEntry;
    showComponentName: boolean;
}

const LogEntryRow = ({collapsed, entry, showComponentName}: LogEntryRowProps) => {
    const [isExpanded, setIsExpanded] = useState(collapsed === false);
    const hasError = entry.exceptionType || entry.exceptionMessage || entry.stackTrace;

    return (
        <div
            className={twMerge(
                'border-b border-stroke-neutral-secondary p-2',
                hasError && 'cursor-pointer hover:bg-surface-neutral-secondary'
            )}
            onClick={() => hasError && setIsExpanded(!isExpanded)}
        >
            <div className="flex items-start gap-2">
                <span className="shrink-0 text-xs text-muted-foreground">
                    {new Date(entry.timestamp).toLocaleTimeString()}
                </span>

                <LogLevelBadge level={entry.level} />

                {showComponentName && (
                    <span className="shrink-0 rounded bg-surface-neutral-secondary px-1.5 py-0.5 text-xs font-medium text-muted-foreground">
                        {entry.componentName}

                        {entry.componentOperationName && `:${entry.componentOperationName}`}
                    </span>
                )}

                <LogEntryMessage collapsed={collapsed} message={entry.message} />
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

                            <span className="text-xs break-words">{entry.exceptionMessage}</span>
                        </div>
                    )}

                    {entry.stackTrace && (
                        <div>
                            <span className="text-xs font-semibold text-muted-foreground">Stack Trace:</span>

                            <pre className="mt-1 overflow-x-auto text-xs whitespace-pre-wrap text-muted-foreground">
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
    toolbarContainer,
    triggerExecutionId,
}: WorkflowExecutionLogsContentProps) => {
    const [collapsed, setCollapsed] = useState(true);
    const [expansionVersion, setExpansionVersion] = useState(0);
    const [hiddenLevels, setHiddenLevels] = useState<LogLevel[]>([]);
    const [page] = useState(0);
    const [size] = useState(100);

    const isTriggerLogs = !isEditorEnvironment && !!triggerExecutionId;
    const jobLogsFilter = taskExecutionId ? {taskExecutionId} : null;

    const productionQuery = useJobFileLogsQuery(
        {
            filter: jobLogsFilter,
            jobId: jobId || '',
            page,
            size,
        },
        {
            enabled: !isEditorEnvironment && !isTriggerLogs && !!jobId,
        }
    );

    const triggerQuery = useTriggerExecutionFileLogsQuery(
        {
            filter: null,
            page,
            size,
            triggerExecutionId: triggerExecutionId || '',
        },
        {
            enabled: isTriggerLogs,
        }
    );

    const editorQuery = useEditorJobFileLogsQuery(
        {
            filter: jobLogsFilter,
            jobId: jobId || '',
            page,
            size,
        },
        {
            enabled: isEditorEnvironment === true && !!jobId,
        }
    );

    const {error, isLoading, logsData} = selectLogsQuery({
        editorQuery,
        isEditorEnvironment,
        isTriggerLogs,
        productionQuery,
        triggerQuery,
    });

    const logs = useMemo(() => logsData?.content || [], [logsData]);

    const levelCounts = useMemo(
        () =>
            LOG_LEVELS.map((level) => ({
                count: logs.filter((logEntry) => logEntry.level === level).length,
                level,
            })).filter((levelCount) => levelCount.count > 0),
        [logs]
    );

    const visibleLogs = useMemo(
        () => logs.filter((logEntry) => !hiddenLevels.includes(logEntry.level)),
        [hiddenLevels, logs]
    );

    const allExpanded = collapsed === false;

    const handleCollapsedChange = (nextCollapsed: boolean) => {
        setCollapsed(nextCollapsed);
        setExpansionVersion(expansionVersion + 1);
    };

    const handleLevelToggle = (level: LogLevel) => {
        setHiddenLevels(
            hiddenLevels.includes(level)
                ? hiddenLevels.filter((hiddenLevel) => hiddenLevel !== level)
                : [...hiddenLevels, level]
        );
    };

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

    const toolbar = (
        <div
            className={twMerge(
                'flex shrink-0 items-center gap-1',
                !toolbarContainer && 'border-b border-stroke-neutral-secondary p-2'
            )}
        >
            {levelCounts.map(({count, level}) => {
                const levelVisible = !hiddenLevels.includes(level);

                return (
                    <Button
                        aria-pressed={levelVisible}
                        className={twMerge(
                            'border-0',
                            levelVisible ? LOG_LEVEL_BADGE_CONFIG[level].className : 'line-through opacity-60'
                        )}
                        icon={LOG_LEVEL_BADGE_CONFIG[level].icon}
                        key={level}
                        label={`${level} (${count})`}
                        onClick={() => handleLevelToggle(level)}
                        size="xs"
                        variant="ghost"
                    />
                );
            })}

            <Button
                aria-label={allExpanded ? 'Collapse all' : 'Expand all'}
                className="ml-auto"
                icon={allExpanded ? <ChevronsDownUpIcon /> : <ChevronsUpDownIcon />}
                onClick={() => handleCollapsedChange(allExpanded)}
                size="iconXs"
                title={allExpanded ? 'Collapse all' : 'Expand all'}
                variant="ghost"
            />
        </div>
    );

    return (
        <div className="flex h-full min-h-0 flex-col">
            {toolbarContainer ? createPortal(toolbar, toolbarContainer) : toolbar}

            <ScrollArea className="min-h-0 flex-1">
                <div className="divide-y divide-stroke-neutral-secondary">
                    {visibleLogs.map((logEntry, index) => (
                        <LogEntryRow
                            collapsed={collapsed}
                            entry={logEntry}
                            key={`${logEntry.timestamp}-${index}-${expansionVersion}`}
                            showComponentName={!taskExecutionId && !isTriggerLogs}
                        />
                    ))}
                </div>

                <ScrollBar orientation="horizontal" />

                <ScrollBar orientation="vertical" />
            </ScrollArea>
        </div>
    );
};

export default WorkflowExecutionLogsContent;
