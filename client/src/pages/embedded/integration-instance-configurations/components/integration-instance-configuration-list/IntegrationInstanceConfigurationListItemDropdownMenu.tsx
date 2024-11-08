import {Button} from '@/components/ui/button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {EllipsisVerticalIcon} from 'lucide-react';

interface IntegrationInstanceConfigurationListItemDropdownMenuProps {
    integrationInstanceConfigurationEnabled: boolean;
    onDeleteClick: () => void;
    onEditClick: () => void;
    onEnableClick: () => void;
    onUpdateIntegrationVersionClick: () => void;
}

const IntegrationInstanceConfigurationListItemDropdownMenu = ({
    integrationInstanceConfigurationEnabled,
    onDeleteClick,
    onEditClick,
    onEnableClick,
    onUpdateIntegrationVersionClick,
}: IntegrationInstanceConfigurationListItemDropdownMenuProps) => {
    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button size="icon" variant="ghost">
                    <EllipsisVerticalIcon className="size-4 hover:cursor-pointer" />
                </Button>
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end">
                <DropdownMenuItem onClick={onEditClick}>Edit</DropdownMenuItem>

                <DropdownMenuItem onClick={onUpdateIntegrationVersionClick}>
                    Update Integration Version
                </DropdownMenuItem>

                <DropdownMenuItem onClick={onEnableClick}>
                    {integrationInstanceConfigurationEnabled ? 'Disable' : 'Enable'}
                </DropdownMenuItem>

                <DropdownMenuSeparator />

                <DropdownMenuItem className="text-destructive" onClick={onDeleteClick}>
                    Delete
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default IntegrationInstanceConfigurationListItemDropdownMenu;
