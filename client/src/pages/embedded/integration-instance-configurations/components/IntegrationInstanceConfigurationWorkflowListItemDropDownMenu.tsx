import {Button} from '@/components/ui/button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {WorkflowModel} from '@/shared/middleware/automation/configuration';
import {EllipsisVerticalIcon} from 'lucide-react';

interface IntegrationInstanceConfigurationWorkflowListItemDropDownMenuProps {
    onEditClick: () => void;
    onEnableClick: () => void;
    integrationInstanceConfigurationEnabled: boolean;
    integrationInstanceConfigurationWorkflowEnabled: boolean;
    workflow: WorkflowModel;
}

const IntegrationInstanceConfigurationWorkflowListItemDropDownMenuProps = ({
    integrationInstanceConfigurationEnabled,
    integrationInstanceConfigurationWorkflowEnabled,
    onEditClick,
    onEnableClick,
    workflow,
}: IntegrationInstanceConfigurationWorkflowListItemDropDownMenuProps) => {
    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button size="icon" variant="ghost">
                    <EllipsisVerticalIcon className="size-4 hover:cursor-pointer" />
                </Button>
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end">
                <DropdownMenuItem
                    disabled={
                        integrationInstanceConfigurationEnabled ||
                        (workflow.connectionsCount === 0 && workflow?.inputsCount === 0)
                    }
                    onClick={onEditClick}
                >
                    Edit
                </DropdownMenuItem>

                <DropdownMenuItem disabled={integrationInstanceConfigurationEnabled} onClick={onEnableClick}>
                    {integrationInstanceConfigurationWorkflowEnabled ? 'Disable' : 'Enable'}
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default IntegrationInstanceConfigurationWorkflowListItemDropDownMenuProps;
