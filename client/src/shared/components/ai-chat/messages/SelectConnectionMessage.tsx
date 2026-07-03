import {
    Select,
    SelectContent,
    SelectGroup,
    SelectItem,
    SelectLabel,
    SelectTrigger,
    SelectValue,
} from '@/components/Select/Select';
import ConnectionScopeBadge from '@/pages/automation/connections/components/ConnectionScopeBadge';
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

type ConnectionVisibilityType = 'ORGANIZATION' | 'PRIVATE' | 'WORKSPACE';

const VISIBILITY_ORDER: Array<ConnectionVisibilityType> = ['PRIVATE', 'WORKSPACE', 'ORGANIZATION'];

const VISIBILITY_LABELS: Record<ConnectionVisibilityType, string> = {
    ORGANIZATION: 'Organization',
    PRIVATE: 'Private',
    WORKSPACE: 'Workspace',
};

/**
 * Renders the LLM's selectConnection tool result as a dropdown listing the workspace's existing connections
 * for a given component. Companion to {@code CreateConnectionMessage}, which renders a "Connect &lt;X&gt;"
 * button for the "create new connection" intent. Splitting the two intents under separate tools means each
 * UI is unambiguous: this component never offers a create affordance (use createConnection for that), and
 * the create-connection never offers a select dropdown.
 *
 * <p>
 * On pick, the user's choice is dispatched as a system message via the assistant-ui thread runtime so the
 * transcript reads {@code "User picked: <connection name>"} — same convention used by AskUserQuestion's
 * answer flow. The agent's next turn reads the pick naturally from chat memory.
 * </p>
 *
 * <p>
 * The dropdown dims once a follow-up message lands on the thread (the user picked, or the agent moved on)
 * so the user sees past-tense at a glance. Same pattern as AskUserQuestion.
 * </p>
 */
const SelectConnectionMessage = ({data}: DataMessagePartProps<SelectConnectionDataI>) => {
    const [pickedConnection, setPickedConnection] = useState<{id: number; name: string} | undefined>();
    const [supersededByLaterMessage, setSupersededByLaterMessage] = useState(false);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const threadRuntime = useThreadRuntime();

    // Fetch the connection definition's version separately from the component version — the two diverge
    // across releases (CLAUDE.md "Workflow editor connection dropdown" gotcha). Filtering existing
    // connections by component version would silently miss connections the user could actually pick.
    const {data: connectionDefinition} = useGetConnectionDefinitionQuery(
        {componentName: data.componentName, componentVersion: 1},
        Boolean(data.componentName)
    );

    // Gated on connectionDefinition?.version AND currentWorkspaceId so we don't fire with
    // connectionVersion=undefined and get the unfiltered list — that would briefly flash an unrelated
    // dropdown before the filtered fetch lands.
    const {data: existingConnections} = useGetWorkspaceConnectionsQuery(
        {
            componentName: data.componentName,
            connectionVersion: connectionDefinition?.version,
            id: currentWorkspaceId!,
        },
        Boolean(connectionDefinition?.version) && currentWorkspaceId != null
    );

    const groupedConnections = useMemo(() => {
        const connections = existingConnections ?? [];

        return VISIBILITY_ORDER.map((visibility) => ({
            connections: connections.filter((connection) => (connection.visibility || 'PRIVATE') === visibility),
            label: VISIBILITY_LABELS[visibility],
            visibility,
        })).filter((group) => group.connections.length > 0);
    }, [existingConnections]);

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

        // Append as a system message so the transcript reads "User picked: <name>" rather than as if the
        // user typed the connection name literally — same convention AskUserQuestion uses for its answers.
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
        // The LLM called selectConnection but the workspace has no existing connections of this component.
        // Surface a clear message rather than rendering an empty dropdown — the LLM should have called
        // createConnection instead. Surfacing the mismatch in the chat helps the user (and the LLM on its
        // next turn) recover by switching to the create path.
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
                    {groupedConnections.map((group) => (
                        <SelectGroup key={group.visibility}>
                            <SelectLabel className="text-xs font-semibold text-muted-foreground uppercase">
                                {group.label}
                            </SelectLabel>

                            {group.connections.map((connection) => (
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

                                        {connection.visibility && (
                                            <ConnectionScopeBadge visibility={connection.visibility} />
                                        )}
                                    </div>
                                </SelectItem>
                            ))}
                        </SelectGroup>
                    ))}
                </SelectContent>
            </Select>
        </div>
    );
};

export default SelectConnectionMessage;
