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
import {
    IntegrationInstance,
    IntegrationInstanceConfiguration,
    IntegrationInstanceConfigurationWorkflow,
    IntegrationInstanceWorkflow,
    Workflow,
} from '@/ee/shared/middleware/embedded/configuration';
import {useUpdateIntegrationInstanceWorkflowMutation} from '@/ee/shared/mutations/embedded/integrationInstanceWorkflows.mutations';
import {useGetComponentDefinitionsQuery} from '@/ee/shared/queries/embedded/componentDefinitions.queries';
import {ConnectedUserKeys} from '@/ee/shared/queries/embedded/connectedUsers.queries';
import {
    useGetAllConnectedUserConnectionsQuery,
    useGetConnectionsQuery,
} from '@/ee/shared/queries/embedded/connections.queries';
import {IntegrationInstanceKeys} from '@/ee/shared/queries/embedded/integrationInstances.queries';
import {WorkflowMockProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import InputConfigurationList from '@/shared/components/InputConfigurationList';
import WorkflowConfigurationCard from '@/shared/components/workflow-configuration/WorkflowConfigurationCard';
import {WorkflowInput} from '@/shared/middleware/platform/configuration';
import {useQueryClient} from '@tanstack/react-query';
import {useMemo} from 'react';
import {Control, FieldValues, FormState, useForm} from 'react-hook-form';

interface WorkflowInputsFormI {
    inputs: Record<string, unknown>;
}

interface WorkflowConnectionRowI {
    connectionName?: string;
    description: string;
    id: string;
    title: string;
}

interface ConnectedUserIntegrationWorkflowEditDialogProps {
    integrationInstance: IntegrationInstance;
    integrationInstanceConfiguration: IntegrationInstanceConfiguration;
    integrationInstanceConfigurationWorkflow: IntegrationInstanceConfigurationWorkflow;
    integrationInstanceWorkflow?: IntegrationInstanceWorkflow;
    onClose: () => void;
    workflow: Workflow;
}

const ConnectedUserIntegrationWorkflowEditDialog = ({
    integrationInstance,
    integrationInstanceConfiguration,
    integrationInstanceConfigurationWorkflow,
    integrationInstanceWorkflow,
    onClose,
    workflow,
}: ConnectedUserIntegrationWorkflowEditDialogProps) => {
    const queryClient = useQueryClient();

    const inputs = (workflow.inputs ?? []).filter((input) => !input.internalOnly) as unknown as WorkflowInput[];

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({connectionDefinitions: true});

    const {data: connectedUserConnections} = useGetAllConnectedUserConnectionsQuery(
        integrationInstance.connectedUserId!,
        integrationInstance.connectedUserId !== undefined
    );

    const configurationConnections = useMemo(
        () => integrationInstanceConfigurationWorkflow.connections ?? [],
        [integrationInstanceConfigurationWorkflow.connections]
    );

    const {data: environmentConnections} = useGetConnectionsQuery(
        {environmentId: integrationInstanceConfiguration.environmentId},
        configurationConnections.length > 0
    );

    const connectionRows = useMemo(() => {
        const getComponentTitle = (componentName?: string) =>
            componentDefinitions?.find((componentDefinition) => componentDefinition.name === componentName)?.title ||
            componentName ||
            'Connection';

        const rows: WorkflowConnectionRowI[] = [];

        if (integrationInstance.connectionId !== undefined) {
            const userConnection = connectedUserConnections?.find(
                (connection) => connection.id === integrationInstance.connectionId
            );

            rows.push({
                connectionName: userConnection?.name,
                description: "The connected user's own account. Manage it on the Connections tab.",
                id: `user-${integrationInstance.connectionId}`,
                title: getComponentTitle(userConnection?.componentName),
            });
        }

        for (const configurationConnection of configurationConnections) {
            const internalConnection = environmentConnections?.find(
                (connection) => connection.id === configurationConnection.connectionId
            );

            rows.push({
                connectionName: internalConnection?.name,
                description: `Internal connection for ${configurationConnection.workflowNodeName}, set by an admin on the integration configuration.`,
                id: `internal-${configurationConnection.workflowNodeName}-${configurationConnection.workflowConnectionKey}`,
                title: getComponentTitle(internalConnection?.componentName),
            });
        }

        return rows;
    }, [
        componentDefinitions,
        configurationConnections,
        connectedUserConnections,
        environmentConnections,
        integrationInstance.connectionId,
    ]);

    const {control, formState, handleSubmit} = useForm<WorkflowInputsFormI>({
        defaultValues: {inputs: {...(integrationInstanceWorkflow?.inputs ?? {})}},
    });

    const updateIntegrationInstanceWorkflowMutation = useUpdateIntegrationInstanceWorkflowMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({
                queryKey: ConnectedUserKeys.connectedUser(integrationInstance.connectedUserId!),
            });
            void queryClient.invalidateQueries({
                queryKey: IntegrationInstanceKeys.integrationInstance(integrationInstance.id!),
            });

            onClose();
        },
    });

    const handleSave = ({inputs: formInputs}: WorkflowInputsFormI) => {
        updateIntegrationInstanceWorkflowMutation.mutate({
            id: integrationInstance.id!,
            updateIntegrationInstanceWorkflowRequest: {
                inputs: {...(integrationInstanceWorkflow?.inputs ?? {}), ...formInputs},
            },
            workflowId: workflow.id!,
        });
    };

    return (
        <Dialog onOpenChange={(open) => !open && onClose()} open>
            <DialogContent className="gap-0 p-0" onInteractOutside={(event) => event.preventDefault()}>
                <DialogMain>
                    <DialogHeader
                        description="See the connections and set the inputs this connected user's workflow runs with."
                        title={`Edit ${workflow.label} Workflow`}
                    />

                    <DialogBody>
                        <div className="max-h-dialog-height overflow-y-auto">
                            <WorkflowConfigurationCard
                                connectionCount={connectionRows.length}
                                connectionsContent={
                                    connectionRows.length === 0 ? (
                                        <p className="text-sm text-content-neutral-secondary">
                                            This workflow uses no connections.
                                        </p>
                                    ) : (
                                        <fieldset className="space-y-3 border-0 p-0">
                                            {connectionRows.map((connectionRow) => (
                                                <fieldset className="space-y-1 border-0 p-0" key={connectionRow.id}>
                                                    <Label htmlFor={`edit-workflow-connection-${connectionRow.id}`}>
                                                        {connectionRow.title}
                                                    </Label>

                                                    <Input
                                                        id={`edit-workflow-connection-${connectionRow.id}`}
                                                        readOnly
                                                        value={connectionRow.connectionName ?? ''}
                                                    />

                                                    <p className="text-xs text-content-neutral-secondary">
                                                        {connectionRow.description}
                                                    </p>
                                                </fieldset>
                                            ))}
                                        </fieldset>
                                    )
                                }
                                enabled
                                inputCount={inputs.length}
                                inputsContent={
                                    inputs.length > 0 ? (
                                        <WorkflowMockProvider>
                                            <InputConfigurationList
                                                control={control as unknown as Control<FieldValues>}
                                                controlPath="inputs"
                                                formState={formState as unknown as FormState<FieldValues>}
                                                inputs={inputs}
                                                workflowId={workflow.id}
                                            />
                                        </WorkflowMockProvider>
                                    ) : (
                                        <p className="text-sm text-content-neutral-secondary">
                                            No inputs to set. Internal-only inputs are set on the integration
                                            configuration.
                                        </p>
                                    )
                                }
                                onEnabledChange={() => undefined}
                                showWorkflowToggle={false}
                                switchId={`edit-workflow-${workflow.id}`}
                            />
                        </div>
                    </DialogBody>

                    <DialogFooter>
                        <DialogCancelButton />

                        <Button
                            disabled={inputs.length === 0 || updateIntegrationInstanceWorkflowMutation.isPending}
                            label="Save"
                            onClick={() => void handleSubmit(handleSave)()}
                            type="button"
                        />
                    </DialogFooter>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default ConnectedUserIntegrationWorkflowEditDialog;
