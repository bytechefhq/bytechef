import UpgradeConnectedUsersDialogView, {
    UpgradeOptionI,
} from '@/ee/pages/embedded/connected-users/components/upgrade-connected-users-dialog/UpgradeConnectedUsersDialogView';
import useUpgradeSelection from '@/ee/pages/embedded/connected-users/components/upgrade-connected-users-dialog/useUpgradeSelection';
import {ConnectedUser} from '@/ee/shared/middleware/embedded/connected-user';
import {useUpgradeIntegrationInstanceConfigurationConnectedUsersMutation} from '@/ee/shared/mutations/embedded/integrationInstanceConfigurations.mutations';
import {
    useGetIntegrationInstanceConfigurationUpgradeImpactQuery,
    useGetIntegrationInstanceConfigurationsQuery,
} from '@/ee/shared/queries/embedded/integrationInstanceConfigurations.queries';
import {useGetIntegrationsQuery} from '@/ee/shared/queries/embedded/integrations.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useMemo} from 'react';

import type {MoveConnectedUsersResultI} from '@/ee/pages/embedded/connected-users/components/move-connected-users-dialog/MoveConnectedUsersDialog';

interface IntegrationUpgradeConnectedUsersDialogProps {
    connectedUsers: ConnectedUser[];
    onClose: () => void;
    onUpgraded: () => void;
}

const IntegrationUpgradeConnectedUsersDialog = ({
    connectedUsers,
    onClose,
    onUpgraded,
}: IntegrationUpgradeConnectedUsersDialogProps) => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {data: integrations} = useGetIntegrationsQuery({includeAllFields: false});

    const connectedUserIds = useMemo(() => connectedUsers.map((connectedUser) => connectedUser.id!), [connectedUsers]);

    const lowestVersionByIntegrationId = useMemo(() => {
        const lowestVersions = new Map<number, number>();

        for (const connectedUser of connectedUsers) {
            for (const integrationInstance of connectedUser.integrationInstances || []) {
                const {integrationId, integrationVersion} = integrationInstance;

                if (integrationId === undefined || integrationVersion === undefined) {
                    continue;
                }

                const lowestVersion = lowestVersions.get(integrationId);

                if (lowestVersion === undefined || integrationVersion < lowestVersion) {
                    lowestVersions.set(integrationId, integrationVersion);
                }
            }
        }

        return lowestVersions;
    }, [connectedUsers]);

    const parentOptions = useMemo<UpgradeOptionI[]>(() => {
        const componentNames = new Map<number, string>();

        for (const connectedUser of connectedUsers) {
            for (const integrationInstance of connectedUser.integrationInstances || []) {
                if (integrationInstance.integrationId !== undefined) {
                    componentNames.set(integrationInstance.integrationId, integrationInstance.componentName || '');
                }
            }
        }

        return Array.from(componentNames.keys())
            .map((integrationId) => {
                const integration = integrations?.find((candidate) => candidate.id === integrationId);

                return {
                    id: String(integrationId),
                    label: integration?.name || integration?.componentName || componentNames.get(integrationId) || '',
                };
            })
            .sort((first, second) => first.label.localeCompare(second.label));
    }, [connectedUsers, integrations]);

    const {
        parentId,
        selectParent,
        selectTarget,
        targetId: selectedTargetId,
    } = useUpgradeSelection(parentOptions.map((option) => option.id));

    const {data: configurations} = useGetIntegrationInstanceConfigurationsQuery(
        {environmentId: currentEnvironmentId, integrationId: parentId ? Number(parentId) : undefined},
        !!parentId
    );

    const targetOptions = useMemo<UpgradeOptionI[]>(() => {
        const lowestVersion = parentId ? lowestVersionByIntegrationId.get(Number(parentId)) : undefined;

        return (configurations || []).map((configuration) => ({
            badge:
                lowestVersion !== undefined && (configuration.integrationVersion || 0) > lowestVersion
                    ? 'Newer version'
                    : undefined,
            id: String(configuration.id),
            label: `${configuration.name} · V${configuration.integrationVersion}`,
        }));
    }, [configurations, lowestVersionByIntegrationId, parentId]);

    const targetId = targetOptions.some((option) => option.id === selectedTargetId) ? selectedTargetId : undefined;

    const {
        data: impact,
        isError: impactError,
        isLoading: impactLoading,
    } = useGetIntegrationInstanceConfigurationUpgradeImpactQuery(
        Number(targetId),
        {connectedUserIds},
        !!targetId && connectedUserIds.length > 0
    );

    const {mutateAsync: upgradeConnectedUsers} = useUpgradeIntegrationInstanceConfigurationConnectedUsersMutation();

    const handleUpgrade = (): Promise<MoveConnectedUsersResultI> =>
        upgradeConnectedUsers({
            configurationUpgradeRequest: {connectedUserIds},
            id: Number(targetId),
        });

    return (
        <UpgradeConnectedUsersDialogView
            connectedUsers={connectedUsers}
            emptyParentMessage="None of the selected users has an integration in this environment."
            impact={targetId ? impact : undefined}
            notOnSourceReason="Doesn't have this integration"
            onClose={onClose}
            onParentChange={selectParent}
            onTargetChange={selectTarget}
            onUpgrade={handleUpgrade}
            onUpgraded={onUpgraded}
            parentId={parentId}
            parentLabel="Integration"
            parentOptions={parentOptions}
            previewError={impactError}
            previewLoading={impactLoading}
            targetId={targetId}
            targetOptions={targetOptions}
            title="Upgrade Integration"
        />
    );
};

export default IntegrationUpgradeConnectedUsersDialog;
