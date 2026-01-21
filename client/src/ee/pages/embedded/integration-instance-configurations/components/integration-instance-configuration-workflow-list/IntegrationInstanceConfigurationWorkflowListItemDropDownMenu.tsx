import Button from '@/components/Button/Button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {Workflow} from '@/shared/middleware/automation/configuration';
import {EllipsisVerticalIcon} from 'lucide-react';

interface IntegrationInstanceConfigurationWorkflowListItemDropDownMenuProps {
    onEditClick: () => void;
    workflow: Workflow;
}

const IntegrationInstanceConfigurationWorkflowListItemDropDownMenu = ({
    onEditClick,
    workflow,
}: IntegrationInstanceConfigurationWorkflowListItemDropDownMenuProps) => {
    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button
                    icon={<EllipsisVerticalIcon className="size-4 hover:cursor-pointer" />}
                    size="icon"
                    variant="ghost"
                />
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end">
                <DropdownMenuItem
                    disabled={workflow.connectionsCount === 0 && workflow?.inputsCount === 0}
                    onClick={onEditClick}
                >
                    Edit
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default IntegrationInstanceConfigurationWorkflowListItemDropDownMenu;
