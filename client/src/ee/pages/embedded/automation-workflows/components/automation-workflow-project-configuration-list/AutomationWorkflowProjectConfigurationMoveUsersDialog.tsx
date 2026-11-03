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

    const moveImpact = moveImpactData?.automationWorkflowProjectConfigurationMoveImpact;

    const {mutateAsync: moveConnectedUsers} = moveMutation;

    const handleMove = useCallback(
        async (targetId: string, connectedUserIds: string[]): Promise<MoveConnectedUsersResultI> => {
            const response = await moveConnectedUsers({
                connectedUserIds,
                sourceId: configuration.id,
                targetId,
            });

            return response.moveAutomationWorkflowProjectConfigurationConnectedUsers;
        },
        [configuration.id, moveConnectedUsers]
    );

    return (
        <MoveConnectedUsersDialog
            connectedUsers={connectedUsers}
            connectedUsersError={connectedUsersError}
            impact={moveImpact?.workflows}
            initialSelectedConnectedUserIds={initialSelectedConnectedUserIds}
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

export default AutomationWorkflowProjectConfigurationMoveUsersDialog;
