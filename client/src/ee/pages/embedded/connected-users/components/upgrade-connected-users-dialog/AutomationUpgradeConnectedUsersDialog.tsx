import UpgradeConnectedUsersDialogView, {
    UpgradeOptionI,
} from '@/ee/pages/embedded/connected-users/components/upgrade-connected-users-dialog/UpgradeConnectedUsersDialogView';
import useUpgradeSelection from '@/ee/pages/embedded/connected-users/components/upgrade-connected-users-dialog/useUpgradeSelection';
import {ConnectedUser} from '@/ee/shared/middleware/embedded/connected-user';
import {
    useAutomationWorkflowProjectConfigurationUpgradeImpactQuery,
    useAutomationWorkflowProjectConfigurationsQuery,
    useAutomationWorkflowProjectsQuery,
    useUpgradeAutomationWorkflowProjectConfigurationConnectedUsersMutation,
} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useMemo} from 'react';

import type {MoveConnectedUsersResultI} from '@/ee/pages/embedded/connected-users/components/move-connected-users-dialog/MoveConnectedUsersDialog';

interface AutomationUpgradeConnectedUsersDialogProps {
    connectedUsers: ConnectedUser[];
    onClose: () => void;
    onUpgraded: () => void;
}

const AutomationUpgradeConnectedUsersDialog = ({
    connectedUsers,
    onClose,
    onUpgraded,
}: AutomationUpgradeConnectedUsersDialogProps) => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {data: projectsData} = useAutomationWorkflowProjectsQuery({environmentId: String(currentEnvironmentId)});

    const connectedUserIds = useMemo(
        () => connectedUsers.map((connectedUser) => String(connectedUser.id)),
        [connectedUsers]
    );

    const parentOptions = useMemo<UpgradeOptionI[]>(
        () =>
            (projectsData?.automationWorkflowProjects || [])
                .filter((project) => project.published && project.configurationCount > 0)
                .map((project) => ({id: project.id, label: project.name})),
        [projectsData]
    );

    const {
        parentId,
        selectParent,
        selectTarget,
        targetId: selectedTargetId,
    } = useUpgradeSelection(parentOptions.map((option) => option.id));

    const {data: configurationsData} = useAutomationWorkflowProjectConfigurationsQuery(
        {environmentId: String(currentEnvironmentId), projectId: parentId || ''},
        {enabled: !!parentId}
    );

    const targetOptions = useMemo<UpgradeOptionI[]>(() => {
        const configurations = configurationsData?.automationWorkflowProjectConfigurations || [];
        const versions = configurations.map((configuration) => configuration.projectVersion);
        const highestVersion = Math.max(...versions);
        const lowestVersion = Math.min(...versions);

        return configurations.map((configuration) => ({
            badge:
                highestVersion > lowestVersion && configuration.projectVersion === highestVersion
                    ? 'Latest version'
                    : undefined,
            id: configuration.id,
            label: `${configuration.name} · V${configuration.projectVersion}`,
        }));
    }, [configurationsData]);

    const targetId = targetOptions.some((option) => option.id === selectedTargetId) ? selectedTargetId : undefined;

    const {
        data: impactData,
        isError: impactError,
        isLoading: impactLoading,
    } = useAutomationWorkflowProjectConfigurationUpgradeImpactQuery(
        {connectedUserIds, targetId: targetId || ''},
        {enabled: !!targetId && connectedUserIds.length > 0}
    );

    const {mutateAsync: upgradeConnectedUsers} =
        useUpgradeAutomationWorkflowProjectConfigurationConnectedUsersMutation();

    const handleUpgrade = async (): Promise<MoveConnectedUsersResultI> => {
        const response = await upgradeConnectedUsers({connectedUserIds, targetId: targetId!});

        return response.upgradeAutomationWorkflowProjectConfigurationConnectedUsers;
    };

    return (
        <UpgradeConnectedUsersDialogView
            connectedUsers={connectedUsers}
            emptyParentMessage="No published project has configurations in this environment."
            impact={targetId ? impactData?.automationWorkflowProjectConfigurationUpgradeImpact : undefined}
            notOnSourceReason="Doesn't have this project"
            onClose={onClose}
            onParentChange={selectParent}
            onTargetChange={selectTarget}
            onUpgrade={handleUpgrade}
            onUpgraded={onUpgraded}
            parentId={parentId}
            parentLabel="Project"
            parentOptions={parentOptions}
            previewError={impactError}
            previewLoading={impactLoading}
            targetId={targetId}
            targetOptions={targetOptions}
            title="Upgrade Automation"
        />
    );
};

export default AutomationUpgradeConnectedUsersDialog;
