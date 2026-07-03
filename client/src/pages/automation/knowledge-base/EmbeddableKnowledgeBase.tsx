import PageLoader from '@/components/PageLoader';
import KnowledgeBaseInfoCard from '@/pages/automation/knowledge-base/components/KnowledgeBaseInfoCard';
import KnowledgeBaseTabs from '@/pages/automation/knowledge-base/components/KnowledgeBaseTabs';
import {useKnowledgeBaseQuery} from '@/shared/middleware/graphql';

interface EmbeddableKnowledgeBasePropsI {
    knowledgeBaseId: string;
}

/**
 * A self-contained knowledge-base viewer suitable for embedding inside panels
 * such as the AI Hub resource panel. Renders the full document list,
 * search interface, and all management dialogs but without page-level chrome
 * (no LayoutContainer, no left sidebar, no page header).
 *
 * Differences from the full KnowledgeBase.tsx page:
 * - No left sidebar listing all knowledge bases
 * - No page header with back button
 * - No workspace-level navigation chrome
 */
const EmbeddableKnowledgeBase = ({knowledgeBaseId}: EmbeddableKnowledgeBasePropsI) => {
    const {data, error, isLoading} = useKnowledgeBaseQuery(
        {id: knowledgeBaseId},
        {
            enabled: !!knowledgeBaseId,
        }
    );

    const rawKnowledgeBase = data?.knowledgeBase ?? undefined;
    const documents = (rawKnowledgeBase?.documents ?? []).filter(
        (document): document is NonNullable<typeof document> => document !== null
    );
    const knowledgeBase = rawKnowledgeBase ? {...rawKnowledgeBase, documents} : undefined;

    return (
        <div className="flex size-full flex-col overflow-auto">
            <PageLoader errors={[error]} loading={isLoading}>
                {knowledgeBase && (
                    <div className="mx-auto flex h-full w-full max-w-screen-lg flex-col px-4 py-4">
                        <KnowledgeBaseInfoCard knowledgeBase={knowledgeBase} />

                        <KnowledgeBaseTabs documents={documents} knowledgeBaseId={knowledgeBaseId} />
                    </div>
                )}
            </PageLoader>
        </div>
    );
};

export default EmbeddableKnowledgeBase;
