import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import EditKnowledgeBaseDialog from '@/pages/automation/knowledge-base/components/EditKnowledgeBaseDialog';
import KnowledgeBaseDeleteAlertDialog from '@/pages/automation/knowledge-base/components/KnowledgeBaseDeleteAlertDialog';
import useKnowledgeBaseDropdownMenu from '@/pages/automation/knowledge-base/components/hooks/useKnowledgeBaseDropdownMenu';
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

    return (
        <>
            <DropdownMenu>
                <DropdownMenuTrigger asChild>
                    <Button
                        aria-label="More Knowledge Base Actions"
                        icon={<EllipsisVerticalIcon />}
                        size="icon"
                        variant="ghost"
                    />
                </DropdownMenuTrigger>

                <DropdownMenuContent align="end">
                    <DropdownMenuItem icon={<EditIcon />} label="Edit" onClick={handleShowEditDialog} />

                    <DropdownMenuSeparator />

                    <DropdownMenuItem
                        icon={<Trash2Icon />}
                        label="Delete"
                        onClick={handleShowDeleteDialog}
                        variant="destructive"
                    />
                </DropdownMenuContent>
            </DropdownMenu>

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
