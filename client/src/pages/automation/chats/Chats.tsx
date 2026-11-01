import Button from '@/components/Button/Button';
import EmptyState from '@/components/EmptyState/EmptyState';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ChatsSidebar from '@/pages/automation/chats/components/ChatsSidebar';
import {useChatsStore} from '@/pages/automation/chats/stores/useChatsStore';
import {CHATS_DOCUMENTATION_URL} from '@/pages/automation/journey/automationJourneySteps';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useEnvironmentScopeNote} from '@/shared/hooks/useEnvironmentScopeNote';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useWorkspaceChatWorkflowsQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {MessageSquareIcon, MessageSquareXIcon, MessagesSquareIcon} from 'lucide-react';
import {useEffect} from 'react';
import {Outlet, useNavigate, useParams} from 'react-router-dom';

const Chats = () => {
    const {workflowExecutionId} = useParams();

    const currentChatName = useChatsStore((state) => state.currentChatName);
    const isRunning = useChatsStore((state) => state.isRunning);
    const resetAll = useChatsStore((state) => state.resetAll);
    const resetCurrentChat = useChatsStore((state) => state.resetCurrentChat);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const navigate = useNavigate();

    const environmentScopeNote = useEnvironmentScopeNote('Chats');

    // Same query as ChatsSidebar, so it is served from that cache.
    const {data: chatWorkflowsData, isLoading: chatWorkflowsIsLoading} = useWorkspaceChatWorkflowsQuery({
        environmentId: String(currentEnvironmentId),
        workspaceId: String(currentWorkspaceId),
    });

    const isFirstRun =
        !workflowExecutionId && !chatWorkflowsIsLoading && chatWorkflowsData?.workspaceChatWorkflows?.length === 0;

    useEffect(() => {
        return () => {
            resetAll();
        };
    }, [resetAll]);

    return (
        <LayoutContainer
            className="bg-surface-main"
            header={
                <Header
                    centerTitle
                    position="main"
                    right={
                        <div className="flex items-center gap-4">
                            {workflowExecutionId && (
                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        <Button
                                            aria-label="Clear messages"
                                            disabled={isRunning}
                                            icon={<MessageSquareXIcon />}
                                            onClick={resetCurrentChat}
                                            size="icon"
                                            variant="ghost"
                                        />
                                    </TooltipTrigger>

                                    <TooltipContent>Clear messages</TooltipContent>
                                </Tooltip>
                            )}
                        </div>
                    }
                    title={isFirstRun ? 'Chats' : currentChatName}
                />
            }
            leftSidebarBody={<ChatsSidebar />}
            leftSidebarHeader={<Header position="sidebar" title="Chats" />}
            leftSidebarHidden={isFirstRun}
            leftSidebarWidth="64"
        >
            {workflowExecutionId ? (
                <Outlet />
            ) : isFirstRun ? (
                <EmptyState
                    actions={<Button label="Go to Projects" onClick={() => void navigate('/automation/projects')} />}
                    description="Chats let you talk to a deployed workflow, sending messages and files that each start a run. A workflow appears here once it has a Chat trigger in Hosted Chat mode and its deployment and workflow are both enabled."
                    docsLink={{href: CHATS_DOCUMENTATION_URL, label: 'How chats work'}}
                    icon={<MessagesSquareIcon />}
                    note={environmentScopeNote}
                    title="No chat workflows yet"
                />
            ) : (
                <div className="flex flex-1 flex-col items-center justify-center text-center">
                    <MessageSquareIcon className="mx-auto mb-4 size-12 text-muted-foreground" />

                    <h2 className="text-lg font-medium text-foreground">Select a chat</h2>

                    <p className="text-sm text-muted-foreground">Choose a chat from the sidebar to start messaging</p>
                </div>
            )}
        </LayoutContainer>
    );
};

export default Chats;
