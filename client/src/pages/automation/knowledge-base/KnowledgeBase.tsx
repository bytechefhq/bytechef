import PageLoader from '@/components/PageLoader';
import KnowledgeBaseHeader from '@/pages/automation/knowledge-base/components/KnowledgeBaseHeader';
import KnowledgeBaseInfoCard from '@/pages/automation/knowledge-base/components/KnowledgeBaseInfoCard';
import KnowledgeBaseLeftSidebarNav from '@/pages/automation/knowledge-base/components/KnowledgeBaseLeftSidebarNav';
import KnowledgeBaseTabs from '@/pages/automation/knowledge-base/components/KnowledgeBaseTabs';
import useKnowledgeBase from '@/pages/automation/knowledge-base/hooks/useKnowledgeBase';
import KnowledgeBaseEmbeddingInactiveEmptyState from '@/pages/automation/knowledge-bases/components/KnowledgeBaseEmbeddingInactiveEmptyState';
import useKnowledgeBaseEmbeddingActive from '@/pages/automation/knowledge-bases/components/hooks/useKnowledgeBaseEmbeddingActive';
import {useInitialLoadComplete} from '@/shared/hooks/useInitialLoadComplete';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';

const KnowledgeBase = () => {
    const {documents, error, isLoading, knowledgeBase, knowledgeBaseId} = useKnowledgeBase();

    const {embeddingActive, isLoading: embeddingActiveLoading} = useKnowledgeBaseEmbeddingActive();

    const initialLoadComplete = useInitialLoadComplete(embeddingActiveLoading);

    return (
        <LayoutContainer
            header={<KnowledgeBaseHeader knowledgeBaseName={knowledgeBase?.name} />}
            leftSidebarBody={embeddingActive && <KnowledgeBaseLeftSidebarNav />}
            leftSidebarHeader={<Header position="sidebar" title="Knowledge Base" />}
            leftSidebarHidden={!initialLoadComplete || !embeddingActive}
            leftSidebarWidth="64"
        >
            <PageLoader errors={[error]} loading={isLoading || embeddingActiveLoading}>
                {!embeddingActive ? (
                    <KnowledgeBaseEmbeddingInactiveEmptyState />
                ) : (
                    knowledgeBase && (
                        <div className="mx-auto flex h-full w-full max-w-(--breakpoint-lg) flex-col p-4 pt-0">
                            <KnowledgeBaseInfoCard knowledgeBase={knowledgeBase} />

                            <KnowledgeBaseTabs documents={documents} knowledgeBaseId={knowledgeBaseId} />
                        </div>
                    )
                )}
            </PageLoader>
        </LayoutContainer>
    );
};

export default KnowledgeBase;
