import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from '@/components/ui/collapsible';
import {Skeleton} from '@/components/ui/skeleton';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import ConnectionDialog from '@/shared/components/connection/ConnectionDialog';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {useGetConnectionDefinitionQuery} from '@/shared/queries/platform/connectionDefinitions.queries';
import {useQueryClient} from '@tanstack/react-query';
import {ChevronDownIcon, PlusIcon} from 'lucide-react';
import {useCallback, useMemo, useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {twMerge} from 'tailwind-merge';

import WorkflowConnectionsSelectItem, {CLEAR_CONNECTION_VALUE} from './WorkflowConnectionsSelectItem';
import WorkflowConnectionsTaskRow from './WorkflowConnectionsTaskRow';
import {
    WorkflowConnectionChangeI,
    WorkflowConnectionGroupI,
    getGroupConnectionChanges,
    getUnassignedConnectionChanges,
    isConnectionOverride,
} from './utils/workflowConnectionGroups';

export interface WorkflowConnectionsGroupProps {
    /** Supplies the connector icon. */
    componentDefinition?: ComponentDefinitionBasic;
    /** A batch is running somewhere in the sheet. */
    disabled: boolean;
    group: WorkflowConnectionGroupI;
    onChangesRequest: (changes: WorkflowConnectionChangeI[]) => void;
    /** This group's batch is running. */
    pending: boolean;
    title: string;
}

function countLabel(count: number, noun: string): string {
    return `${count} ${noun}${count === 1 ? '' : 's'}`;
}

const WorkflowConnectionsGroup = ({
    componentDefinition,
    disabled,
    group,
    onChangesRequest,
    pending,
    title,
}: WorkflowConnectionsGroupProps) => {
    const [showConnectionDialog, setShowConnectionDialog] = useState(false);
    const [tasksOpen, setTasksOpen] = useState(false);

    const connectionDialogAllowed = useWorkflowNodeDetailsPanelStore((state) => state.connectionDialogAllowed);

    const {
        ConnectionKeys,
        useCreateConnectionMutation,
        useGetComponentDefinitionsQuery,
        useGetConnectionTagsQuery,
        useGetConnectionsQuery,
    } = useWorkflowEditor();

    const {componentName, componentVersion, distinctConnectionCount, entries, overrideCount} = group;

    const {data: connectionDefinition} = useGetConnectionDefinitionQuery({componentName, componentVersion});

    const {data: connections} = useGetConnectionsQuery(
        {componentName, connectionVersion: connectionDefinition?.version},
        !!connectionDefinition
    );

    const {data: dialogComponentDefinition} = useGetComponentDefinitionQuery(
        {componentName, componentVersion},
        showConnectionDialog
    );

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({}, showConnectionDialog);

    const queryClient = useQueryClient();

    const unassignedChanges = useMemo(() => getUnassignedConnectionChanges(group), [group]);

    const groupConnectionId = group.connectionId;
    const hasConnections = !!connections?.length;
    const mixed = groupConnectionId === undefined && distinctConnectionCount > 1;
    const multipleTasks = entries.length > 1;
    const singleEntry = entries.length === 1 ? entries[0] : undefined;

    const meta =
        overrideCount > 0
            ? `${countLabel(entries.length, 'task')}, ${countLabel(overrideCount, 'override')}`
            : countLabel(entries.length, 'task');

    const handleGroupValueChange = useCallback(
        (value: string) => {
            if (value === CLEAR_CONNECTION_VALUE && singleEntry) {
                onChangesRequest([
                    {
                        connectionId: undefined,
                        previousConnectionId: singleEntry.connectionId,
                        workflowConnectionKey: singleEntry.workflowConnectionKey,
                        workflowNodeName: singleEntry.workflowNodeName,
                    },
                ]);

                return;
            }

            onChangesRequest(getGroupConnectionChanges(group, Number(value)));
        },
        [group, onChangesRequest, singleEntry]
    );

    const handleConnectionCreate = useCallback(
        async (connectionId: number) => {
            await queryClient.invalidateQueries({queryKey: ConnectionKeys.connections});

            onChangesRequest(getGroupConnectionChanges(group, connectionId));
        },
        [ConnectionKeys.connections, group, onChangesRequest, queryClient]
    );

    return (
        <Collapsible asChild onOpenChange={setTasksOpen} open={tasksOpen}>
            <fieldset
                aria-busy={pending}
                className="flex min-w-0 flex-col gap-3 rounded-lg border border-stroke-neutral-secondary bg-surface-neutral-primary p-3"
            >
                <legend className="sr-only">{title}</legend>

                <div className="flex min-w-0 items-center gap-3">
                    {componentDefinition?.icon && (
                        <InlineSVG className="size-5 flex-none" src={componentDefinition.icon} />
                    )}

                    <div className="min-w-0 flex-1">
                        <div className="flex min-w-0 items-center gap-2">
                            <span className="truncate text-sm font-semibold text-content-neutral-primary">{title}</span>

                            {pending && <LoadingIcon className="m-0 size-3.5 text-content-neutral-secondary" />}
                        </div>

                        <p className="text-xs text-content-neutral-secondary">{meta}</p>

                        {singleEntry && (
                            <p className="truncate text-xs text-content-neutral-secondary">
                                <span className="text-content-neutral-primary">{singleEntry.name}</span>

                                {` ${singleEntry.description}`}
                            </p>
                        )}
                    </div>

                    <Badge
                        label={`${group.connectedCount}/${entries.length}`}
                        styleType={group.missingCount > 0 ? 'warning-outline' : 'secondary-filled'}
                        weight="semibold"
                    />

                    {multipleTasks && (
                        <CollapsibleTrigger asChild>
                            <Button size="xs" variant="ghost">
                                {tasksOpen ? 'Hide tasks' : 'Show tasks'}

                                <ChevronDownIcon
                                    aria-hidden="true"
                                    className={twMerge(
                                        'transition-transform motion-reduce:transition-none',
                                        tasksOpen && 'rotate-180'
                                    )}
                                />
                            </Button>
                        </CollapsibleTrigger>
                    )}
                </div>

                <div className="flex min-w-0 flex-col gap-2">
                    <div className="flex min-w-0 gap-2">
                        {connections === undefined && <Skeleton className="h-9 flex-1" />}

                        {hasConnections && (
                            <Select
                                disabled={disabled}
                                onValueChange={handleGroupValueChange}
                                value={groupConnectionId === undefined ? '' : String(groupConnectionId)}
                            >
                                <SelectTrigger
                                    aria-label={`${title} connection`}
                                    className="min-w-0 flex-1 overflow-hidden bg-surface-neutral-primary text-left [&>span]:block [&>span]:min-w-0 [&>span]:flex-1 [&>span]:overflow-hidden"
                                >
                                    <SelectValue
                                        placeholder={
                                            mixed
                                                ? `Mixed: ${distinctConnectionCount} connections`
                                                : 'Choose Connection...'
                                        }
                                    />
                                </SelectTrigger>

                                <SelectContent>
                                    {singleEntry?.connectionId !== undefined && (
                                        <SelectItem
                                            className="text-content-neutral-secondary"
                                            value={CLEAR_CONNECTION_VALUE}
                                        >
                                            Select...
                                        </SelectItem>
                                    )}

                                    {connections.map((connection) => (
                                        <WorkflowConnectionsSelectItem connection={connection} key={connection.id} />
                                    ))}
                                </SelectContent>
                            </Select>
                        )}

                        {hasConnections && connectionDialogAllowed && (
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <Button
                                        aria-label={`Create ${title} connection`}
                                        disabled={disabled}
                                        icon={<PlusIcon />}
                                        onClick={() => setShowConnectionDialog(true)}
                                        size="icon"
                                        variant="outline"
                                    />
                                </TooltipTrigger>

                                <TooltipContent>{`Create ${title} connection`}</TooltipContent>
                            </Tooltip>
                        )}

                        {connections !== undefined &&
                            !hasConnections &&
                            (connectionDialogAllowed ? (
                                <Button
                                    className="w-full"
                                    disabled={disabled}
                                    icon={<PlusIcon />}
                                    label="Create connection"
                                    onClick={() => setShowConnectionDialog(true)}
                                    variant="outline"
                                />
                            ) : (
                                <p className="text-sm text-content-neutral-secondary">No connections available.</p>
                            ))}
                    </div>

                    {unassignedChanges.length > 0 && (
                        <Button
                            className="self-start"
                            disabled={disabled}
                            label={`Use for all ${title} tasks`}
                            onClick={() => onChangesRequest(unassignedChanges)}
                            size="xs"
                            variant="outline"
                        />
                    )}

                    {mixed && (
                        <p className="text-xs text-content-neutral-secondary">
                            {`Choosing a connection applies it to all ${entries.length} tasks.`}
                        </p>
                    )}

                    {overrideCount > 0 && (
                        <p className="text-xs text-content-neutral-secondary">
                            Tasks marked Override keep their own connection.
                        </p>
                    )}
                </div>

                {multipleTasks && (
                    <CollapsibleContent>
                        <ul className="flex flex-col gap-1 rounded-md bg-surface-main p-1">
                            {entries.map((entry) => (
                                <WorkflowConnectionsTaskRow
                                    connections={connections}
                                    disabled={disabled}
                                    entry={entry}
                                    groupConnectionId={groupConnectionId}
                                    key={entry.id}
                                    onChangesRequest={onChangesRequest}
                                    override={isConnectionOverride(group, entry)}
                                    resetSlotReserved={overrideCount > 0}
                                />
                            ))}
                        </ul>
                    </CollapsibleContent>
                )}

                {showConnectionDialog && componentDefinitions && dialogComponentDefinition && (
                    <ConnectionDialog
                        componentDefinition={dialogComponentDefinition}
                        componentDefinitions={componentDefinitions}
                        connectionTagsQueryKey={ConnectionKeys.connectionTags}
                        connectionsQueryKey={ConnectionKeys.connections}
                        onClose={() => setShowConnectionDialog(false)}
                        onConnectionCreate={handleConnectionCreate}
                        useCreateConnectionMutation={useCreateConnectionMutation}
                        useGetConnectionTagsQuery={useGetConnectionTagsQuery}
                    />
                )}
            </fieldset>
        </Collapsible>
    );
};

export default WorkflowConnectionsGroup;
