import KnowledgeBaseDropdownMenu from '@/pages/automation/knowledge-base/components/KnowledgeBaseDropdownMenu';
import {KnowledgeBase, KnowledgeBaseDocument} from '@/shared/middleware/graphql';
import {DatabaseIcon} from 'lucide-react';

interface KnowledgeBaseInfoCardProps {
    knowledgeBase: KnowledgeBase & {documents: KnowledgeBaseDocument[]};
    showDropdownMenu?: boolean;
}

// The full page hosts the actions menu in its header next to the copilot button, so it opts out here. The embeddable
// variant renders without page chrome, which leaves this card as its only place for edit/delete.
const KnowledgeBaseInfoCard = ({knowledgeBase, showDropdownMenu = true}: KnowledgeBaseInfoCardProps) => {
    return (
        <div className="mb-4 rounded-lg border border-border/50 bg-surface-neutral-primary p-4">
            <div className="flex items-center justify-between">
                <div className="flex items-center space-x-2">
                    <DatabaseIcon className="size-5 text-content-neutral-tertiary" />

                    <h2 className="text-lg font-semibold">{knowledgeBase.name}</h2>
                </div>

                {showDropdownMenu && <KnowledgeBaseDropdownMenu knowledgeBase={knowledgeBase} />}
            </div>

            {knowledgeBase.description && (
                <p className="mt-2 text-sm text-content-neutral-secondary">{knowledgeBase.description}</p>
            )}

            <div className="mt-4 flex space-x-6 text-sm text-content-neutral-secondary">
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
