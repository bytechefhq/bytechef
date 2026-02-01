import PageLoader from '@/components/PageLoader';
import KnowledgeBaseHeader from '@/pages/automation/knowledge-base/components/KnowledgeBaseHeader';
import KnowledgeBaseInfoCard from '@/pages/automation/knowledge-base/components/KnowledgeBaseInfoCard';
import KnowledgeBaseLeftSidebarNav from '@/pages/automation/knowledge-base/components/KnowledgeBaseLeftSidebarNav';
import KnowledgeBaseTabs from '@/pages/automation/knowledge-base/components/KnowledgeBaseTabs';
import useKnowledgeBase from '@/pages/automation/knowledge-base/hooks/useKnowledgeBase';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';

const KnowledgeBase = () => {
    const {documents, error, handleBackClick, isLoading, knowledgeBase, knowledgeBaseId} = useKnowledgeBase();

    return (
        <LayoutContainer
            header={<KnowledgeBaseHeader knowledgeBaseName={knowledgeBase?.name} onBackClick={handleBackClick} />}
            leftSidebarBody={<KnowledgeBaseLeftSidebarNav />}
            leftSidebarHeader={<Header position="sidebar" title="Knowledge Base" />}
            leftSidebarWidth="64"
        >
            <PageLoader errors={[error]} loading={isLoading}>
                {knowledgeBase && (
                    <div className="mx-auto flex h-full w-full max-w-screen-lg flex-col px-4 py-4">
                        <KnowledgeBaseInfoCard knowledgeBase={knowledgeBase} />

                        <KnowledgeBaseTabs documents={documents} knowledgeBaseId={knowledgeBaseId} />
                    </div>
                )}
            </PageLoader>
        </LayoutContainer>
    );
};

export default KnowledgeBase;
