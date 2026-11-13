import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ConnectionI} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {RotateCcwIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

import WorkflowConnectionsSelectItem, {CLEAR_CONNECTION_VALUE} from './WorkflowConnectionsSelectItem';
import {WorkflowConnectionChangeI, WorkflowConnectionEntryI} from './utils/workflowConnectionGroups';

interface WorkflowConnectionsTaskRowProps {
    connections?: ConnectionI[];
    disabled: boolean;
    entry: WorkflowConnectionEntryI;
    groupConnectionId?: number;
    onChangesRequest: (changes: WorkflowConnectionChangeI[]) => void;
    override: boolean;
    resetSlotReserved: boolean;
}

const WorkflowConnectionsTaskRow = ({
    connections,
    disabled,
    entry,
    groupConnectionId,
    onChangesRequest,
    override,
    resetSlotReserved,
}: WorkflowConnectionsTaskRowProps) => {
    const {connectionId, description, name, required, workflowConnectionKey, workflowNodeName} = entry;

    const requestConnection = (nextConnectionId: number | undefined) =>
        onChangesRequest([
            {
                connectionId: nextConnectionId,
                previousConnectionId: connectionId,
                workflowConnectionKey,
                workflowNodeName,
            },
        ]);

    const handleValueChange = (value: string) =>
        requestConnection(value === CLEAR_CONNECTION_VALUE ? undefined : Number(value));

    return (
        <li className="flex items-center gap-3 px-2 py-1.5">
            <div className="min-w-0 flex-1">
                <div className="flex min-w-0 items-center gap-2">
                    <span className="truncate text-sm text-content-neutral-primary">{name}</span>

                    {override && <Badge label="Override" styleType="primary-outline" />}
                </div>

                {description && <p className="truncate text-xs text-content-neutral-secondary">{description}</p>}
            </div>

            {/* With no connection to pick, a dropdown would open empty, so the row only states what it lacks. */}

            {connections?.length === 0 ? (
                <Badge className="shrink-0" label="Needs a connection" styleType="warning-outline" />
            ) : (
                <Select
                    disabled={disabled}
                    onValueChange={handleValueChange}
                    value={connectionId === undefined ? '' : String(connectionId)}
                >
                    <SelectTrigger
                        aria-label={`${name} connection`}
                        className={twMerge(
                            'w-48 shrink-0 overflow-hidden bg-surface-neutral-primary text-left [&_[data-connection-detail]]:hidden [&>span]:block [&>span]:min-w-0 [&>span]:flex-1 [&>span]:overflow-hidden',
                            required && connectionId === undefined && 'data-[placeholder]:text-content-warning-primary'
                        )}
                        size="sm"
                    >
                        <SelectValue placeholder="Choose Connection..." />
                    </SelectTrigger>

                    <SelectContent align="end" className="w-auto max-w-80 min-w-(--radix-select-trigger-width)">
                        {connectionId !== undefined && (
                            <SelectItem className="text-content-neutral-secondary" value={CLEAR_CONNECTION_VALUE}>
                                Select...
                            </SelectItem>
                        )}

                        {connections?.map((connection) => (
                            <WorkflowConnectionsSelectItem connection={connection} key={connection.id} />
                        ))}
                    </SelectContent>
                </Select>
            )}

            {resetSlotReserved && (
                <div className="flex size-8 shrink-0 items-center justify-center">
                    {override && groupConnectionId !== undefined && (
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Button
                                    aria-label="Use group connection"
                                    disabled={disabled}
                                    icon={<RotateCcwIcon />}
                                    onClick={() => requestConnection(groupConnectionId)}
                                    size="iconSm"
                                    variant="ghost"
                                />
                            </TooltipTrigger>

                            <TooltipContent>Use group connection</TooltipContent>
                        </Tooltip>
                    )}
                </div>
            )}
        </li>
    );
};

export default WorkflowConnectionsTaskRow;
