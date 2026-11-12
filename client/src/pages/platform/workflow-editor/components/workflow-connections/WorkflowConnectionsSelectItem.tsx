import Badge from '@/components/Badge/Badge';
import {SelectItem} from '@/components/Select/Select';
import {ConnectionI} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import EnvironmentBadge from '@/shared/components/EnvironmentBadge';

/** The select value of the item that clears a task's connection. */
export const CLEAR_CONNECTION_VALUE = 'clear';

interface WorkflowConnectionsSelectItemProps {
    connection: ConnectionI;
}

const WorkflowConnectionsSelectItem = ({connection}: WorkflowConnectionsSelectItemProps) => (
    <SelectItem
        className="[&>span:last-child]:block [&>span:last-child]:min-w-0 [&>span:last-child]:flex-1"
        value={String(connection.id)}
    >
        <div className="flex w-full min-w-0 items-center gap-1">
            <span className="min-w-0 flex-1 truncate">{connection.name}</span>

            {!!connection.tags?.length && (
                <span className="min-w-0 truncate text-xs text-content-neutral-secondary" data-connection-detail>
                    {connection.tags.map((tag) => tag.name).join(', ')}
                </span>
            )}

            {connection.environmentId !== undefined && (
                <span className="shrink-0" data-connection-detail>
                    <EnvironmentBadge environmentId={+connection.environmentId} />
                </span>
            )}

            {connection.credentialStatus === 'INVALID' && (
                <span className="shrink-0">
                    <Badge className="uppercase" label="Invalid" styleType="destructive-outline" weight="semibold" />
                </span>
            )}
        </div>
    </SelectItem>
);

export default WorkflowConnectionsSelectItem;
