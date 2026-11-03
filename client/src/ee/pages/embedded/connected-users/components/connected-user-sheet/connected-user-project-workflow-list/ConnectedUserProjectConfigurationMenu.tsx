import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import AutomationWorkflowProjectConfigurationMoveUsersDialog from '@/ee/pages/embedded/automation-workflows/components/automation-workflow-project-configuration-list/AutomationWorkflowProjectConfigurationMoveUsersDialog';
import {NO_OTHER_CONFIGURATION_REASON} from '@/ee/pages/embedded/connected-users/components/move-connected-users-dialog/moveConnectedUsersReasons';
import {useAutomationWorkflowProjectConfigurationsQuery} from '@/shared/middleware/graphql';
import {EllipsisVerticalIcon, RefreshCcwIcon} from 'lucide-react';
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

    const {data} = useAutomationWorkflowProjectConfigurationsQuery({projectId});

    const targetConfigurations = useMemo(() => {
        const configurations = data?.automationWorkflowProjectConfigurations ?? [];

        const currentConfigurationEnvironmentId = configurations.find(
            (configuration) => configuration.id === currentConfiguration.configurationId
        )?.environmentId;

        return configurations.filter(
            (configuration) =>
                configuration.id !== currentConfiguration.configurationId &&
                configuration.environmentId === currentConfigurationEnvironmentId
        );
    }, [currentConfiguration.configurationId, data]);

    const connectedUsers = useMemo(() => [connectedUser], [connectedUser]);

    const configuration = useMemo(
        () => ({
            id: currentConfiguration.configurationId,
            name: currentConfiguration.configurationName,
            projectVersion: currentConfiguration.configurationProjectVersion,
        }),
        [currentConfiguration]
    );

    const disabledReason = data && targetConfigurations.length === 0 ? NO_OTHER_CONFIGURATION_REASON : undefined;

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
                                    disabled={!data || !!disabledReason}
                                    icon={<RefreshCcwIcon />}
                                    label="Change configuration…"
                                    onClick={() => setShowChangeDialog(true)}
                                />
                            </span>
                        </TooltipTrigger>

                        {disabledReason && <TooltipContent side="left">{disabledReason}</TooltipContent>}
                    </Tooltip>
                </DropdownMenuContent>
            </DropdownMenu>

            {showChangeDialog && (
                <AutomationWorkflowProjectConfigurationMoveUsersDialog
                    configuration={configuration}
                    connectedUsers={connectedUsers}
                    initialSelectedConnectedUserIds={[connectedUser.connectedUserId]}
                    onClose={() => setShowChangeDialog(false)}
                    targetConfigurations={targetConfigurations}
                    title="Change configuration"
                />
            )}
        </>
    );
};

export default ConnectedUserProjectConfigurationMenu;
