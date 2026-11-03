import MoveConnectedUsersDialog, {
    MoveConnectedUsersResultI,
} from '@/ee/pages/embedded/connected-users/components/move-connected-users-dialog/MoveConnectedUsersDialog';
import {
    ConfigurationConnectedUser,
    IntegrationInstanceConfiguration,
} from '@/ee/shared/middleware/embedded/configuration';
import {useMoveIntegrationInstanceConfigurationConnectedUsersMutation} from '@/ee/shared/mutations/embedded/integrationInstanceConfigurations.mutations';
import {ConnectedUserKeys} from '@/ee/shared/queries/embedded/connectedUsers.queries';
import {
    IntegrationInstanceConfigurationKeys,
    useGetIntegrationInstanceConfigurationConnectedUsersQuery,
    useGetIntegrationInstanceConfigurationMoveImpactQuery,
} from '@/ee/shared/queries/embedded/integrationInstanceConfigurations.queries';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback, useMemo, useState} from 'react';

type ConfigurationReferenceType = Pick<IntegrationInstanceConfiguration, 'id' | 'integrationVersion' | 'name'>;

type TargetConfigurationType = ConfigurationReferenceType & Pick<IntegrationInstanceConfiguration, 'enabled'>;

interface IntegrationInstanceConfigurationMoveUsersDialogProps {
    configuration: ConfigurationReferenceType;
    connectedUsers?: ConfigurationConnectedUser[];
    initialSelectedConnectedUserIds?: number[];
    onClose: () => void;
    targetConfigurations: TargetConfigurationType[];
    title?: string;
}

const formatConfigurationLabel = (configuration: ConfigurationReferenceType): string =>
    `${configuration.name} · v${configuration.integrationVersion}`;

const formatTargetLabel = (configuration: TargetConfigurationType): string =>
    `${formatConfigurationLabel(configuration)}${configuration.enabled === false ? ' (disabled)' : ''}`;

const IntegrationInstanceConfigurationMoveUsersDialog = ({
    configuration,
    connectedUsers: presetConnectedUsers,
    initialSelectedConnectedUserIds,
    onClose,
    targetConfigurations,
    title = 'Move Users',
}: IntegrationInstanceConfigurationMoveUsersDialogProps) => {
    const [selection, setSelection] = useState<{connectedUserIds?: string[]; targetId?: string}>({});

    const queryClient = useQueryClient();

    const {data: loadedConnectedUsers, isError: connectedUsersError} =
        useGetIntegrationInstanceConfigurationConnectedUsersQuery(configuration.id!, !presetConnectedUsers);

    const connectedUsers = presetConnectedUsers || loadedConnectedUsers;

    const {
        data: moveImpact,
        isError: moveImpactError,
        isLoading: moveImpactLoading,
    } = useGetIntegrationInstanceConfigurationMoveImpactQuery(
        configuration.id!,
        {
            connectedUserIds: (selection.connectedUserIds ?? []).map(Number),
            targetId: Number(selection.targetId),
        },
        !!selection.targetId && !!selection.connectedUserIds?.length
    );

    const moveMutation = useMoveIntegrationInstanceConfigurationConnectedUsersMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({
                queryKey: IntegrationInstanceConfigurationKeys.integrationInstanceConfigurations,
            });
            void queryClient.invalidateQueries({queryKey: ConnectedUserKeys.connectedUsers});
        },
    });

    const dialogConnectedUsers = useMemo(
        () =>
            connectedUsers?.map((connectedUser) => ({
                ...connectedUser,
                connectedUserId: String(connectedUser.connectedUserId),
            })),
        [connectedUsers]
    );

    const targets = useMemo(
        () =>
            targetConfigurations.map((targetConfiguration) => ({
                id: String(targetConfiguration.id),
                label: formatTargetLabel(targetConfiguration),
            })),
        [targetConfigurations]
    );

    const {mutateAsync: moveConnectedUsers} = moveMutation;

    const handleMove = useCallback(
        (targetId: string, connectedUserIds: string[]): Promise<MoveConnectedUsersResultI> =>
            moveConnectedUsers({
                configurationMoveRequest: {
                    connectedUserIds: connectedUserIds.map(Number),
                    targetId: Number(targetId),
                },
                id: configuration.id!,
            }),
        [configuration.id, moveConnectedUsers]
    );

    return (
        <MoveConnectedUsersDialog
            connectedUsers={dialogConnectedUsers}
            connectedUsersError={connectedUsersError}
            impact={moveImpact?.workflows}
            initialSelectedConnectedUserIds={initialSelectedConnectedUserIds?.map(String)}
            mcpServerCount={moveImpact?.mcpServerCount}
            onClose={onClose}
            onMove={handleMove}
            onSelectionChange={(targetId, connectedUserIds) => setSelection({connectedUserIds, targetId})}
            previewError={moveImpactError}
            previewLoading={moveImpactLoading}
            previewSkipped={moveImpact?.skipped}
            sourceLabel={formatConfigurationLabel(configuration)}
            targets={targets}
            title={title}
        />
    );
};

export default IntegrationInstanceConfigurationMoveUsersDialog;
