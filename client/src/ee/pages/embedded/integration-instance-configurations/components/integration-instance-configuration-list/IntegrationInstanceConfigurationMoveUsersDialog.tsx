import {Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle} from '@/components/ui/dialog';
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

interface SkippedConnectedUserI {
    connectedUserId: number;
    reason: string;
    workflowLabels: string[];
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
            (connectedUsers ?? []).map((connectedUser) => ({
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

    const labelByConnectedUserId = useMemo(
        () =>
            new Map(
                dialogConnectedUsers.map((connectedUser) => [
                    connectedUser.connectedUserId,
                    connectedUser.name || connectedUser.email || connectedUser.externalId,
                ])
            ),
        [dialogConnectedUsers]
    );

    const toSkippedLines = useCallback(
        (skippedUsers: SkippedConnectedUserI[]) =>
            skippedUsers.map((skippedUser) => ({
                label:
                    labelByConnectedUserId.get(String(skippedUser.connectedUserId)) ||
                    String(skippedUser.connectedUserId),
                reason: skippedUser.reason,
                workflowLabels: skippedUser.workflowLabels,
            })),
        [labelByConnectedUserId]
    );

    const previewSkipped = useMemo(() => toSkippedLines(moveImpact?.skipped ?? []), [moveImpact, toSkippedLines]);

    const {mutateAsync: moveConnectedUsers} = moveMutation;

    const handleMove = useCallback(
        async (targetId: string, connectedUserIds: string[]): Promise<MoveConnectedUsersResultI> => {
            const moveResult = await moveConnectedUsers({
                configurationMoveRequest: {
                    connectedUserIds: connectedUserIds.map(Number),
                    targetId: Number(targetId),
                },
                id: configuration.id!,
            });

            return {
                movedCount: moveResult.movedConnectedUserIds.length,
                needsAttentionCount: moveResult.needsAttentionConnectedUserIds.length,
                skipped: toSkippedLines(moveResult.skipped),
            };
        },
        [configuration.id, moveConnectedUsers, toSkippedLines]
    );

    if (!connectedUsers) {
        return (
            <Dialog onOpenChange={(open) => !open && onClose()} open>
                <DialogContent showCloseButton>
                    <DialogHeader>
                        <DialogTitle>{title}</DialogTitle>

                        <DialogDescription>
                            {connectedUsersError ? 'Could not load the connected users.' : 'Loading users...'}
                        </DialogDescription>
                    </DialogHeader>
                </DialogContent>
            </Dialog>
        );
    }

    return (
        <MoveConnectedUsersDialog
            connectedUsers={dialogConnectedUsers}
            impact={moveImpact?.workflows}
            initialSelectedConnectedUserIds={initialSelectedConnectedUserIds?.map(String)}
            mcpServerCount={moveImpact?.mcpServerCount}
            onClose={onClose}
            onMove={handleMove}
            onSelectionChange={(targetId, connectedUserIds) => setSelection({connectedUserIds, targetId})}
            previewError={moveImpactError}
            previewLoading={moveImpactLoading}
            previewSkipped={previewSkipped}
            sourceLabel={formatConfigurationLabel(configuration)}
            targets={targets}
            title={title}
        />
    );
};

export default IntegrationInstanceConfigurationMoveUsersDialog;
