import KnowledgeBaseDropdownMenu from '@/pages/automation/knowledge-base/components/KnowledgeBaseDropdownMenu';
import {KnowledgeBase, KnowledgeBaseDocument} from '@/shared/middleware/graphql';
import {Database} from 'lucide-react';

interface KnowledgeBaseInfoCardProps {
    knowledgeBase: KnowledgeBase & {documents: KnowledgeBaseDocument[]};
}

const KnowledgeBaseInfoCard = ({knowledgeBase}: KnowledgeBaseInfoCardProps) => {
    return (
        <div className="mb-4 rounded-lg border border-gray-200 bg-white p-4">
            <div className="flex items-center justify-between">
                <div className="flex items-center space-x-2">
                    <Database className="size-5 text-gray-400" />

                    <h2 className="text-lg font-semibold">{knowledgeBase.name}</h2>
                </div>

                <KnowledgeBaseDropdownMenu knowledgeBase={knowledgeBase} />
            </div>

            {knowledgeBase.description && <p className="mt-2 text-sm text-gray-500">{knowledgeBase.description}</p>}

            <div className="mt-4 flex space-x-6 text-sm text-gray-500">
                <div>
                    <span className="font-medium">Documents: </span>

                    <span>{knowledgeBase.documents?.length || 0}</span>
                </div>

                <div>
                    <span className="font-medium">Chunk Size: </span>

                    <span>
                        {knowledgeBase.minChunkSizeChars}-{knowledgeBase.maxChunkSize}
                    </span>
                </div>

                <div>
                    <span className="font-medium">Overlap: </span>

                    <span>{knowledgeBase.overlap}</span>
                </div>
            </div>
        </div>
    );
};

export default KnowledgeBaseInfoCard;
