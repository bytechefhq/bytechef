import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import useKnowledgeBaseDocumentChunkListItemDropdownMenu from '@/pages/automation/knowledge-base/components/knowledge-base-document-list/hooks/useKnowledgeBaseDocumentChunkListItemDropdownMenu';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';
import {KnowledgeBaseDocumentChunk} from '@/shared/middleware/graphql';
import {EditIcon, EllipsisVerticalIcon, Trash2Icon} from 'lucide-react';

interface KnowledgeBaseDocumentChunkListItemDropdownMenuProps {
    chunk: KnowledgeBaseDocumentChunk;
}

const KnowledgeBaseDocumentChunkListItemDropdownMenu = ({
    chunk,
}: KnowledgeBaseDocumentChunkListItemDropdownMenuProps) => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {handleDelete, handleEdit} = useKnowledgeBaseDocumentChunkListItemDropdownMenu({chunk});

    const canEditKnowledgeBase = useHasWorkspaceScope(currentWorkspaceId, 'KNOWLEDGE_BASE_EDIT');

    if (!canEditKnowledgeBase) {
        return null;
    }

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button aria-label="More Chunk Actions" icon={<EllipsisVerticalIcon />} size="icon" variant="ghost" />
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end">
                <DropdownMenuItem icon={<EditIcon />} label="Edit" onClick={handleEdit} />

                <DropdownMenuSeparator />

                <DropdownMenuItem icon={<Trash2Icon />} label="Delete" onClick={handleDelete} variant="destructive" />
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default KnowledgeBaseDocumentChunkListItemDropdownMenu;
