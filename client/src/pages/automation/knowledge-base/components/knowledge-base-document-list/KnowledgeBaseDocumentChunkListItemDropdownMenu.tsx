import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import useKnowledgeBaseDocumentChunkListItemDropdownMenu from '@/pages/automation/knowledge-base/components/knowledge-base-document-list/hooks/useKnowledgeBaseDocumentChunkListItemDropdownMenu';
import {KnowledgeBaseDocumentChunk} from '@/shared/middleware/graphql';
import {EditIcon, EllipsisVerticalIcon, Trash2Icon} from 'lucide-react';

interface KnowledgeBaseDocumentChunkListItemDropdownMenuProps {
    chunk: KnowledgeBaseDocumentChunk;
}

const KnowledgeBaseDocumentChunkListItemDropdownMenu = ({
    chunk,
}: KnowledgeBaseDocumentChunkListItemDropdownMenuProps) => {
    const {handleDelete, handleEdit} = useKnowledgeBaseDocumentChunkListItemDropdownMenu({chunk});

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button aria-label="More Chunk Actions" icon={<EllipsisVerticalIcon />} size="icon" variant="ghost" />
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end" className="p-0">
                <DropdownMenuItem className="dropdown-menu-item" onClick={handleEdit}>
                    <EditIcon /> Edit
                </DropdownMenuItem>

                <DropdownMenuSeparator className="m-0" />

                <DropdownMenuItem className="dropdown-menu-item-destructive" onClick={handleDelete}>
                    <Trash2Icon /> Delete
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default KnowledgeBaseDocumentChunkListItemDropdownMenu;
