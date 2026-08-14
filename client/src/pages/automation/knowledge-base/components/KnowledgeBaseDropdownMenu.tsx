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
import {KnowledgeBase} from '@/shared/middleware/graphql';
import {EditIcon, EllipsisVerticalIcon, Trash2Icon} from 'lucide-react';

interface KnowledgeBaseDropdownMenuProps {
    /** Extra classes for the trigger. The sidebar passes the hover-reveal the list rows do not want. */
    className?: string;
    knowledgeBase: KnowledgeBase;
}

const KnowledgeBaseDropdownMenu = ({className, knowledgeBase}: KnowledgeBaseDropdownMenuProps) => {
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
                        className={className}
                        icon={<EllipsisVerticalIcon />}
                        size="icon"
                        variant="ghost"
                    />
                </DropdownMenuTrigger>

                <DropdownMenuContent align="end" className="p-0">
                    <DropdownMenuItem onClick={handleShowEditDialog}>
                        <EditIcon /> Edit
                    </DropdownMenuItem>

                    <DropdownMenuSeparator />

                    <DropdownMenuItem onClick={handleShowDeleteDialog} variant="destructive">
                        <Trash2Icon /> Delete
                    </DropdownMenuItem>
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
