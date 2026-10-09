import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import useKnowledgeBaseDocumentListItemDropdownMenu from '@/pages/automation/knowledge-base/components/knowledge-base-document-list/hooks/useKnowledgeBaseDocumentListItemDropdownMenu';
import {EllipsisVerticalIcon, Trash2Icon} from 'lucide-react';

interface KnowledgeBaseDocumentListItemDropdownMenuProps {
    documentId: string;
}

const KnowledgeBaseDocumentListItemDropdownMenu = ({documentId}: KnowledgeBaseDocumentListItemDropdownMenuProps) => {
    const {handleDelete} = useKnowledgeBaseDocumentListItemDropdownMenu({documentId});

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild onClick={(event) => event.stopPropagation()}>
                <Button
                    aria-label="More Document Actions"
                    icon={<EllipsisVerticalIcon />}
                    size="icon"
                    variant="ghost"
                />
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end">
                <DropdownMenuSeparator />

                <DropdownMenuItem
                    icon={<Trash2Icon />}
                    label="Delete"
                    onClick={(event) => {
                        event.stopPropagation();

                        handleDelete();
                    }}
                    variant="destructive"
                />
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default KnowledgeBaseDocumentListItemDropdownMenu;
