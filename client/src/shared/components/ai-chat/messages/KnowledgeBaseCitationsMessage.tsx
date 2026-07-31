import {DataMessagePartProps} from '@assistant-ui/react';
import {BookOpenIcon} from 'lucide-react';
import {Link} from 'react-router-dom';

export interface KnowledgeBaseCitationHitI {
    docId?: string;
    docTitle?: string;
    excerpt?: string;
    knowledgeBaseId?: string;
    knowledgeBaseName?: string;
    score?: number;
}

export interface KnowledgeBaseCitationsDataI {
    hits: KnowledgeBaseCitationHitI[];
    kind: 'knowledge-base-citations';
}

/**
 * Renders a queryKnowledgeBase tool result as source-citation chips under the assistant's answer. The chips come
 * straight from the tool result (not re-emitted by the LLM), so the sources shown are exactly the documents the RAG
 * search retrieved. Hits are deduplicated per document — the search returns one hit per chunk, and several chunks of
 * the same document often match one question. Each chip links to the cited knowledge base's detail page; the chunk
 * excerpt is exposed as the chip's tooltip.
 */
const KnowledgeBaseCitationsMessage = ({data}: DataMessagePartProps<KnowledgeBaseCitationsDataI>) => {
    const hits: KnowledgeBaseCitationHitI[] = data.hits || [];

    const seenDocumentKeys = new Set<string>();
    const uniqueDocumentHits = hits.filter((hit) => {
        const documentKey = hit.docId || hit.docTitle;

        if (!documentKey || seenDocumentKeys.has(documentKey)) {
            return false;
        }

        seenDocumentKeys.add(documentKey);

        return true;
    });

    if (uniqueDocumentHits.length === 0) {
        return null;
    }

    const chipClassName =
        'inline-flex max-w-72 items-center gap-1 rounded-full border border-border bg-muted/40 px-2 py-0.5 text-xs';

    return (
        <div className="mt-2 flex flex-col gap-1">
            <span className="text-xs font-medium text-muted-foreground">Sources</span>

            <ul className="m-0 flex list-none flex-wrap gap-1.5 p-0">
                {uniqueDocumentHits.map((hit) => {
                    const documentKey = hit.docId || hit.docTitle;
                    const label = hit.docTitle || `Document ${hit.docId}`;

                    const chipContent = (
                        <>
                            <BookOpenIcon className="size-3.5 shrink-0" />

                            <span className="truncate">{label}</span>

                            {hit.knowledgeBaseName && (
                                <span className="truncate text-muted-foreground">{hit.knowledgeBaseName}</span>
                            )}
                        </>
                    );

                    return (
                        <li key={documentKey}>
                            {hit.knowledgeBaseId ? (
                                <Link
                                    className={`${chipClassName} hover:bg-muted`}
                                    title={hit.excerpt}
                                    to={`/automation/knowledge-bases/${hit.knowledgeBaseId}`}
                                >
                                    {chipContent}
                                </Link>
                            ) : (
                                <span className={chipClassName} title={hit.excerpt}>
                                    {chipContent}
                                </span>
                            )}
                        </li>
                    );
                })}
            </ul>
        </div>
    );
};

export default KnowledgeBaseCitationsMessage;
