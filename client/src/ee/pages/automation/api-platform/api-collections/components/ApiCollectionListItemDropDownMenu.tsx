import {Button} from '@/components/ui/button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {DotsVerticalIcon} from '@radix-ui/react-icons';

interface ApiCollectionListItemDropdownMenuProps {
    onDeleteClick: () => void;
    onEditClick: () => void;
    onNewEndpoint: () => void;
    onUpdateProjectVersionClick: () => void;
}

const ApiCollectionListItemDropDownMenu = ({
    onDeleteClick,
    onEditClick,
    onNewEndpoint,
    onUpdateProjectVersionClick,
}: ApiCollectionListItemDropdownMenuProps) => {
    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button size="icon" variant="ghost">
                    <DotsVerticalIcon className="size-4 hover:cursor-pointer" />
                </Button>
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end">
                <DropdownMenuItem onClick={onEditClick}>Edit</DropdownMenuItem>

                <DropdownMenuItem onClick={onUpdateProjectVersionClick}>Update Project Version</DropdownMenuItem>

                <DropdownMenuItem onClick={onNewEndpoint}>New Endpoint</DropdownMenuItem>

                <DropdownMenuSeparator />

                <DropdownMenuItem className="text-red-600" onClick={onDeleteClick}>
                    Delete
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default ApiCollectionListItemDropDownMenu;
