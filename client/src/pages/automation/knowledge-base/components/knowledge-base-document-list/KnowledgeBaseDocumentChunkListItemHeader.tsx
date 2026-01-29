import {Checkbox} from '@/components/ui/checkbox';
import useKnowledgeBaseDocumentChunkListItemHeader from '@/pages/automation/knowledge-base/components/knowledge-base-document-list/hooks/useKnowledgeBaseDocumentChunkListItemHeader';

interface KnowledgeBaseDocumentChunkListItemHeaderProps {
    chunkId: string;
    chunkIndex: number;
    documentName: string;
}

const KnowledgeBaseDocumentChunkListItemHeader = ({
    chunkId,
    chunkIndex,
    documentName,
}: KnowledgeBaseDocumentChunkListItemHeaderProps) => {
    const {handleSelectionChange, isSelected} = useKnowledgeBaseDocumentChunkListItemHeader({chunkId});

    return (
        <div className="flex items-center space-x-3">
            <Checkbox checked={isSelected} onCheckedChange={handleSelectionChange} />

            <div className="flex items-center space-x-2 text-sm text-gray-500">
                <span className="font-medium">{documentName}</span>

                <span>•</span>

                <span>Chunk {chunkIndex + 1}</span>
            </div>
        </div>
    );
};

export default KnowledgeBaseDocumentChunkListItemHeader;
