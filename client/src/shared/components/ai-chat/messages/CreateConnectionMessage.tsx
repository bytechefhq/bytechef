import Button from '@/components/Button/Button';
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
 * Renders the LLM's createConnection tool result as a single "Connect &lt;Component&gt;" button that opens
 * the ConnectionDialog. Companion to {@code SelectConnectionMessage} (the "pick an existing connection"
 * intent): splitting the two intents across separate tools keeps each UI unambiguous — this one only
 * creates, never offers a picker.
 */
const CreateConnectionMessage = ({data}: DataMessagePartProps<CreateConnectionDataI>) => {
    const [createdConnection, setCreatedConnection] = useState<{id: number; name: string} | undefined>();
    const [dialogOpen, setDialogOpen] = useState(false);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const connectionTagsQueryResult = useGetConnectionTagsQuery();

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

    const {data: targetComponentDefinition} = useGetComponentDefinitionQuery(
        {componentName: data.componentName, componentVersion: 1},
        Boolean(data.componentName)
    );

    const handleConnectionCreate = async (newConnectionId: number) => {
        await queryClient.invalidateQueries({queryKey: ConnectionKeys.connections});

        const fallbackName = data.suggestedName || `${data.componentLabel} connection`;

        setCreatedConnection({id: newConnectionId, name: fallbackName});
    };

    if (componentDefinitionsLoading) {
        return null;
    }

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
                    connectionTagsQueryKey={ConnectionKeys.connectionTags}
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
