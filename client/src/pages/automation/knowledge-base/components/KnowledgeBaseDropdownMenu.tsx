import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import EditKnowledgeBaseDialog from '@/pages/automation/knowledge-base/components/EditKnowledgeBaseDialog';
import KnowledgeBaseDeleteAlertDialog from '@/pages/automation/knowledge-base/components/KnowledgeBaseDeleteAlertDialog';
import useKnowledgeBaseDropdownMenu from '@/pages/automation/knowledge-base/components/hooks/useKnowledgeBaseDropdownMenu';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';
import {KnowledgeBase} from '@/shared/middleware/graphql';
import {EditIcon, EllipsisVerticalIcon, Trash2Icon} from 'lucide-react';

interface KnowledgeBaseDropdownMenuProps {
    knowledgeBase: KnowledgeBase;
}

const KnowledgeBaseDropdownMenu = ({knowledgeBase}: KnowledgeBaseDropdownMenuProps) => {
    const {
        handleCloseDeleteDialog,
        handleCloseEditDialog,
        handleShowDeleteDialog,
        handleShowEditDialog,
        showDeleteDialog,
        showEditDialog,
    } = useKnowledgeBaseDropdownMenu();

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    // Delete is ADMIN-tier on the server (WorkspaceKnowledgeBaseFacadeImpl.deleteWorkspaceKnowledgeBase), so offering it
    // to a member without KNOWLEDGE_BASE_DELETE means a confirm dialog that ends in a 403.
    const canDelete = useHasWorkspaceScope(currentWorkspaceId, 'KNOWLEDGE_BASE_DELETE');
    const canEditKnowledgeBase = useHasWorkspaceScope(currentWorkspaceId, 'KNOWLEDGE_BASE_EDIT');

    return (
        <>
            {(canEditKnowledgeBase || canDelete) && (
                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <Button
                            aria-label="More Knowledge Base Actions"
                            icon={<EllipsisVerticalIcon />}
                            size="icon"
                            variant="ghost"
                        />
                    </DropdownMenuTrigger>

                    <DropdownMenuContent align="end" className="p-0">
                        {canEditKnowledgeBase && (
                            <DropdownMenuItem className="dropdown-menu-item" onClick={handleShowEditDialog}>
                                <EditIcon /> Edit
                            </DropdownMenuItem>
                        )}

                        {canEditKnowledgeBase && canDelete && <DropdownMenuSeparator className="m-0" />}

                        {canDelete && (
                            <DropdownMenuItem
                                className="dropdown-menu-item-destructive"
                                onClick={handleShowDeleteDialog}
                                variant="destructive"
                            >
                                <Trash2Icon /> Delete
                            </DropdownMenuItem>
                        )}
                    </DropdownMenuContent>
                </DropdownMenu>
            )}

            <EditKnowledgeBaseDialog
                knowledgeBase={knowledgeBase}
                onOpenChange={handleCloseEditDialog}
                open={showEditDialog}
            />

            <KnowledgeBaseDeleteAlertDialog
                knowledgeBaseId={knowledgeBase.id}
                onClose={handleCloseDeleteDialog}
                open={showDeleteDialog}
            />
        </>
    );
};

export default KnowledgeBaseDropdownMenu;
