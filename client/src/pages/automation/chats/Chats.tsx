import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ChatsSidebar from '@/pages/automation/chats/components/ChatsSidebar';
import {useChatsStore} from '@/pages/automation/chats/stores/useChatsStore';
import EnvironmentSelect from '@/shared/components/EnvironmentSelect';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {MessageSquareIcon, MessageSquareXIcon} from 'lucide-react';
import {useEffect} from 'react';
import {Outlet, useParams} from 'react-router-dom';

const Chats = () => {
    const {workflowExecutionId} = useParams();

    const currentChatName = useChatsStore((state) => state.currentChatName);
    const isRunning = useChatsStore((state) => state.isRunning);
    const resetAll = useChatsStore((state) => state.resetAll);
    const resetCurrentChat = useChatsStore((state) => state.resetCurrentChat);

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
                            <EnvironmentSelect />

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
                    title={currentChatName}
                />
            }
            leftSidebarBody={<ChatsSidebar />}
            leftSidebarHeader={<Header position="sidebar" title="Chats" />}
            leftSidebarWidth="64"
        >
            {workflowExecutionId ? (
                <Outlet />
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
