import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import CreateKnowledgeBaseDialog from '@/pages/automation/knowledge-bases/components/CreateKnowledgeBaseDialog';
import KnowledgeBaseEmbeddingInactiveEmptyState from '@/pages/automation/knowledge-bases/components/KnowledgeBaseEmbeddingInactiveEmptyState';
import KnowledgeBasesEmptyState from '@/pages/automation/knowledge-bases/components/KnowledgeBasesEmptyState';
import KnowledgeBasesFilterTitle from '@/pages/automation/knowledge-bases/components/KnowledgeBasesFilterTitle';
import KnowledgeBasesLeftSidebarNav from '@/pages/automation/knowledge-bases/components/KnowledgeBasesLeftSidebarNav';
import useKnowledgeBaseEmbeddingActive from '@/pages/automation/knowledge-bases/components/hooks/useKnowledgeBaseEmbeddingActive';
import useKnowledgeBases from '@/pages/automation/knowledge-bases/components/hooks/useKnowledgeBases';
import KnowledgeBaseList from '@/pages/automation/knowledge-bases/components/knowledge-base-list/KnowledgeBaseList';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useInitialLoadComplete} from '@/shared/hooks/useInitialLoadComplete';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {DatabaseIcon} from 'lucide-react';

const KnowledgeBases = () => {
    const currentWorkspaceId = String(useWorkspaceStore((state) => state.currentWorkspaceId));

    const {allTags, error, filteredKnowledgeBases, isLoading, knowledgeBases, tagId, tagsByKnowledgeBaseData} =
        useKnowledgeBases();

    const {embeddingActive, isLoading: embeddingActiveLoading} = useKnowledgeBaseEmbeddingActive();

    const initialLoadComplete = useInitialLoadComplete(isLoading || embeddingActiveLoading);

    const showKnowledgeBases = embeddingActive && knowledgeBases.length > 0;

    const isLoaded = !isLoading && !embeddingActiveLoading;

    const isFirstRun = isLoaded && !tagId && knowledgeBases.length === 0;

    const leftSidebarHidden = isFirstRun || (isLoaded && !embeddingActive);

    return (
        <LayoutContainer
            header={
                <Header
                    centerTitle={true}
                    position="main"
                    right={
                        showKnowledgeBases && (
                            <CreateKnowledgeBaseDialog
                                trigger={<Button>New Knowledge Base</Button>}
                                workspaceId={currentWorkspaceId}
                            />
                        )
                    }
                    title={
                        showKnowledgeBases ? (
                            <KnowledgeBasesFilterTitle
                                allTags={allTags}
                                tagsByKnowledgeBaseData={tagsByKnowledgeBaseData}
                            />
                        ) : (
                            leftSidebarHidden && 'Knowledge Bases'
                        )
                    }
                />
            }
            leftSidebarBody={embeddingActive && <KnowledgeBasesLeftSidebarNav />}
            leftSidebarHeader={<Header position="sidebar" title="Knowledge Bases" />}
            leftSidebarHidden={!initialLoadComplete || leftSidebarHidden}
            leftSidebarWidth="64"
        >
            <PageLoader errors={[error]} loading={isLoading || embeddingActiveLoading}>
                {isFirstRun ? (
                    <KnowledgeBasesEmptyState embeddingActive={embeddingActive} workspaceId={currentWorkspaceId} />
                ) : !embeddingActive ? (
                    <KnowledgeBaseEmbeddingInactiveEmptyState />
                ) : (
                    <div className="flex size-full">
                        {filteredKnowledgeBases.length > 0 ? (
                            <KnowledgeBaseList
                                allTags={allTags}
                                knowledgeBases={filteredKnowledgeBases}
                                tagsByKnowledgeBaseData={tagsByKnowledgeBaseData}
                            />
                        ) : (
                            <EmptyList
                                icon={<DatabaseIcon className="size-24 text-stroke-neutral-tertiary" />}
                                message="No knowledge bases match the selected tag."
                                title="No Matching Knowledge Bases"
                            />
                        )}
                    </div>
                )}
            </PageLoader>
        </LayoutContainer>
    );
};

export default KnowledgeBases;
