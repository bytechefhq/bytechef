import Button from '@/components/Button/Button';
import PageLoader from '@/components/PageLoader';
import KnowledgeBaseDropdownMenu from '@/pages/automation/knowledge-base/components/KnowledgeBaseDropdownMenu';
import KnowledgeBaseHeader from '@/pages/automation/knowledge-base/components/KnowledgeBaseHeader';
import KnowledgeBaseInfoCard from '@/pages/automation/knowledge-base/components/KnowledgeBaseInfoCard';
import KnowledgeBaseLeftSidebarNav from '@/pages/automation/knowledge-base/components/KnowledgeBaseLeftSidebarNav';
import KnowledgeBaseTabs from '@/pages/automation/knowledge-base/components/KnowledgeBaseTabs';
import useKnowledgeBase from '@/pages/automation/knowledge-base/hooks/useKnowledgeBase';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import CreateKnowledgeBaseDialog from '@/shared/components/knowledge-bases/components/CreateKnowledgeBaseDialog';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useQueryClient} from '@tanstack/react-query';
import {PlusIcon, SparklesIcon} from 'lucide-react';
import {useEffect} from 'react';

const KnowledgeBase = () => {
    const {documents, error, isLoading, knowledgeBase, knowledgeBaseId} = useKnowledgeBase();

    const copilotEnabled = useApplicationInfoStore((state) => state.ai.copilot.enabled);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const workspaceId = currentWorkspaceId == null ? undefined : String(currentWorkspaceId);

    const setContext = useCopilotStore((state) => state.setContext);
    const setCopilotPanelOpen = useCopilotPanelStore((state) => state.setCopilotPanelOpen);
    const registerPostTurn = useCopilotPostTurnRegistry((state) => state.register);

    const queryClient = useQueryClient();

    const openCopilot = () => {
        setContext({
            mode: MODE.ASK,
            parameters: {knowledgeBaseId},
            source: Source.KNOWLEDGE_BASE,
        });

        setCopilotPanelOpen(true);
    };

    // Refresh this knowledge base's data + the knowledge base list after a BUILD-mode copilot turn mutates data
    // (e.g. uploading a document), so the page reflects the change without a manual reload.
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
                        <div className="flex items-center gap-1">
                            {copilotEnabled && (
                                <Button
                                    aria-label="Ask Copilot"
                                    icon={<SparklesIcon />}
                                    onClick={openCopilot}
                                    size="icon"
                                    variant="ghost"
                                />
                            )}

                            {knowledgeBase && <KnowledgeBaseDropdownMenu knowledgeBase={knowledgeBase} />}
                        </div>
                    }
                />
            }
            leftSidebarBody={<KnowledgeBaseLeftSidebarNav />}
            leftSidebarHeader={
                // The + beside the sidebar title, matching the data tables, skills and agent detail
                // sidebars — creating one from here needs no trip back to the list.
                <Header
                    position="sidebar"
                    right={
                        workspaceId ? (
                            <CreateKnowledgeBaseDialog
                                trigger={
                                    <Button
                                        aria-label="New knowledge base"
                                        icon={<PlusIcon />}
                                        size="icon"
                                        variant="ghost"
                                    />
                                }
                                workspaceId={workspaceId}
                            />
                        ) : undefined
                    }
                    title="Knowledge Base"
                />
            }
            leftSidebarWidth="64"
        >
            <PageLoader errors={[error]} loading={isLoading}>
                {knowledgeBase && (
                    <div className="mx-auto flex h-full w-full max-w-(--breakpoint-lg) flex-col px-4 py-4">
                        <KnowledgeBaseInfoCard knowledgeBase={knowledgeBase} showDropdownMenu={false} />

                        <KnowledgeBaseTabs documents={documents} knowledgeBaseId={knowledgeBaseId} />
                    </div>
                )}
            </PageLoader>
        </LayoutContainer>
    );
};

export default KnowledgeBase;
