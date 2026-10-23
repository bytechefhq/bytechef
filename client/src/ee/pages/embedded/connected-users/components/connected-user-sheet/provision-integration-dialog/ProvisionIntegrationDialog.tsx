import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {Label} from '@/components/ui/label';
import ProvisionIntegrationDialogConfigurationSelect from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/provision-integration-dialog/ProvisionIntegrationDialogConfigurationSelect';
import ProvisionIntegrationDialogConnectionStep, {
    isOAuth2AppMisconfigured,
    isOAuth2CodeAuthorizationType,
} from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/provision-integration-dialog/ProvisionIntegrationDialogConnectionStep';
import ProvisionIntegrationDialogWorkflowItem from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/provision-integration-dialog/ProvisionIntegrationDialogWorkflowItem';
import {useProvisionIntegrationInstanceMutation} from '@/ee/shared/mutations/embedded/integrationInstances.mutations';
import {ConnectedUserKeys} from '@/ee/shared/queries/embedded/connectedUsers.queries';
import {useGetIntegrationVersionWorkflowsQuery} from '@/ee/shared/queries/embedded/integrationWorkflows.queries';
import {useGetProvisionableIntegrationsQuery} from '@/ee/shared/queries/embedded/provisionableIntegrations.queries';
import {CodePayloadI} from '@/shared/components/connection/oauth2/useOAuth2';
import {useQueryClient} from '@tanstack/react-query';
import {useMemo} from 'react';
import {Control, FieldValues, FormState, useForm} from 'react-hook-form';

interface ProvisionIntegrationFormI {
    connectionParameters: Record<string, unknown>;
    integrationId?: number;
    integrationInstanceConfigurationId?: number;
    workflows: Array<{enabled: boolean; inputs: Record<string, unknown>; workflowUuid: string}>;
}

interface ProvisionIntegrationDialogProps {
    connectedUserId: number;
    onClose: () => void;
}

const ProvisionIntegrationDialog = ({connectedUserId, onClose}: ProvisionIntegrationDialogProps) => {
    const queryClient = useQueryClient();

    const {control, formState, handleSubmit, setValue, watch} = useForm<ProvisionIntegrationFormI>({
        defaultValues: {connectionParameters: {}, workflows: []},
    });

    const {data: provisionableIntegrations} = useGetProvisionableIntegrationsQuery(connectedUserId);

    const integrationId = watch('integrationId');
    const integrationInstanceConfigurationId = watch('integrationInstanceConfigurationId');
    const code = watch('connectionParameters.code');

    const provisionableIntegration = useMemo(
        () => provisionableIntegrations?.find((current) => current.integration?.id === integrationId),
        [provisionableIntegrations, integrationId]
    );

    const provisionableConfiguration = useMemo(
        () =>
            provisionableIntegration?.integrationInstanceConfigurations?.find(
                (current) => current.integrationInstanceConfiguration?.id === integrationInstanceConfigurationId
            ),
        [provisionableIntegration, integrationInstanceConfigurationId]
    );

    const configuration = provisionableConfiguration?.integrationInstanceConfiguration;

    const {data: integrationWorkflows} = useGetIntegrationVersionWorkflowsQuery(
        configuration?.integrationId ?? 0,
        configuration?.integrationVersion ?? 0,
        true,
        !!configuration
    );

    const provisionIntegrationInstanceMutation = useProvisionIntegrationInstanceMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ConnectedUserKeys.connectedUser(connectedUserId)});

            onClose();
        },
    });

    const codeRequired = isOAuth2CodeAuthorizationType(configuration?.authorizationType);

    const canProvision =
        !!configuration && !isOAuth2AppMisconfigured(provisionableConfiguration) && (!codeRequired || !!code);

    const noProvisionableIntegrations = provisionableIntegrations?.length === 0;

    const handleIntegrationChange = (value: string) => {
        setValue('integrationId', value ? Number(value) : undefined);
        setValue('integrationInstanceConfigurationId', undefined);
        setValue('connectionParameters', {});
        setValue('workflows', []);
    };

    const handleConfigurationChange = (value: number) => {
        const selectedConfiguration = provisionableIntegration?.integrationInstanceConfigurations?.find(
            (current) => current.integrationInstanceConfiguration?.id === value
        )?.integrationInstanceConfiguration;

        setValue('integrationInstanceConfigurationId', value);
        setValue('connectionParameters', {});
        setValue(
            'workflows',
            (selectedConfiguration?.integrationInstanceConfigurationWorkflows ?? []).map((workflow) => ({
                enabled: false,
                inputs: {},
                workflowUuid: workflow.workflowUuid!,
            }))
        );
    };

    const handleCodeSuccess = ({code, code_verifier: codeVerifier}: CodePayloadI) => {
        setValue('connectionParameters.code', code);

        if (codeVerifier) {
            setValue('connectionParameters.code_verifier', codeVerifier);
        }
    };

    const handleProvision = ({connectionParameters, workflows}: ProvisionIntegrationFormI) => {
        provisionIntegrationInstanceMutation.mutate({
            connectedUserId,
            provisionIntegrationInstanceRequest: {
                connectionParameters,
                integrationInstanceConfigurationId: integrationInstanceConfigurationId!,
                workflows: workflows
                    .filter((workflow) => workflow.enabled)
                    .map(({inputs, workflowUuid}) => ({inputs, workflowUuid})),
            },
        });
    };

    return (
        <Dialog onOpenChange={(open) => !open && onClose()} open>
            <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-xl" showCloseButton>
                <DialogHeader>
                    <DialogTitle>Provision integration</DialogTitle>

                    <DialogDescription>Add an integration instance for this connected user.</DialogDescription>
                </DialogHeader>

                <div className="flex flex-col gap-4">
                    {noProvisionableIntegrations ? (
                        <p className="text-sm text-content-neutral-secondary">
                            No integrations can be provisioned for this connected user.
                        </p>
                    ) : (
                        <fieldset className="space-y-2 border-0 p-0">
                            <Label htmlFor="provision-integration-id">Integration</Label>

                            <select
                                className="flex h-9 w-full rounded-md border border-stroke-neutral-secondary bg-surface-neutral-primary px-3 py-1 text-sm"
                                id="provision-integration-id"
                                onChange={(event) => handleIntegrationChange(event.target.value)}
                                value={integrationId ?? ''}
                            >
                                <option value="">Select...</option>

                                {provisionableIntegrations?.map((current) => (
                                    <option key={current.integration?.id} value={current.integration?.id}>
                                        {current.integration?.name}
                                    </option>
                                ))}
                            </select>
                        </fieldset>
                    )}

                    {provisionableIntegration && (
                        <ProvisionIntegrationDialogConfigurationSelect
                            configurations={provisionableIntegration.integrationInstanceConfigurations ?? []}
                            onChange={handleConfigurationChange}
                            value={integrationInstanceConfigurationId}
                        />
                    )}

                    {provisionableIntegration && provisionableConfiguration && (
                        <ProvisionIntegrationDialogConnectionStep
                            componentName={provisionableIntegration.integration!.componentName}
                            componentVersion={provisionableIntegration.componentVersion}
                            configuration={provisionableConfiguration}
                            control={control as unknown as Control<FieldValues>}
                            formState={formState as unknown as FormState<FieldValues>}
                            key={integrationInstanceConfigurationId}
                            onCodeSuccess={handleCodeSuccess}
                        />
                    )}

                    {configuration && !configuration.integrationInstanceConfigurationWorkflows?.length && (
                        <p className="text-sm text-content-neutral-secondary">
                            This configuration has no enabled workflows.
                        </p>
                    )}

                    {configuration?.integrationInstanceConfigurationWorkflows?.map(
                        (configurationWorkflow, workflowIndex) => {
                            const workflow = integrationWorkflows?.find(
                                (current) =>
                                    current.workflowUuid === configurationWorkflow.workflowUuid ||
                                    current.id === configurationWorkflow.workflowId
                            );

                            return (
                                workflow && (
                                    <ProvisionIntegrationDialogWorkflowItem
                                        control={control as unknown as Control<FieldValues>}
                                        formState={formState as unknown as FormState<FieldValues>}
                                        key={configurationWorkflow.workflowUuid}
                                        onEnabledChange={(enabled) =>
                                            setValue(`workflows.${workflowIndex}.enabled`, enabled)
                                        }
                                        workflow={workflow}
                                        workflowIndex={workflowIndex}
                                    />
                                )
                            );
                        }
                    )}
                </div>

                <DialogFooter>
                    <Button label="Cancel" onClick={onClose} variant="outline" />

                    <Button
                        disabled={!canProvision || provisionIntegrationInstanceMutation.isPending}
                        label="Provision"
                        onClick={handleSubmit(handleProvision)}
                    />
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default ProvisionIntegrationDialog;
