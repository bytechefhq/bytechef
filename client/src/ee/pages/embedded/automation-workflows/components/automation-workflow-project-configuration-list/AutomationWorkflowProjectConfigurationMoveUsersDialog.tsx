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

const AutomationWorkflowProjectConfigurationMoveUsersDialog = ({
    configuration,
    onClose,
    targetConfigurations,
}: AutomationWorkflowProjectConfigurationMoveUsersDialogProps) => {
    const [selection, setSelection] = useState<{connectedUserIds?: string[]; targetId?: string}>({});

    const queryClient = useQueryClient();

    const {data: connectedUsersData} = useAutomationWorkflowProjectConfigurationConnectedUsersQuery({
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

    const previewSkipped = useMemo(
        () =>
            (moveImpact?.skipped ?? []).map((skippedUser) => ({
                label: labelByConnectedUserId.get(skippedUser.connectedUserId) || skippedUser.connectedUserId,
                reason: skippedUser.reason,
                workflowLabels: skippedUser.workflowLabels,
            })),
        [labelByConnectedUserId, moveImpact]
    );

    const handleMove = useCallback(
        async (targetId: string, connectedUserIds: string[]): Promise<MoveConnectedUsersResultI> => {
            const response = await moveMutation.mutateAsync({
                connectedUserIds,
                sourceId: configuration.id,
                targetId,
            });

            const moveResult = response.moveAutomationWorkflowProjectConfigurationConnectedUsers;

            return {
                movedCount: moveResult.movedConnectedUserIds.length,
                needsAttentionCount: moveResult.needsAttentionConnectedUserIds.length,
                skipped: moveResult.skipped.map((skippedUser) => ({
                    label: labelByConnectedUserId.get(skippedUser.connectedUserId) || skippedUser.connectedUserId,
                    reason: skippedUser.reason,
                    workflowLabels: skippedUser.workflowLabels,
                })),
            };
        },
        [configuration.id, labelByConnectedUserId, moveMutation]
    );

    if (!connectedUsers) {
        return (
            <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50" role="status">
                <span className="rounded bg-background px-4 py-2 text-sm">Loading users...</span>
            </div>
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
