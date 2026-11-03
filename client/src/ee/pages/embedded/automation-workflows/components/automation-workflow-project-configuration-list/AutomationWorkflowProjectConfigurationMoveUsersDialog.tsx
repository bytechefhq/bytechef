import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
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

interface AutomationWorkflowProjectConfigurationMoveUsersDialogProps {
    configuration: AutomationWorkflowProjectConfigurationType;
    onClose: () => void;
    targetConfigurations: AutomationWorkflowProjectConfigurationType[];
}

const formatConfigurationLabel = (configuration: AutomationWorkflowProjectConfigurationType): string =>
    `${configuration.name} · v${configuration.projectVersion}`;

interface SkippedConnectedUserI {
    connectedUserId: string;
    reason: string;
    workflowLabels: string[];
}

const AutomationWorkflowProjectConfigurationMoveUsersDialog = ({
    configuration,
    onClose,
    targetConfigurations,
}: AutomationWorkflowProjectConfigurationMoveUsersDialogProps) => {
    const [selection, setSelection] = useState<{connectedUserIds?: string[]; targetId?: string}>({});

    const queryClient = useQueryClient();

    const {data: connectedUsersData, isError: connectedUsersError} =
        useAutomationWorkflowProjectConfigurationConnectedUsersQuery({
            id: configuration.id,
        });

    const connectedUsers = connectedUsersData?.automationWorkflowProjectConfigurationConnectedUsers;

    const {data: moveImpactData} = useAutomationWorkflowProjectConfigurationMoveImpactQuery(
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
                label: formatConfigurationLabel(targetConfiguration),
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
                        <DialogTitle>Move Users</DialogTitle>

                        <DialogDescription>
                            {connectedUsersError ? 'Could not load the connected users.' : 'Loading users...'}
                        </DialogDescription>
                    </DialogHeader>

                    {connectedUsersError && (
                        <DialogFooter>
                            <Button label="Close" onClick={onClose} />
                        </DialogFooter>
                    )}
                </DialogContent>
            </Dialog>
        );
    }

    return (
        <MoveConnectedUsersDialog
            connectedUsers={connectedUsers}
            impact={moveImpact?.workflows}
            onClose={onClose}
            onMove={handleMove}
            onSelectionChange={(targetId, connectedUserIds) => setSelection({connectedUserIds, targetId})}
            previewSkipped={previewSkipped}
            sourceLabel={formatConfigurationLabel(configuration)}
            targets={targets}
            title="Move Users"
        />
    );
};

export default AutomationWorkflowProjectConfigurationMoveUsersDialog;
