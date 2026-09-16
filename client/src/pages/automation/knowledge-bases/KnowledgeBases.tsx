import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import CreateKnowledgeBaseDialog from '@/pages/automation/knowledge-bases/components/CreateKnowledgeBaseDialog';
import KnowledgeBaseEmbeddingInactiveAlert from '@/pages/automation/knowledge-bases/components/KnowledgeBaseEmbeddingInactiveAlert';
import KnowledgeBasesFilterTitle from '@/pages/automation/knowledge-bases/components/KnowledgeBasesFilterTitle';
import KnowledgeBasesLeftSidebarNav from '@/pages/automation/knowledge-bases/components/KnowledgeBasesLeftSidebarNav';
import useKnowledgeBaseEmbeddingActive from '@/pages/automation/knowledge-bases/components/hooks/useKnowledgeBaseEmbeddingActive';
import useKnowledgeBases from '@/pages/automation/knowledge-bases/components/hooks/useKnowledgeBases';
import KnowledgeBaseList from '@/pages/automation/knowledge-bases/components/knowledge-base-list/KnowledgeBaseList';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {DatabaseIcon} from 'lucide-react';

const KnowledgeBases = () => {
    const currentWorkspaceId = String(useWorkspaceStore((state) => state.currentWorkspaceId));

    const {allTags, error, filteredKnowledgeBases, isLoading, knowledgeBases, tagId, tagsByKnowledgeBaseData} =
        useKnowledgeBases();

    const {embeddingActive, isLoading: embeddingActiveLoading} = useKnowledgeBaseEmbeddingActive();

    const showKnowledgeBases = embeddingActive && knowledgeBases.length > 0;

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
                            ''
                        )
                    }
                />
            }
            leftSidebarBody={embeddingActive && <KnowledgeBasesLeftSidebarNav />}
            leftSidebarHeader={<Header position="sidebar" title="Knowledge Bases" />}
            leftSidebarWidth="64"
        >
            <PageLoader errors={[error]} loading={isLoading || embeddingActiveLoading}>
                {embeddingActive ? (
                    <div className="flex size-full">
                        {filteredKnowledgeBases.length > 0 ? (
                            <KnowledgeBaseList
                                allTags={allTags}
                                knowledgeBases={filteredKnowledgeBases}
                                tagsByKnowledgeBaseData={tagsByKnowledgeBaseData}
                            />
                        ) : (
                            <EmptyList
                                button={
                                    <CreateKnowledgeBaseDialog
                                        trigger={<Button>Create Knowledge Base</Button>}
                                        workspaceId={currentWorkspaceId}
                                    />
                                }
                                icon={<DatabaseIcon className="size-24 text-gray-300" />}
                                message={
                                    tagId
                                        ? 'No knowledge bases match the selected tag.'
                                        : 'Get started by creating a new knowledge base.'
                                }
                                title={tagId ? 'No Matching Knowledge Bases' : 'No Knowledge Bases'}
                            />
                        )}
                    </div>
                ) : (
                    <KnowledgeBaseEmbeddingInactiveAlert />
                )}
            </PageLoader>
        </LayoutContainer>
    );
};

export default KnowledgeBases;
