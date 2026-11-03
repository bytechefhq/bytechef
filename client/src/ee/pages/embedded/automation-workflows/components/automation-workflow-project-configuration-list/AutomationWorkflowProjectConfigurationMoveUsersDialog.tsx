import {Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import MoveConnectedUsersDialog, {
    MoveConnectedUsersResultI,
} from '@/ee/pages/embedded/connected-users/components/move-connected-users-dialog/MoveConnectedUsersDialog';
import {
    AutomationWorkflowProjectConfigurationsQuery,
    useAutomationWorkflowProjectConfigurationConnectedUsersQuery,
    useAutomationWorkflowProjectConfigurationMoveImpactQuery,
    useMoveAutomationWorkflowProjectConfigurationConnectedUsersMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback, useMemo, useState} from 'react';

type AutomationWorkflowProjectConfigurationType =
    AutomationWorkflowProjectConfigurationsQuery['automationWorkflowProjectConfigurations'][number];

type ConfigurationReferenceType = Pick<AutomationWorkflowProjectConfigurationType, 'id' | 'name' | 'projectVersion'>;

type TargetConfigurationType = ConfigurationReferenceType & {enabled?: boolean};

interface AutomationWorkflowProjectConfigurationMoveUsersDialogProps {
    configuration: ConfigurationReferenceType;
    connectedUsers?: Array<{connectedUserId: string; email?: string | null; externalId: string; name?: string | null}>;
    initialSelectedConnectedUserIds?: string[];
    onClose: () => void;
    targetConfigurations: TargetConfigurationType[];
    title?: string;
}

const formatConfigurationLabel = (configuration: ConfigurationReferenceType): string =>
    `${configuration.name} · v${configuration.projectVersion}`;

const formatTargetLabel = (configuration: TargetConfigurationType): string =>
    `${formatConfigurationLabel(configuration)}${configuration.enabled === false ? ' (disabled)' : ''}`;

interface SkippedConnectedUserI {
    connectedUserId: string;
    reason: string;
    workflowLabels: string[];
}

const AutomationWorkflowProjectConfigurationMoveUsersDialog = ({
    configuration,
    connectedUsers: presetConnectedUsers,
    initialSelectedConnectedUserIds,
    onClose,
    targetConfigurations,
    title = 'Move Users',
}: AutomationWorkflowProjectConfigurationMoveUsersDialogProps) => {
    const [selection, setSelection] = useState<{connectedUserIds?: string[]; targetId?: string}>({});

    const queryClient = useQueryClient();

    const {data: connectedUsersData, isError: connectedUsersError} =
        useAutomationWorkflowProjectConfigurationConnectedUsersQuery(
            {id: configuration.id},
            {enabled: !presetConnectedUsers}
        );

    const connectedUsers =
        presetConnectedUsers || connectedUsersData?.automationWorkflowProjectConfigurationConnectedUsers;

    const {
        data: moveImpactData,
        isError: moveImpactError,
        isLoading: moveImpactLoading,
    } = useAutomationWorkflowProjectConfigurationMoveImpactQuery(
        {
            connectedUserIds: selection.connectedUserIds ?? [],
            sourceId: configuration.id,
            targetId: selection.targetId ?? '',
        },
        {enabled: !!selection.targetId && !!selection.connectedUserIds?.length}
    );

    const moveMutation = useMoveAutomationWorkflowProjectConfigurationConnectedUsersMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['automationWorkflowProjectConfigurations']});
            void queryClient.invalidateQueries({queryKey: ['connectedUserProjects']});
        },
    });

    const targets = useMemo(
        () =>
            targetConfigurations.map((targetConfiguration) => ({
                id: targetConfiguration.id,
                label: formatTargetLabel(targetConfiguration),
            })),
        [targetConfigurations]
    );

    const labelByConnectedUserId = useMemo(
        () =>
            new Map(
                (connectedUsers ?? []).map((connectedUser) => [
                    connectedUser.connectedUserId,
                    connectedUser.name || connectedUser.email || connectedUser.externalId,
                ])
            ),
        [connectedUsers]
    );

    const moveImpact = moveImpactData?.automationWorkflowProjectConfigurationMoveImpact;

    const toSkippedLines = useCallback(
        (skippedUsers: SkippedConnectedUserI[]) =>
            skippedUsers.map((skippedUser) => ({
                label: labelByConnectedUserId.get(skippedUser.connectedUserId) || skippedUser.connectedUserId,
                reason: skippedUser.reason,
                workflowLabels: skippedUser.workflowLabels,
            })),
        [labelByConnectedUserId]
    );

    const previewSkipped = useMemo(() => toSkippedLines(moveImpact?.skipped ?? []), [moveImpact, toSkippedLines]);

    const {mutateAsync: moveConnectedUsers} = moveMutation;

    const handleMove = useCallback(
        async (targetId: string, connectedUserIds: string[]): Promise<MoveConnectedUsersResultI> => {
            const response = await moveConnectedUsers({
                connectedUserIds,
                sourceId: configuration.id,
                targetId,
            });

            const moveResult = response.moveAutomationWorkflowProjectConfigurationConnectedUsers;

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
            connectedUsers={connectedUsers}
            impact={moveImpact?.workflows}
            initialSelectedConnectedUserIds={initialSelectedConnectedUserIds}
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

export default AutomationWorkflowProjectConfigurationMoveUsersDialog;
