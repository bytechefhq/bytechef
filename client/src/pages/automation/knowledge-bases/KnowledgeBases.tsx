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
import StorageUsageBanner from '@/shared/components/StorageUsageBanner';
import CopilotButton from '@/shared/components/copilot/CopilotButton';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import {Source} from '@/shared/components/copilot/stores/useCopilotStore';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useKnowledgeBaseEmbeddingActiveQuery, useKnowledgeBaseStorageUsageQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {DatabaseIcon} from 'lucide-react';
import {useEffect} from 'react';

const KnowledgeBases = () => {
    const currentWorkspaceId = String(useWorkspaceStore((state) => state.currentWorkspaceId));

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {allTags, error, filteredKnowledgeBases, isLoading, knowledgeBases, tagId, tagsByKnowledgeBaseData} =
        useKnowledgeBases();

    const {data: embeddingActiveData} = useKnowledgeBaseEmbeddingActiveQuery({
        environment: currentEnvironmentId,
    });

    const embeddingActive = embeddingActiveData?.knowledgeBaseEmbeddingActive ?? true;

    const {data: storageUsageData} = useKnowledgeBaseStorageUsageQuery();

    const storageUsage = storageUsageData?.knowledgeBaseStorageUsage;

    const registerPostTurn = useCopilotPostTurnRegistry((state) => state.register);

    const queryClient = useQueryClient();

    // Refresh the list and the tag sidebar after a BUILD-mode copilot turn creates or retags a knowledge base.
    useEffect(() => {
        return registerPostTurn(Source.KNOWLEDGE_BASE, () => {
            queryClient.invalidateQueries({queryKey: ['knowledgeBases']});
            queryClient.invalidateQueries({queryKey: ['knowledgeBaseTags']});
            queryClient.invalidateQueries({queryKey: ['knowledgeBaseTagsByKnowledgeBase']});
            queryClient.invalidateQueries({queryKey: ['KnowledgeBaseStorageUsage']});
        });
    }, [queryClient, registerPostTurn]);

    return (
        <LayoutContainer
            header={
                <Header
                    centerTitle={true}
                    position="main"
                    right={
                        (knowledgeBases.length > 0 || !isLoading) && (
                            <div className="flex items-center gap-1">
                                <EnvironmentSelect />

                                <CopilotButton source={Source.KNOWLEDGE_BASE} />

                                {knowledgeBases.length > 0 && (
                                    // This is the "Create knowledge base" command's target.
                                    <CreateKnowledgeBaseDialog
                                        claimsCreateIntent={true}
                                        trigger={<Button>New Knowledge Base</Button>}
                                        workspaceId={currentWorkspaceId}
                                    />
                                )}
                            </div>
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
                    {storageUsage && (
                        <StorageUsageBanner
                            label="Knowledge base"
                            limitBytes={storageUsage.limitBytes}
                            percentage={storageUsage.percentage}
                            unlimited={storageUsage.unlimited}
                            usedBytes={storageUsage.usedBytes}
                        />
                    )}

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
                                    // This is the "Create knowledge base" command's target.
                                    <CreateKnowledgeBaseDialog
                                        claimsCreateIntent={true}
                                        trigger={<Button>Create Knowledge Base</Button>}
                                        workspaceId={currentWorkspaceId}
                                    />
                                }
                                icon={<DatabaseIcon className="size-24 text-stroke-neutral-tertiary" />}
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
