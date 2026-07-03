import Button from '@/components/Button/Button';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import ConnectionDialog from '@/shared/components/connection/ConnectionDialog';
import {Connection} from '@/shared/middleware/automation/configuration';
import {useCreateConnectionMutation} from '@/shared/mutations/automation/connections.mutations';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {ConnectionKeys, useGetConnectionTagsQuery} from '@/shared/queries/automation/connections.queries';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {DataMessagePartProps} from '@assistant-ui/react';
import {useQueryClient} from '@tanstack/react-query';
import {CheckIcon} from 'lucide-react';
import {useState} from 'react';

export interface CreateConnectionDataI {
    componentLabel: string;
    componentName: string;
    kind: 'create-connection';
    suggestedName?: string;
}

/**
 * Renders the LLM's createConnection tool result as a single "Connect &lt;Component&gt;" button that
 * opens the ConnectionDialog. Companion to {@code SelectConnectionMessage}, which renders a dropdown
 * of EXISTING connections for the "pick" intent. Splitting the two intents under separate tools means
 * each UI is unambiguous: this component never offers a picker (use selectConnection for that), and the
 * select-connection message never offers a create affordance.
 *
 * <ul>
 * <li><b>Create a new connection</b> — what createConnection should mean. A single button is the cleanest
 * affordance: one click, dialog opens, user fills it in.</li>
 * <li><b>Pick an existing connection</b> — separate intent the LLM expresses via {@code selectConnection},
 * which renders {@code SelectConnectionMessage}'s dropdown.</li>
 * </ul>
 *
 * <p>
 * Keeping createConnection narrowly scoped to "create new" also means the button is unambiguous when the
 * workspace has many existing connections of the same component (e.g. multiple Slack workspaces) — there's
 * no risk of the user clicking the wrong dropdown item.
 * </p>
 */
const CreateConnectionMessage = ({data}: DataMessagePartProps<CreateConnectionDataI>) => {
    const [createdConnection, setCreatedConnection] = useState<{id: number; name: string} | undefined>();
    const [dialogOpen, setDialogOpen] = useState(false);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const connectionTagsQueryResult = useGetConnectionTagsQuery(currentWorkspaceId!);

    const queryClient = useQueryClient();

    const {
        data: componentDefinitions,
        error: componentDefinitionsError,
        isError: componentDefinitionsIsError,
        isLoading: componentDefinitionsLoading,
        refetch: refetchComponentDefinitions,
    } = useGetComponentDefinitionsQuery({
        connectionDefinitions: true,
    });

    // Fetch the full ComponentDefinition (not just the basic list shape) for the requested component
    // so the dialog opens directly to the configuration step pre-selected on this component, instead
    // of forcing the user back to the "pick a component" step. We've already established WHICH
    // component to connect via the LLM's `createConnection` tool call — making the user pick again
    // would be redundant and error-prone (long catalogs, name collisions across versions).
    const {data: targetComponentDefinition} = useGetComponentDefinitionQuery(
        // No componentVersion in the LLM's `createConnection` payload — connections are versioned
        // independently from components, and the dialog only needs the component definition to render
        // its connection-config step. Pass the latest known version (1) which the server's
        // GetComponentDefinitionRequest treats as "latest" via the connection-definition lookup.
        {componentName: data.componentName, componentVersion: 1},
        Boolean(data.componentName)
    );

    const handleConnectionCreate = async (newConnectionId: number) => {
        await queryClient.invalidateQueries({queryKey: ConnectionKeys.connections});

        // The dialog returns only the new id, not the new object. Use the LLM's suggested name (or fall
        // back to the component label) for the confirmation row — the user just typed the actual name
        // into the dialog, so showing it back as a bare id would read worse than the suggestion.
        const fallbackName = data.suggestedName || `${data.componentLabel} connection`;

        setCreatedConnection({id: newConnectionId, name: fallbackName});
    };

    if (componentDefinitionsLoading) {
        return null;
    }

    // Distinguish "still loading" from "load failed". Without this branch, a flaky/erroring component-definitions
    // endpoint silently disables the entire connect flow — the user sees an empty space where the
    // "Connect <Component>" button should be and has no idea the agent's tool call ever fired.
    if (componentDefinitionsIsError || !componentDefinitions) {
        return (
            <div className="mt-2 flex items-center gap-2">
                <span className="text-sm text-destructive">
                    Could not load component definitions
                    {componentDefinitionsError ? `: ${componentDefinitionsError.message}` : ''}.
                </span>

                <Button label="Retry" onClick={() => refetchComponentDefinitions()} variant="outline" />
            </div>
        );
    }

    if (createdConnection) {
        return (
            <div className="mt-2 flex items-center gap-2 text-sm">
                <CheckIcon className="size-4 text-emerald-600" />

                <span>
                    Connection ready: <span className="font-medium">{createdConnection.name}</span>
                </span>
            </div>
        );
    }

    return (
        <div className="mt-2 flex w-full min-w-0 items-center gap-2">
            <Button label={`Connect ${data.componentLabel}`} onClick={() => setDialogOpen(true)} variant="outline" />

            {dialogOpen && (
                <ConnectionDialog
                    componentDefinition={targetComponentDefinition}
                    componentDefinitions={componentDefinitions}
                    connection={{environmentId: currentEnvironmentId} as Connection}
                    connectionTagsQueryKey={ConnectionKeys.connectionTags(currentWorkspaceId!)}
                    connectionsQueryKey={ConnectionKeys.connections}
                    onClose={() => setDialogOpen(false)}
                    onConnectionCreate={handleConnectionCreate}
                    useCreateConnectionMutation={useCreateConnectionMutation}
                    useGetConnectionTagsQuery={() => connectionTagsQueryResult}
                />
            )}
        </div>
    );
};

export default CreateConnectionMessage;
