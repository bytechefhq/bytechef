import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import EEVersion from '@/shared/edition/EEVersion';
import {McpServer} from '@/shared/middleware/graphql';
import {EditIcon, EllipsisVerticalIcon, RocketIcon, Trash2Icon} from 'lucide-react';

interface McpServerListItemDropdownMenuProps {
    mcpServer: McpServer;
    onDeleteClick: () => void;
    onEditClick: () => void;
    onPromoteClick?: () => void;
    showPromoteToEnvironment?: boolean;
}

const McpServerListItemDropdownMenu = ({
    onDeleteClick,
    onEditClick,
    onPromoteClick,
    showPromoteToEnvironment,
}: McpServerListItemDropdownMenuProps) => {
    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button icon={<EllipsisVerticalIcon />} size="icon" variant="ghost" />
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end">
                <DropdownMenuItem icon={<EditIcon />} label="Edit" onClick={onEditClick} />

                {showPromoteToEnvironment && (
                    <EEVersion hidden={true}>
                        <DropdownMenuItem
                            icon={<RocketIcon />}
                            label="Promote to environment…"
                            onClick={onPromoteClick}
                        />
                    </EEVersion>
                )}

                <DropdownMenuSeparator />

                <DropdownMenuItem icon={<Trash2Icon />} label="Delete" onClick={onDeleteClick} variant="destructive" />
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default McpServerListItemDropdownMenu;
