import Button from '@/components/Button/Button';
import {Dialog, DialogBody, DialogContent, DialogFooter, DialogHeader, DialogMain} from '@/components/Dialog';
import LoadingIcon from '@/components/LoadingIcon';
import {Label} from '@/components/ui/label';
import ProvisionIntegrationDialogConnectionStep, {
    isOAuth2AppMisconfigured,
    isOAuth2CodeAuthorizationType,
} from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/provision-integration-dialog/ProvisionIntegrationDialogConnectionStep';
import {ConnectedUserIntegrationInstance} from '@/ee/shared/middleware/embedded/connected-user';
import {useUpdateIntegrationInstanceCredentialsMutation} from '@/ee/shared/mutations/embedded/integrationInstances.mutations';
import {ConnectedUserKeys} from '@/ee/shared/queries/embedded/connectedUsers.queries';
import {ConnectionKeys} from '@/ee/shared/queries/embedded/connections.queries';
import {
    IntegrationInstanceKeys,
    useGetIntegrationInstanceConnectionQuery,
} from '@/ee/shared/queries/embedded/integrationInstances.queries';
import {useGetProvisionableIntegrationsQuery} from '@/ee/shared/queries/embedded/provisionableIntegrations.queries';
import EnvironmentBadge from '@/shared/components/EnvironmentBadge';
import ConnectionParameters from '@/shared/components/connection/ConnectionParameters';
import OAuth2Button from '@/shared/components/connection/OAuth2Button';
import {CodePayloadI} from '@/shared/components/connection/oauth2/useOAuth2';
import {useGetConnectionComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {useGetConnectionDefinitionQuery} from '@/shared/queries/platform/connectionDefinitions.queries';
import {useQueryClient} from '@tanstack/react-query';
import {KeyRoundIcon} from 'lucide-react';
import {useMemo, useState} from 'react';
import {Control, FieldValues, FormState, useForm} from 'react-hook-form';

interface CredentialsFormI {
    connectionParameters: Record<string, unknown>;
}

interface ConnectedUserIntegrationCredentialsDialogProps {
    connectedUserId: number;
    integrationInstance: ConnectedUserIntegrationInstance;
    onClose: () => void;
}

const ConnectedUserIntegrationCredentialsDialog = ({
    connectedUserId,
    integrationInstance,
    onClose,
}: ConnectedUserIntegrationCredentialsDialogProps) => {
    const [updating, setUpdating] = useState(false);

    const queryClient = useQueryClient();

    const {control, formState, handleSubmit, setValue, watch} = useForm<CredentialsFormI>({
        defaultValues: {connectionParameters: {}},
    });

    const {data: connection, isLoading: connectionLoading} = useGetIntegrationInstanceConnectionQuery(
        integrationInstance.id!
    );

    const {data: provisionableIntegrations} = useGetProvisionableIntegrationsQuery(connectedUserId);

    const {data: componentDefinition} = useGetConnectionComponentDefinitionQuery(
        {componentName: connection?.componentName || '', connectionVersion: connection?.connectionVersion as number},
        !!connection?.componentName && !!connection?.connectionVersion
    );

    const {data: connectionDefinition} = useGetConnectionDefinitionQuery(
        {componentName: connection?.componentName || '', componentVersion: componentDefinition?.version},
        !!componentDefinition
    );

    const updateIntegrationInstanceCredentialsMutation = useUpdateIntegrationInstanceCredentialsMutation({
        onError: () => {
            void queryClient.invalidateQueries({
                queryKey: IntegrationInstanceKeys.integrationInstanceConnection(integrationInstance.id!),
            });
        },
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ConnectedUserKeys.connectedUser(connectedUserId)});
            void queryClient.invalidateQueries({queryKey: ConnectionKeys.connections});
            void queryClient.invalidateQueries({
                queryKey: IntegrationInstanceKeys.integrationInstance(integrationInstance.id!),
            });

            onClose();
        },
    });

    const code = watch('connectionParameters.code');

    const provisionableIntegration = useMemo(
        () =>
            provisionableIntegrations?.find(
                (currentIntegration) => currentIntegration.integration?.id === integrationInstance.integrationId
            ),
        [integrationInstance.integrationId, provisionableIntegrations]
    );

    const provisionableConfiguration = useMemo(
        () =>
            provisionableIntegration?.integrationInstanceConfigurations?.find(
                (currentConfiguration) =>
                    currentConfiguration.integrationInstanceConfiguration?.id ===
                    integrationInstance.integrationInstanceConfigurationId
            ),
        [integrationInstance.integrationInstanceConfigurationId, provisionableIntegration]
    );

    const configurationNotProvisionable = !!provisionableIntegrations && !provisionableConfiguration;

    const codeRequired = isOAuth2CodeAuthorizationType(
        provisionableConfiguration?.integrationInstanceConfiguration?.authorizationType
    );

    const canSave =
        connection?.version != null &&
        !!provisionableConfiguration &&
        !isOAuth2AppMisconfigured(provisionableConfiguration) &&
        (!codeRequired || !!code) &&
        !updateIntegrationInstanceCredentialsMutation.isPending;

    const oAuth2AuthorizationParameters = provisionableConfiguration?.oAuth2AuthorizationParameters;

    const showOAuth2Button =
        updating && codeRequired && !code && !updateIntegrationInstanceCredentialsMutation.isPending;

    const handleCodeSuccess = ({code, code_verifier: codeVerifier}: CodePayloadI) => {
        setValue('connectionParameters.code', code);

        if (codeVerifier) {
            setValue('connectionParameters.code_verifier', codeVerifier);
        }

        if (!updateIntegrationInstanceCredentialsMutation.isPending) {
            void handleSubmit(handleSave)();
        }
    };

    const handleSave = ({connectionParameters}: CredentialsFormI) => {
        updateIntegrationInstanceCredentialsMutation.mutate({
            id: integrationInstance.id!,
            updateConnectionCredentialsRequest: {parameters: connectionParameters, version: connection!.version!},
        });
    };

    return (
        <Dialog onOpenChange={(open) => !open && onClose()} open>
            <DialogContent onInteractOutside={(event) => event.preventDefault()} size="sm">
                <DialogMain>
                    <DialogHeader title="Edit Connection" />

                    <DialogBody className="flex min-w-0 flex-col gap-4">
                        {connectionLoading && <LoadingIcon />}

                        {connection?.environmentId != null && (
                            <div className="space-x-2">
                                <Label>Environment</Label>

                                <EnvironmentBadge environmentId={connection.environmentId} />
                            </div>
                        )}

                        {configurationNotProvisionable && (
                            <p className="text-sm text-content-neutral-secondary">
                                This integration&apos;s configuration can no longer be provisioned, so its credentials
                                cannot be replaced.
                            </p>
                        )}

                        {updating && provisionableIntegration && provisionableConfiguration && (
                            <ProvisionIntegrationDialogConnectionStep
                                componentName={provisionableIntegration.integration!.componentName}
                                componentVersion={provisionableIntegration.componentVersion}
                                configuration={provisionableConfiguration}
                                control={control as unknown as Control<FieldValues>}
                                formState={formState as unknown as FormState<FieldValues>}
                                showLabel={false}
                            />
                        )}

                        {connection && connectionDefinition && (
                            <div className="min-w-0 pt-4">
                                <ConnectionParameters
                                    authorizationParameters={connection.authorizationParameters}
                                    authorizationType={connection.authorizationType}
                                    baseUri={connection.baseUri}
                                    connectionDefinition={connectionDefinition}
                                    connectionParameters={connection.connectionParameters}
                                />
                            </div>
                        )}
                    </DialogBody>

                    <DialogFooter>
                        <div className="flex flex-wrap items-center justify-end gap-2">
                            <Button label="Cancel" onClick={onClose} type="button" variant="outline" />

                            {!updating && provisionableConfiguration && connection && (
                                <Button onClick={() => setUpdating(true)} type="button" variant="outline">
                                    <KeyRoundIcon /> Update credentials
                                </Button>
                            )}

                            {updating && (
                                <Button
                                    label="Back"
                                    onClick={() => setUpdating(false)}
                                    type="button"
                                    variant="outline"
                                />
                            )}

                            {showOAuth2Button &&
                                provisionableIntegration &&
                                connection?.version != null &&
                                oAuth2AuthorizationParameters?.authorizationUrl &&
                                oAuth2AuthorizationParameters.clientId && (
                                    <OAuth2Button
                                        authorizationUrl={oAuth2AuthorizationParameters.authorizationUrl}
                                        clientId={oAuth2AuthorizationParameters.clientId}
                                        componentName={provisionableIntegration.integration!.componentName}
                                        extraQueryParameters={oAuth2AuthorizationParameters.extraQueryParameters}
                                        onClick={(getAuth: () => void) => getAuth()}
                                        onCodeSuccess={handleCodeSuccess}
                                        redirectUri={provisionableConfiguration?.redirectUri || ''}
                                        responseType="code"
                                        scopes={oAuth2AuthorizationParameters.scopes}
                                    />
                                )}

                            {updating && !showOAuth2Button && (
                                <Button
                                    disabled={!canSave}
                                    label="Update credentials"
                                    onClick={handleSubmit(handleSave)}
                                    type="submit"
                                />
                            )}
                        </div>
                    </DialogFooter>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default ConnectedUserIntegrationCredentialsDialog;
