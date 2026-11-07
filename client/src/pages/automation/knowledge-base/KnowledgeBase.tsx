import PageLoader from '@/components/PageLoader';
import KnowledgeBaseHeader from '@/pages/automation/knowledge-base/components/KnowledgeBaseHeader';
import KnowledgeBaseInfoCard from '@/pages/automation/knowledge-base/components/KnowledgeBaseInfoCard';
import KnowledgeBaseLeftSidebarNav from '@/pages/automation/knowledge-base/components/KnowledgeBaseLeftSidebarNav';
import KnowledgeBaseTabs from '@/pages/automation/knowledge-base/components/KnowledgeBaseTabs';
import useKnowledgeBase from '@/pages/automation/knowledge-base/hooks/useKnowledgeBase';
import KnowledgeBaseEmbeddingInactiveEmptyState from '@/pages/automation/knowledge-bases/components/KnowledgeBaseEmbeddingInactiveEmptyState';
import useKnowledgeBaseEmbeddingActive from '@/pages/automation/knowledge-bases/components/hooks/useKnowledgeBaseEmbeddingActive';
import CopilotButton from '@/shared/components/copilot/CopilotButton';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import {Source} from '@/shared/components/copilot/stores/useCopilotStore';
import {useInitialLoadComplete} from '@/shared/hooks/useInitialLoadComplete';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect} from 'react';

const KnowledgeBase = () => {
    const registerPostTurn = useCopilotPostTurnRegistry((state) => state.register);

    const {documents, error, isLoading, knowledgeBase, knowledgeBaseId} = useKnowledgeBase();

    const {embeddingActive, isLoading: embeddingActiveLoading} = useKnowledgeBaseEmbeddingActive();

    const queryClient = useQueryClient();

    const initialLoadComplete = useInitialLoadComplete(embeddingActiveLoading);

    useEffect(() => {
        return registerPostTurn(Source.KNOWLEDGE_BASE, () => {
            queryClient.invalidateQueries({queryKey: ['knowledgeBase']});
            queryClient.invalidateQueries({queryKey: ['knowledgeBases']});
        });
    }, [queryClient, registerPostTurn]);

    return (
        <LayoutContainer
            header={
                <KnowledgeBaseHeader
                    knowledgeBaseName={knowledgeBase?.name}
                    right={
                        embeddingActive && (
                            <CopilotButton parameters={{knowledgeBaseId}} source={Source.KNOWLEDGE_BASE} />
                        )
                    }
                />
            }
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
                        <div className="flex h-full w-full flex-col self-start p-4 pt-0 3xl:mx-auto 3xl:w-4/5">
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
