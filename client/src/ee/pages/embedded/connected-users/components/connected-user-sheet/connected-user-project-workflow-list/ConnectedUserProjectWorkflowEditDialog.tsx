import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogBody,
    DialogCancelButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogMain,
} from '@/components/Dialog';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import ConnectedUserNewConnectionDialog from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserNewConnectionDialog';
import ProvisionProjectWorkflowConnectionField from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/provision-project-workflow-dialog/ProvisionProjectWorkflowConnectionField';
import {Connection} from '@/ee/shared/middleware/embedded/configuration';
import {useGetAllConnectedUserConnectionsQuery} from '@/ee/shared/queries/embedded/connections.queries';
import WorkflowConfigurationCard from '@/shared/components/workflow-configuration/WorkflowConfigurationCard';
import {
    useConnectedUserProjectWorkflowReferenceValuesQuery,
    useProvisionConnectedUserProjectWorkflowsMutation,
    useProvisionableConnectedUserProjectsQuery,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useMemo, useState} from 'react';

interface ConnectedUserProjectWorkflowEditDialogProps {
    configurationId: string;
    connectedUserId: number;
    connectedUserProjectWorkflowId: string;
    onClose: () => void;
    projectId: string;
    workflowLabel: string;
    workflowUuid: string;
}

const ConnectedUserProjectWorkflowEditDialog = ({
    configurationId,
    connectedUserId,
    connectedUserProjectWorkflowId,
    onClose,
    projectId,
    workflowLabel,
    workflowUuid,
}: ConnectedUserProjectWorkflowEditDialogProps) => {
    const [changedConnectionIds, setChangedConnectionIds] = useState<Record<string, string>>({});
    const [changedInputs, setChangedInputs] = useState<Record<string, string>>({});
    const [newConnectionComponentName, setNewConnectionComponentName] = useState<string>();

    const queryClient = useQueryClient();

    const {data: provisionableData, isLoading: provisionableLoading} = useProvisionableConnectedUserProjectsQuery({
        connectedUserId: connectedUserId.toString(),
    });

    const {data: referenceValuesData, isLoading: referenceValuesLoading} =
        useConnectedUserProjectWorkflowReferenceValuesQuery({id: connectedUserProjectWorkflowId});

    const {data: connectedUserConnections} = useGetAllConnectedUserConnectionsQuery(connectedUserId);

    const provisionConnectedUserProjectWorkflowsMutation = useProvisionConnectedUserProjectWorkflowsMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['connectedUserProjects']});
            void queryClient.invalidateQueries({queryKey: ['connectedUserProjectWorkflowReferenceValues']});
            void queryClient.invalidateQueries({queryKey: ['provisionableConnectedUserProjects']});

            onClose();
        },
    });

    const workflow = useMemo(
        () =>
            provisionableData?.provisionableConnectedUserProjects
                .find((project) => project.projectId === projectId)
                ?.configurations.find((configuration) => configuration.id === configurationId)
                ?.workflows.find((configurationWorkflow) => configurationWorkflow.workflowUuid === workflowUuid),
        [configurationId, projectId, provisionableData, workflowUuid]
    );

    const connectionsByComponentName = useMemo(() => {
        const connectionsByName = new Map<string, Connection[]>();

        (connectedUserConnections ?? []).forEach((connection) => {
            connectionsByName.set(connection.componentName, [
                ...(connectionsByName.get(connection.componentName) ?? []),
                connection,
            ]);
        });

        return connectionsByName;
    }, [connectedUserConnections]);

    const currentConnectionIds = useMemo(
        () =>
            Object.fromEntries(
                (referenceValuesData?.connectedUserProjectWorkflowReferenceValues.connections ?? []).map(
                    (connection) => [connection.componentName, connection.connectionId]
                )
            ) as Record<string, string>,
        [referenceValuesData]
    );

    const currentInputs = useMemo(
        () =>
            (referenceValuesData?.connectedUserProjectWorkflowReferenceValues.inputs || {}) as Record<string, unknown>,
        [referenceValuesData]
    );

    const components = workflow?.components || [];
    const inputs = workflow?.inputs || [];

    const loading = provisionableLoading || referenceValuesLoading;

    const getConnectionId = (componentName: string) =>
        changedConnectionIds[componentName] ||
        currentConnectionIds[componentName] ||
        connectionsByComponentName.get(componentName)?.[0]?.id?.toString();

    const getInputValue = (inputName: string) => {
        if (inputName in changedInputs) {
            return changedInputs[inputName];
        }

        const currentInput = currentInputs[inputName];

        return currentInput == null ? '' : String(currentInput);
    };

    const canSave =
        !!workflow &&
        !workflow.dangling &&
        !!referenceValuesData &&
        components.length + inputs.length > 0 &&
        inputs.every((input) => !input.required || !!getInputValue(input.name).trim());

    const handleSave = () => {
        if (!workflow) {
            return;
        }

        const connections = components
            .map((component) => ({componentName: component.name, connectionId: getConnectionId(component.name)}))
            .filter((connection): connection is {componentName: string; connectionId: string} =>
                Boolean(connection.connectionId)
            );

        provisionConnectedUserProjectWorkflowsMutation.mutate({
            configurationId,
            connectedUserId: connectedUserId.toString(),
            workflows: [
                {
                    ...(connections.length > 0 && {connections}),
                    ...(inputs.length > 0 && {
                        inputs: Object.fromEntries(inputs.map((input) => [input.name, getInputValue(input.name)])),
                    }),
                    workflowUuid,
                },
            ],
        });
    };

    return (
        <Dialog onOpenChange={(open) => !open && onClose()} open>
            <DialogContent className="gap-0 p-0" onInteractOutside={(event) => event.preventDefault()}>
                <DialogMain>
                    <DialogHeader
                        description="Set the connections and inputs this connected user's workflow runs with."
                        title={`Edit ${workflowLabel} Workflow`}
                    />

                    <DialogBody>
                        <div className="max-h-dialog-height overflow-y-auto">
                            {loading ? (
                                <p className="text-sm text-content-neutral-secondary">Loading...</p>
                            ) : !workflow ? (
                                <p className="text-sm text-content-neutral-secondary">
                                    This workflow cannot be edited because its configuration is not released to this
                                    user&apos;s environment.
                                </p>
                            ) : workflow.dangling ? (
                                <p className="text-sm text-content-warning-primary">
                                    This workflow needs attention before its connections and inputs can be edited. Its
                                    configuration no longer includes it.
                                </p>
                            ) : (
                                <WorkflowConfigurationCard
                                    connectionCount={components.length}
                                    connectionsContent={
                                        components.length === 0 ? (
                                            <p className="text-sm text-content-neutral-secondary">
                                                No connections to set. The configuration provides them or the workflow
                                                needs none.
                                            </p>
                                        ) : (
                                            <fieldset className="space-y-3 border-0 p-0">
                                                {components.map((component) => (
                                                    <ProvisionProjectWorkflowConnectionField
                                                        componentTitle={component.title || component.name}
                                                        connections={
                                                            connectionsByComponentName.get(component.name) ?? []
                                                        }
                                                        id={`edit-workflow-connection-${component.name}`}
                                                        key={component.name}
                                                        onChange={(connectionId) =>
                                                            setChangedConnectionIds((previousConnectionIds) => ({
                                                                ...previousConnectionIds,
                                                                [component.name]: connectionId,
                                                            }))
                                                        }
                                                        onNewConnection={() =>
                                                            setNewConnectionComponentName(component.name)
                                                        }
                                                        value={getConnectionId(component.name)}
                                                    />
                                                ))}
                                            </fieldset>
                                        )
                                    }
                                    enabled
                                    inputCount={inputs.length}
                                    inputsContent={
                                        inputs.length === 0 ? (
                                            <p className="text-sm text-content-neutral-secondary">
                                                No inputs to set. The configuration provides them or the workflow
                                                defines none.
                                            </p>
                                        ) : (
                                            <fieldset className="space-y-3 border-0 p-0">
                                                {inputs.map((input) => (
                                                    <fieldset className="space-y-1 border-0 p-0" key={input.name}>
                                                        <Label htmlFor={`edit-workflow-input-${input.name}`}>
                                                            {input.label || input.name}

                                                            {input.required && ' *'}
                                                        </Label>

                                                        <Input
                                                            id={`edit-workflow-input-${input.name}`}
                                                            onChange={(event) =>
                                                                setChangedInputs((previousInputs) => ({
                                                                    ...previousInputs,
                                                                    [input.name]: event.target.value,
                                                                }))
                                                            }
                                                            value={getInputValue(input.name)}
                                                        />
                                                    </fieldset>
                                                ))}
                                            </fieldset>
                                        )
                                    }
                                    onEnabledChange={() => undefined}
                                    showWorkflowToggle={false}
                                    switchId={`edit-workflow-${workflowUuid}`}
                                />
                            )}
                        </div>
                    </DialogBody>

                    <DialogFooter>
                        <DialogCancelButton />

                        <Button
                            disabled={!canSave || provisionConnectedUserProjectWorkflowsMutation.isPending}
                            label="Save"
                            onClick={handleSave}
                        />
                    </DialogFooter>
                </DialogMain>

                {newConnectionComponentName && (
                    <ConnectedUserNewConnectionDialog
                        componentName={newConnectionComponentName}
                        connectedUserId={connectedUserId}
                        onClose={() => setNewConnectionComponentName(undefined)}
                        onConnectionCreate={(connectionId) => {
                            setChangedConnectionIds((previousConnectionIds) => ({
                                ...previousConnectionIds,
                                [newConnectionComponentName]: connectionId.toString(),
                            }));

                            setNewConnectionComponentName(undefined);
                        }}
                    />
                )}
            </DialogContent>
        </Dialog>
    );
};

export default ConnectedUserProjectWorkflowEditDialog;
