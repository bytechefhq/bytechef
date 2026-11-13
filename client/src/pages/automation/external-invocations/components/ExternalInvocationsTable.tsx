import Badge from '@/components/Badge/Badge';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {
    EXECUTIONS_TABLE_CELL_CLASS_NAME,
    EXECUTIONS_TABLE_HEADER_CLASS_NAME,
    EXECUTIONS_TABLE_HEAD_CLASS_NAME,
    EXECUTIONS_TABLE_ROW_CLASS_NAME,
    EXECUTIONS_TABLE_STRIPED_ROW_CLASS_NAME,
    PINNED_CELL_CLASS_NAME,
    PINNED_HEAD_CLASS_NAME,
    PINNED_STRIPED_CELL_CLASS_NAME,
} from '@/shared/components/workflow-executions/workflowExecutionsTableClassNames';
import {type LucideIcon, PuzzleIcon, SparklesIcon, WorkflowIcon, ZapIcon} from 'lucide-react';
import {KeyboardEvent} from 'react';
import {twMerge} from 'tailwind-merge';

import {
    type ExternalInvocationLogItemType,
    SOURCE_LABELS,
    STATUS_STYLE_TYPES,
    formatCreatedDate,
    targetLabel,
} from '../utils/externalInvocationLabels';
import ExternalInvocationsDropdownMenu from './ExternalInvocationsDropdownMenu';

interface ExternalInvocationsTableProps {
    a2aServerNames?: Record<string, string>;
    mcpServerNames?: Record<string, string>;
    onSelect?: (externalInvocationLog: ExternalInvocationLogItemType) => void;
    onViewJob?: (jobId: number) => void;
    showConnectedUser?: boolean;
    showSource?: boolean;
    externalInvocationLogs: Array<ExternalInvocationLogItemType>;
}

const PINNED_IDLE_CELL_CLASS_NAME =
    'sticky right-0 z-10 bg-surface-neutral-primary [tr:nth-child(even)>&]:bg-surface-neutral-secondary';

interface InvocationTypeI {
    icon: LucideIcon;
    label: string;
}

const KIND_TYPES: Record<string, InvocationTypeI> = {
    COMPONENT: {icon: PuzzleIcon, label: 'Component tool'},
    WORKFLOW: {icon: WorkflowIcon, label: 'Workflow tool'},
};

const SOURCE_TYPES: Record<string, InvocationTypeI> = {
    A2A_SERVER: {icon: SparklesIcon, label: 'Skill'},
    API_ACTION: {icon: ZapIcon, label: 'Action'},
};

const serverLabel = (
    externalInvocationLog: ExternalInvocationLogItemType,
    mcpServerNames: Record<string, string> | undefined,
    a2aServerNames: Record<string, string> | undefined
): string => {
    if (externalInvocationLog.mcpServerId != null) {
        return mcpServerNames?.[String(externalInvocationLog.mcpServerId)] || '—';
    }

    if (externalInvocationLog.a2aServerId != null) {
        return a2aServerNames?.[String(externalInvocationLog.a2aServerId)] || '—';
    }

    return '—';
};

const handleRowKeyDown = (event: KeyboardEvent<HTMLTableRowElement>, openRow: () => void) => {
    if (event.target !== event.currentTarget || (event.key !== 'Enter' && event.key !== ' ')) {
        return;
    }

    event.preventDefault();

    openRow();
};

const InvocationTypeCell = ({externalInvocationLog}: {externalInvocationLog: ExternalInvocationLogItemType}) => {
    const invocationType = SOURCE_TYPES[externalInvocationLog.source] || KIND_TYPES[externalInvocationLog.kind];

    if (!invocationType) {
        return <>{externalInvocationLog.kind}</>;
    }

    const Icon = invocationType.icon;

    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <span aria-label={invocationType.label} className="inline-flex align-middle" role="img">
                    <Icon className="size-4 text-content-neutral-secondary" />
                </span>
            </TooltipTrigger>

            <TooltipContent>{invocationType.label}</TooltipContent>
        </Tooltip>
    );
};

const ExternalInvocationsTable = ({
    a2aServerNames,
    externalInvocationLogs,
    mcpServerNames,
    onSelect,
    onViewJob,
    showConnectedUser = false,
    showSource = true,
}: ExternalInvocationsTableProps) => (
    <Table>
        <TableHeader className={EXECUTIONS_TABLE_HEADER_CLASS_NAME}>
            <TableRow>
                <TableHead className={EXECUTIONS_TABLE_HEAD_CLASS_NAME}>Status</TableHead>

                <TableHead className={EXECUTIONS_TABLE_HEAD_CLASS_NAME}>Name</TableHead>

                <TableHead className={twMerge(EXECUTIONS_TABLE_HEAD_CLASS_NAME, 'text-center')}>Type</TableHead>

                {showSource && <TableHead className={EXECUTIONS_TABLE_HEAD_CLASS_NAME}>Source</TableHead>}

                <TableHead className={EXECUTIONS_TABLE_HEAD_CLASS_NAME}>Server</TableHead>

                {showConnectedUser && (
                    <TableHead className={EXECUTIONS_TABLE_HEAD_CLASS_NAME}>Connected User</TableHead>
                )}

                <TableHead className={EXECUTIONS_TABLE_HEAD_CLASS_NAME}>Target</TableHead>

                <TableHead className={EXECUTIONS_TABLE_HEAD_CLASS_NAME}>Duration</TableHead>

                <TableHead className={EXECUTIONS_TABLE_HEAD_CLASS_NAME}>Date</TableHead>

                {onViewJob && (
                    <TableHead className={twMerge(EXECUTIONS_TABLE_HEAD_CLASS_NAME, PINNED_HEAD_CLASS_NAME, 'w-px')}>
                        Actions
                    </TableHead>
                )}
            </TableRow>
        </TableHeader>

        <TableBody>
            {externalInvocationLogs.map((externalInvocationLog) => {
                const jobId = externalInvocationLog.jobId;
                const viewable = onViewJob != null && jobId != null;

                const openRow = onSelect
                    ? () => onSelect(externalInvocationLog)
                    : viewable
                      ? () => onViewJob(Number(jobId))
                      : undefined;

                return (
                    <TableRow
                        className={twMerge(
                            openRow ? EXECUTIONS_TABLE_ROW_CLASS_NAME : 'border-0',
                            EXECUTIONS_TABLE_STRIPED_ROW_CLASS_NAME
                        )}
                        key={externalInvocationLog.id}
                        onClick={openRow}
                        onKeyDown={openRow && ((event) => handleRowKeyDown(event, openRow))}
                        tabIndex={openRow ? 0 : undefined}
                    >
                        <TableCell className={EXECUTIONS_TABLE_CELL_CLASS_NAME}>
                            <Badge
                                className="border-transparent uppercase"
                                label={externalInvocationLog.status}
                                styleType={STATUS_STYLE_TYPES[externalInvocationLog.status] || 'secondary-filled'}
                                weight="semibold"
                            />
                        </TableCell>

                        <TableCell className={EXECUTIONS_TABLE_CELL_CLASS_NAME}>
                            {externalInvocationLog.invocationName || '—'}
                        </TableCell>

                        <TableCell className={twMerge(EXECUTIONS_TABLE_CELL_CLASS_NAME, 'text-center')}>
                            <InvocationTypeCell externalInvocationLog={externalInvocationLog} />
                        </TableCell>

                        {showSource && (
                            <TableCell className={EXECUTIONS_TABLE_CELL_CLASS_NAME}>
                                {SOURCE_LABELS[externalInvocationLog.source] || externalInvocationLog.source}
                            </TableCell>
                        )}

                        <TableCell className={EXECUTIONS_TABLE_CELL_CLASS_NAME}>
                            {serverLabel(externalInvocationLog, mcpServerNames, a2aServerNames)}
                        </TableCell>

                        {showConnectedUser && (
                            <TableCell className={EXECUTIONS_TABLE_CELL_CLASS_NAME}>
                                {externalInvocationLog.externalUserId || externalInvocationLog.connectedUserId || '—'}
                            </TableCell>
                        )}

                        <TableCell className={EXECUTIONS_TABLE_CELL_CLASS_NAME}>
                            {targetLabel(externalInvocationLog)}
                        </TableCell>

                        <TableCell className={EXECUTIONS_TABLE_CELL_CLASS_NAME}>
                            {externalInvocationLog.durationMs}ms
                        </TableCell>

                        <TableCell className={EXECUTIONS_TABLE_CELL_CLASS_NAME}>
                            {formatCreatedDate(externalInvocationLog.createdDate)}
                        </TableCell>

                        {onViewJob && (
                            <TableCell
                                className={
                                    viewable
                                        ? twMerge(
                                              EXECUTIONS_TABLE_CELL_CLASS_NAME,
                                              PINNED_CELL_CLASS_NAME,
                                              PINNED_STRIPED_CELL_CLASS_NAME
                                          )
                                        : twMerge(EXECUTIONS_TABLE_CELL_CLASS_NAME, PINNED_IDLE_CELL_CLASS_NAME)
                                }
                                onClick={(event) => event.stopPropagation()}
                            >
                                {viewable && (
                                    <ExternalInvocationsDropdownMenu
                                        jobId={jobId}
                                        onViewJob={onViewJob}
                                        status={externalInvocationLog.status}
                                    />
                                )}
                            </TableCell>
                        )}
                    </TableRow>
                );
            })}
        </TableBody>
    </Table>
);

export default ExternalInvocationsTable;
