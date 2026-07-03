import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import EnvironmentBadge from '@/shared/components/EnvironmentBadge';
import {useGetWorkspaceConnectionsQuery} from '@/shared/queries/automation/connections.queries';
import {useGetConnectionDefinitionQuery} from '@/shared/queries/platform/connectionDefinitions.queries';
import {DataMessagePartProps, useThreadRuntime} from '@assistant-ui/react';
import {CheckIcon} from 'lucide-react';
import {useEffect, useMemo, useState} from 'react';

export interface SelectConnectionDataI {
    componentLabel: string;
    componentName: string;
    kind: 'select-connection';
}

/**
 * Renders the LLM's selectConnection tool result as a dropdown of the workspace's existing connections for a
 * component. Companion to {@code CreateConnectionMessage} (the "create new" intent). On pick, the choice is
 * dispatched as a system message ("User picked: <name>") via the assistant-ui thread runtime so the agent's
 * next turn reads it from chat memory. The dropdown dims once a later message lands on the thread.
 */
const SelectConnectionMessage = ({data}: DataMessagePartProps<SelectConnectionDataI>) => {
    const [pickedConnection, setPickedConnection] = useState<{id: number; name: string} | undefined>();
    const [supersededByLaterMessage, setSupersededByLaterMessage] = useState(false);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const threadRuntime = useThreadRuntime();

    const {data: connectionDefinition} = useGetConnectionDefinitionQuery(
        {componentName: data.componentName, componentVersion: 1},
        Boolean(data.componentName)
    );

    const {data: existingConnections} = useGetWorkspaceConnectionsQuery(
        {
            componentName: data.componentName,
            connectionVersion: connectionDefinition?.version,
            id: currentWorkspaceId!,
        },
        Boolean(connectionDefinition?.version) && currentWorkspaceId != null
    );

    const connections = useMemo(() => existingConnections ?? [], [existingConnections]);

    useEffect(() => {
        const initialMessageCount = threadRuntime.getState().messages.length;

        return threadRuntime.subscribe(() => {
            const currentCount = threadRuntime.getState().messages.length;

            if (currentCount > initialMessageCount) {
                setSupersededByLaterMessage(true);
            }
        });
    }, [threadRuntime]);

    const handleSelectChange = (value: string) => {
        const connectionId = Number(value);
        const connection = (existingConnections ?? []).find((candidate) => candidate.id === connectionId);

        if (!connection || connection.id == null) {
            return;
        }

        setPickedConnection({id: connection.id, name: connection.name});

        threadRuntime.append({
            content: [{text: `User picked: ${connection.name} (ID: ${connection.id})`, type: 'text'}],
            role: 'system',
        });
    };

    if (pickedConnection) {
        return (
            <div className="mt-2 flex items-center gap-2 text-sm">
                <CheckIcon className="size-4 text-emerald-600" />

                <span>
                    Picked: <span className="font-medium">{pickedConnection.name}</span>
                </span>
            </div>
        );
    }

    const isEmpty = (existingConnections?.length ?? 0) === 0;

    if (isEmpty) {
        return (
            <div className="mt-2 rounded-md border border-border bg-muted/30 p-3 text-sm text-muted-foreground">
                No existing {data.componentLabel} connection in this workspace. Ask the assistant to create one.
            </div>
        );
    }

    return (
        <div className={`mt-2 flex w-full min-w-0 items-center gap-2${supersededByLaterMessage ? 'opacity-60' : ''}`}>
            <Select disabled={supersededByLaterMessage} onValueChange={handleSelectChange}>
                <div className="min-w-0 flex-1">
                    <SelectTrigger>
                        <SelectValue placeholder={`Choose ${data.componentLabel} connection...`} />
                    </SelectTrigger>
                </div>

                <SelectContent>
                    {connections.map((connection) => (
                        <SelectItem key={connection.id} value={connection.id!.toString()}>
                            <div className="flex items-center space-x-1">
                                <span>{connection.name}</span>

                                {connection.tags && connection.tags.length > 0 && (
                                    <span className="text-xs text-gray-500">
                                        {connection.tags.map((tag) => tag.name).join(', ')}
                                    </span>
                                )}

                                {connection.environmentId != null && (
                                    <EnvironmentBadge environmentId={+connection.environmentId} />
                                )}
                            </div>
                        </SelectItem>
                    ))}
                </SelectContent>
            </Select>
        </div>
    );
};

export default SelectConnectionMessage;
