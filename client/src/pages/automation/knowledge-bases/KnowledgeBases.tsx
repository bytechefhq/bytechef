import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Alert, AlertDescription, AlertTitle} from '@/components/ui/alert';
import CreateKnowledgeBaseDialog from '@/pages/automation/knowledge-bases/components/CreateKnowledgeBaseDialog';
import KnowledgeBasesFilterTitle from '@/pages/automation/knowledge-bases/components/KnowledgeBasesFilterTitle';
import KnowledgeBasesLeftSidebarNav from '@/pages/automation/knowledge-bases/components/KnowledgeBasesLeftSidebarNav';
import useKnowledgeBases from '@/pages/automation/knowledge-bases/components/hooks/useKnowledgeBases';
import KnowledgeBaseList from '@/pages/automation/knowledge-bases/components/knowledge-base-list/KnowledgeBaseList';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import EnvironmentSelect from '@/shared/components/EnvironmentSelect';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useKnowledgeBaseEmbeddingActiveQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {DatabaseIcon} from 'lucide-react';

const KnowledgeBases = () => {
    const currentWorkspaceId = String(useWorkspaceStore((state) => state.currentWorkspaceId));

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {allTags, error, filteredKnowledgeBases, isLoading, knowledgeBases, tagId, tagsByKnowledgeBaseData} =
        useKnowledgeBases();

    const {data: embeddingActiveData} = useKnowledgeBaseEmbeddingActiveQuery({
        environment: currentEnvironmentId,
    });

    const embeddingActive = embeddingActiveData?.knowledgeBaseEmbeddingActive ?? true;

    return (
        <LayoutContainer
            header={
                <Header
                    centerTitle={true}
                    position="main"
                    right={
                        knowledgeBases.length > 0 ? (
                            <div className="flex items-center gap-4">
                                <EnvironmentSelect />

                                <CreateKnowledgeBaseDialog
                                    trigger={<Button>New Knowledge Base</Button>}
                                    workspaceId={currentWorkspaceId}
                                />
                            </div>
                        ) : (
                            !isLoading && <EnvironmentSelect />
                        )
                    }
                    title={
                        knowledgeBases.length > 0 ? (
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
            leftSidebarBody={<KnowledgeBasesLeftSidebarNav />}
            leftSidebarHeader={<Header position="sidebar" title="Knowledge Bases" />}
            leftSidebarWidth="64"
        >
            <PageLoader errors={[error]} loading={isLoading}>
                <div className="flex size-full flex-col">
                    {!embeddingActive && (
                        <Alert className="m-4 mb-0 w-auto" variant="destructive">
                            <AlertTitle>No embedding model is active</AlertTitle>

                            <AlertDescription className="flex flex-col gap-1">
                                <span>
                                    Knowledge Base documents can&apos;t be processed until an embedding-capable AI
                                    provider is activated for this environment.
                                </span>

                                <a className="font-medium underline" href="/automation/settings/ai-providers">
                                    Go to AI Providers
                                </a>
                            </AlertDescription>
                        </Alert>
                    )}

                    <div className="flex flex-1">
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
                </div>
            </PageLoader>
        </LayoutContainer>
    );
};

export default KnowledgeBases;
