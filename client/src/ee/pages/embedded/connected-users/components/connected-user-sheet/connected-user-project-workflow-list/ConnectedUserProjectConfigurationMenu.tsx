import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import AutomationWorkflowProjectConfigurationMoveUsersDialog from '@/ee/pages/embedded/automation-workflows/components/automation-workflow-project-configuration-list/AutomationWorkflowProjectConfigurationMoveUsersDialog';
import {useAutomationWorkflowProjectConfigurationsQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {EllipsisVerticalIcon, RefreshCcwIcon} from 'lucide-react';
import {useMemo, useState} from 'react';

const NO_OTHER_CONFIGURATION_REASON = 'No other configuration in this environment';

export interface ConnectedUserCurrentConfigurationI {
    configurationId: string;
    configurationName: string;
    configurationProjectVersion: number;
}

interface ConnectedUserProjectConfigurationMenuProps {
    connectedUser: {connectedUserId: string; externalId: string};
    currentConfiguration: ConnectedUserCurrentConfigurationI;
    projectId: string;
}

const ConnectedUserProjectConfigurationMenu = ({
    connectedUser,
    currentConfiguration,
    projectId,
}: ConnectedUserProjectConfigurationMenuProps) => {
    const [showChangeDialog, setShowChangeDialog] = useState(false);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {data} = useAutomationWorkflowProjectConfigurationsQuery({projectId});

    const targetConfigurations = useMemo(
        () =>
            (data?.automationWorkflowProjectConfigurations ?? []).filter(
                (configuration) =>
                    configuration.enabled &&
                    configuration.id !== currentConfiguration.configurationId &&
                    configuration.environmentId === currentEnvironmentId?.toString()
            ),
        [currentConfiguration.configurationId, currentEnvironmentId, data]
    );

    const connectedUsers = useMemo(() => [connectedUser], [connectedUser]);

    const configuration = useMemo(
        () => ({
            id: currentConfiguration.configurationId,
            name: currentConfiguration.configurationName,
            projectVersion: currentConfiguration.configurationProjectVersion,
        }),
        [currentConfiguration]
    );

    const disabledReason = targetConfigurations.length === 0 ? NO_OTHER_CONFIGURATION_REASON : undefined;

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
                                    disabled={!!disabledReason}
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
