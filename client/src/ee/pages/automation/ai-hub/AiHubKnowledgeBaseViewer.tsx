import EmbeddableKnowledgeBase from '@/pages/automation/knowledge-base/EmbeddableKnowledgeBase';
import {ExternalLinkIcon} from 'lucide-react';
import {Link} from 'react-router-dom';

interface AiHubKnowledgeBaseViewerPropsI {
    knowledgeBaseId: string;
    name: string;
}

const AiHubKnowledgeBaseViewer = ({knowledgeBaseId, name}: AiHubKnowledgeBaseViewerPropsI) => {
    const fullViewHref = `/automation/knowledge-bases/${knowledgeBaseId}`;

    return (
        <div className="flex size-full flex-col">
            <header className="flex shrink-0 items-center justify-between gap-2 border-b border-stroke-neutral-secondary px-4 py-3">
                <span className="truncate text-sm font-semibold text-content-neutral-primary">{name}</span>

                <Link
                    className="flex shrink-0 items-center gap-1 text-xs text-content-brand-primary hover:underline"
                    rel="noreferrer"
                    target="_blank"
                    to={fullViewHref}
                >
                    Open in full view
                    <ExternalLinkIcon className="size-3" />
                </Link>
            </header>

            <div className="min-h-0 flex-1">
                <EmbeddableKnowledgeBase knowledgeBaseId={knowledgeBaseId} />
            </div>
        </div>
    );
};

export default AiHubKnowledgeBaseViewer;
