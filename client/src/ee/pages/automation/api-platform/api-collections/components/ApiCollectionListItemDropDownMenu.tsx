import '@/shared/styles/dropdownMenu.css';
import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';
import {DownloadIcon, EditIcon, EllipsisVerticalIcon, PlusIcon, RefreshCcwIcon, Trash2Icon} from 'lucide-react';

interface ApiCollectionListItemDropdownMenuProps {
    apiCollectionId: number;
    onChangeProjectVersionClick: () => void;
    onDeleteClick: () => void;
    onEditClick: () => void;
    onNewEndpoint: () => void;
}

const ApiCollectionListItemDropDownMenu = ({
    apiCollectionId,
    onChangeProjectVersionClick,
    onDeleteClick,
    onEditClick,
    onNewEndpoint,
}: ApiCollectionListItemDropdownMenuProps) => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const canEdit = useHasWorkspaceScope(currentWorkspaceId, 'API_PLATFORM_EDIT');
    const canChangeProjectVersion = useHasWorkspaceScope(currentWorkspaceId, 'DEPLOYMENT_CREATE');
    // Delete is ADMIN-tier on the server (ApiCollectionFacadeImpl.deleteApiCollection), so offering it to a member without API_PLATFORM_DELETE means a confirm dialog
    // that ends in a 403.
    const canDelete = useHasWorkspaceScope(currentWorkspaceId, 'API_PLATFORM_DELETE');

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button
                    aria-label="API Collection actions"
                    icon={<EllipsisVerticalIcon />}
                    size="icon"
                    variant="ghost"
                />
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end" className="p-0">
                {canEdit && (
                    <DropdownMenuItem className="dropdown-menu-item" onClick={onEditClick}>
                        <EditIcon /> Edit
                    </DropdownMenuItem>
                )}

                {canChangeProjectVersion && (
                    <DropdownMenuItem className="dropdown-menu-item" onClick={onChangeProjectVersionClick}>
                        <RefreshCcwIcon /> Change Project Version
                    </DropdownMenuItem>
                )}

                {canEdit && (
                    <DropdownMenuItem className="dropdown-menu-item" onClick={onNewEndpoint}>
                        <PlusIcon /> New Endpoint
                    </DropdownMenuItem>
                )}

                <DropdownMenuItem
                    className="dropdown-menu-item"
                    onClick={() =>
                        (window.location.href = `/api/automation/api-platform/internal/api-collections/${apiCollectionId}/openapi.json`)
                    }
                >
                    <DownloadIcon /> Download OpenAPI Spec
                </DropdownMenuItem>

                {canDelete && (
                    <>
                        <DropdownMenuSeparator className="m-0" />

                        <DropdownMenuItem
                            className="dropdown-menu-item-destructive"
                            onClick={onDeleteClick}
                            variant="destructive"
                        >
                            <Trash2Icon /> Delete
                        </DropdownMenuItem>
                    </>
                )}
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default ApiCollectionListItemDropDownMenu;
