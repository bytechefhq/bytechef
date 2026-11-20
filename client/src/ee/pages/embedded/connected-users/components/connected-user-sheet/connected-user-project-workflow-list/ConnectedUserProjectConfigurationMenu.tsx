import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import AutomationProjectConfigurationMoveUsersDialog from '@/ee/pages/embedded/automations/components/automation-project-configuration-list/AutomationProjectConfigurationMoveUsersDialog';
import ConnectedUserChangeVersionDialog, {
    NO_OTHER_VERSION_REASON,
    VERSIONS_LOAD_FAILED_REASON,
    getOtherVersions,
} from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/change-version-dialog/ConnectedUserChangeVersionDialog';
import {MoveConnectedUsersResultI} from '@/ee/pages/embedded/connected-users/components/move-connected-users-dialog/MoveConnectedUsersDialog';
import {
    CONFIGURATIONS_LOAD_FAILED_REASON,
    NO_OTHER_CONFIGURATION_REASON,
} from '@/ee/pages/embedded/connected-users/components/move-connected-users-dialog/moveConnectedUsersReasons';
import {MoveWorkflowInputsI} from '@/ee/pages/embedded/connected-users/components/move-connected-users-dialog/useMoveWorkflowInputs';
import {
    useAutomationProjectConfigurationMoveImpactQuery,
    useAutomationProjectConfigurationsQuery,
    useAutomationProjectVersionsQuery,
    useChangeAutomationProjectConfigurationConnectedUserVersionMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {ArrowUpDownIcon, EllipsisVerticalIcon, RefreshCcwIcon} from 'lucide-react';
import {useMemo, useState} from 'react';

export interface ConnectedUserCurrentConfigurationI {
    configurationId: string;
    configurationName: string;
    configurationProjectVersion: number;
}

interface ConnectedUserProjectConfigurationMenuProps {
    connectedUser: {connectedUserId: string; email?: string | null; externalId: string; name?: string | null};
    currentConfiguration: ConnectedUserCurrentConfigurationI;
    projectId: string;
}

const ConnectedUserProjectConfigurationMenu = ({
    connectedUser,
    currentConfiguration,
    projectId,
}: ConnectedUserProjectConfigurationMenuProps) => {
    const [showChangeDialog, setShowChangeDialog] = useState(false);
    const [showChangeVersionDialog, setShowChangeVersionDialog] = useState(false);
    const [changeVersionTargetId, setChangeVersionTargetId] = useState<string>();

    const queryClient = useQueryClient();

    const {data, isError: configurationsError} = useAutomationProjectConfigurationsQuery({projectId});

    const {data: versionsData, isError: versionsError} = useAutomationProjectVersionsQuery({id: projectId});

    const targetConfigurations = useMemo(() => {
        const configurations = data?.automationProjectConfigurations ?? [];

        const currentConfigurationEnvironmentId = configurations.find(
            (configuration) => configuration.id === currentConfiguration.configurationId
        )?.environmentId;

        return configurations.filter(
            (configuration) =>
                configuration.id !== currentConfiguration.configurationId &&
                configuration.environmentId === currentConfigurationEnvironmentId
        );
    }, [currentConfiguration.configurationId, data]);

    const publishedVersions = useMemo(
        () =>
            (versionsData?.automationProjectVersions ?? [])
                .filter((projectVersion) => projectVersion.status === 'PUBLISHED')
                .map((projectVersion) => projectVersion.version),
        [versionsData]
    );

    const changeVersionConfigurations = useMemo(
        () =>
            targetConfigurations.map((targetConfiguration) => ({
                enabled: targetConfiguration.enabled,
                id: targetConfiguration.id,
                name: targetConfiguration.name,
                version: targetConfiguration.projectVersion,
            })),
        [targetConfigurations]
    );

    const getChangeConfigurationDisabledReason = () => {
        if (configurationsError) {
            return CONFIGURATIONS_LOAD_FAILED_REASON;
        }

        return data && targetConfigurations.length === 0 ? NO_OTHER_CONFIGURATION_REASON : undefined;
    };

    const getChangeVersionDisabledReason = () => {
        if (configurationsError) {
            return CONFIGURATIONS_LOAD_FAILED_REASON;
        }

        if (versionsError) {
            return VERSIONS_LOAD_FAILED_REASON;
        }

        return versionsData &&
            getOtherVersions(publishedVersions, currentConfiguration.configurationProjectVersion).length === 0
            ? NO_OTHER_VERSION_REASON
            : undefined;
    };

    const changeConfigurationDisabledReason = getChangeConfigurationDisabledReason();

    const changeVersionDisabledReason = getChangeVersionDisabledReason();

    const changeVersionMutation = useChangeAutomationProjectConfigurationConnectedUserVersionMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['automationProjectConfigurations']});
            void queryClient.invalidateQueries({queryKey: ['connectedUserProjects']});
        },
    });

    const {mutateAsync: changeConnectedUserVersion} = changeVersionMutation;

    const {
        data: changeVersionMoveImpactData,
        isError: changeVersionMoveImpactError,
        isLoading: changeVersionMoveImpactLoading,
    } = useAutomationProjectConfigurationMoveImpactQuery(
        {
            connectedUserIds: [connectedUser.connectedUserId],
            sourceId: currentConfiguration.configurationId,
            targetId: changeVersionTargetId ?? '',
        },
        {enabled: showChangeVersionDialog && !!changeVersionTargetId}
    );

    const changeVersionMoveImpact = changeVersionMoveImpactData?.automationProjectConfigurationMoveImpact;

    const handleChangeVersion = async (
        version: number,
        targetId: string | undefined,
        workflowInputs: MoveWorkflowInputsI[]
    ): Promise<MoveConnectedUsersResultI> => {
        const response = await changeConnectedUserVersion({
            connectedUserId: connectedUser.connectedUserId,
            projectVersion: version,
            sourceId: currentConfiguration.configurationId,
            targetId,
            workflowInputs,
        });

        return response.changeAutomationProjectConfigurationConnectedUserVersion;
    };

    const handleChangeVersionClose = () => {
        setChangeVersionTargetId(undefined);
        setShowChangeVersionDialog(false);
    };

    const connectedUsers = useMemo(() => [connectedUser], [connectedUser]);

    const configuration = useMemo(
        () => ({
            id: currentConfiguration.configurationId,
            name: currentConfiguration.configurationName,
            projectVersion: currentConfiguration.configurationProjectVersion,
        }),
        [currentConfiguration]
    );

    return (
        <>
            <DropdownMenu>
                <DropdownMenuTrigger asChild>
                    <Button
                        aria-label="Configuration Actions"
                        icon={<EllipsisVerticalIcon />}
                        size="icon"
                        variant="ghost"
                    />
                </DropdownMenuTrigger>

                <DropdownMenuContent align="end">
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <span className="block">
                                <DropdownMenuItem
                                    aria-label="Change configuration"
                                    disabled={!data || !!changeConfigurationDisabledReason}
                                    icon={<RefreshCcwIcon />}
                                    label="Change configuration…"
                                    onClick={() => setShowChangeDialog(true)}
                                />
                            </span>
                        </TooltipTrigger>

                        {changeConfigurationDisabledReason && (
                            <TooltipContent side="left">{changeConfigurationDisabledReason}</TooltipContent>
                        )}
                    </Tooltip>

                    <Tooltip>
                        <TooltipTrigger asChild>
                            <span className="block">
                                <DropdownMenuItem
                                    aria-label="Change version"
                                    disabled={!versionsData || !data || !!changeVersionDisabledReason}
                                    icon={<ArrowUpDownIcon />}
                                    label="Change version…"
                                    onClick={() => setShowChangeVersionDialog(true)}
                                />
                            </span>
                        </TooltipTrigger>

                        {changeVersionDisabledReason && (
                            <TooltipContent side="left">{changeVersionDisabledReason}</TooltipContent>
                        )}
                    </Tooltip>
                </DropdownMenuContent>
            </DropdownMenu>

            {showChangeDialog && (
                <AutomationProjectConfigurationMoveUsersDialog
                    configuration={configuration}
                    connectedUsers={connectedUsers}
                    initialSelectedConnectedUserIds={[connectedUser.connectedUserId]}
                    onClose={() => setShowChangeDialog(false)}
                    singleConnectedUser
                    targetConfigurations={targetConfigurations}
                    title="Change configuration"
                />
            )}

            {showChangeVersionDialog && (
                <ConnectedUserChangeVersionDialog
                    configurations={changeVersionConfigurations}
                    connectedUserLabel={connectedUser.name || connectedUser.email || connectedUser.externalId}
                    currentConfigurationName={currentConfiguration.configurationName}
                    currentVersion={currentConfiguration.configurationProjectVersion}
                    editableInputs
                    impact={changeVersionMoveImpact?.workflows}
                    onChange={handleChangeVersion}
                    onClose={handleChangeVersionClose}
                    onTargetChange={setChangeVersionTargetId}
                    pending={changeVersionMutation.isPending}
                    previewError={changeVersionMoveImpactError}
                    previewLoading={changeVersionMoveImpactLoading}
                    previewSkipped={changeVersionMoveImpact?.skipped}
                    versions={publishedVersions}
                />
            )}
        </>
    );
};

export default ConnectedUserProjectConfigurationMenu;
