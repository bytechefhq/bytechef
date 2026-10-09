import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
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

            <DropdownMenuContent align="end">
                <DropdownMenuItem icon={<EditIcon />} label="Edit" onClick={handleEdit} />

                <DropdownMenuSeparator />

                <DropdownMenuItem icon={<Trash2Icon />} label="Delete" onClick={handleDelete} variant="destructive" />
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default KnowledgeBaseDocumentChunkListItemDropdownMenu;
