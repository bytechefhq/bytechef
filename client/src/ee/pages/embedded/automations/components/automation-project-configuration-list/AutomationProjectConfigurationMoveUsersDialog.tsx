import MoveConnectedUsersDialog, {
    MoveConnectedUsersResultI,
} from '@/ee/pages/embedded/connected-users/components/move-connected-users-dialog/MoveConnectedUsersDialog';
import {
    AutomationProjectConfigurationsQuery,
    useAutomationProjectConfigurationConnectedUsersQuery,
    useAutomationProjectConfigurationMoveImpactQuery,
    useMoveAutomationProjectConfigurationConnectedUsersMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback, useMemo, useState} from 'react';

type AutomationProjectConfigurationType =
    AutomationProjectConfigurationsQuery['automationProjectConfigurations'][number];

type ConfigurationReferenceType = Pick<AutomationProjectConfigurationType, 'id' | 'name' | 'projectVersion'>;

type TargetConfigurationType = ConfigurationReferenceType & {enabled?: boolean};

interface AutomationProjectConfigurationMoveUsersDialogProps {
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

const AutomationProjectConfigurationMoveUsersDialog = ({
    configuration,
    connectedUsers: presetConnectedUsers,
    initialSelectedConnectedUserIds,
    onClose,
    targetConfigurations,
    title = 'Move Users',
}: AutomationProjectConfigurationMoveUsersDialogProps) => {
    const [selection, setSelection] = useState<{connectedUserIds?: string[]; targetId?: string}>({});

    const queryClient = useQueryClient();

    const {data: connectedUsersData, isError: connectedUsersError} =
        useAutomationProjectConfigurationConnectedUsersQuery({id: configuration.id}, {enabled: !presetConnectedUsers});

    const connectedUsers = presetConnectedUsers || connectedUsersData?.automationProjectConfigurationConnectedUsers;

    const {
        data: moveImpactData,
        isError: moveImpactError,
        isLoading: moveImpactLoading,
    } = useAutomationProjectConfigurationMoveImpactQuery(
        {
            connectedUserIds: selection.connectedUserIds ?? [],
            sourceId: configuration.id,
            targetId: selection.targetId ?? '',
        },
        {enabled: !!selection.targetId && !!selection.connectedUserIds?.length}
    );

    const moveMutation = useMoveAutomationProjectConfigurationConnectedUsersMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['automationProjectConfigurations']});
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

    const moveImpact = moveImpactData?.automationProjectConfigurationMoveImpact;

    const {mutateAsync: moveConnectedUsers} = moveMutation;

    const handleMove = useCallback(
        async (targetId: string, connectedUserIds: string[]): Promise<MoveConnectedUsersResultI> => {
            const response = await moveConnectedUsers({
                connectedUserIds,
                sourceId: configuration.id,
                targetId,
            });

            return response.moveAutomationProjectConfigurationConnectedUsers;
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

export default AutomationProjectConfigurationMoveUsersDialog;
