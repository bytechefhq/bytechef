import {Thread} from '@/components/assistant-ui/thread';
import {Button} from '@/components/ui/button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {CopilotRuntimeProvider} from '@/shared/components/copilot/runtime-providers/CopilotRuntimeProvider';
import {useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {BotMessageSquareIcon, MessageSquareOffIcon, XIcon} from 'lucide-react';
import {useEffect} from 'react';
import {useShallow} from 'zustand/react/shallow';

const CopilotPanel = () => {
    const {context, generateConversationId, resetMessages, setContext, setCopilotPanelOpen} = useCopilotStore(
        useShallow((state) => ({
            context: state.context,
            generateConversationId: state.generateConversationId,
            resetMessages: state.resetMessages,
            setContext: state.setContext,
            setCopilotPanelOpen: state.setCopilotPanelOpen,
        }))
    );

    const handleCleanMessages = () => {
        resetMessages();
        generateConversationId();
    };

    const handleCloseClick = () => {
        setContext(undefined);
        setCopilotPanelOpen(false);
    };

    useEffect(() => {
        generateConversationId();
        resetMessages();

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    useEffect(() => {
        generateConversationId();
        resetMessages();

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [context?.source]);

    return (
        <div className="relative h-full min-h-[50vh] w-[450px] bg-surface-main">
            <div className="flex items-center justify-between p-3">
                <div className="flex items-center space-x-1">
                    <BotMessageSquareIcon className="size-6" /> <h4>AI Copilot</h4>
                </div>

                <div className="flex">
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <Button onClick={handleCleanMessages} size="icon" variant="ghost">
                                <MessageSquareOffIcon className="size-4" />
                            </Button>
                        </TooltipTrigger>

                        <TooltipContent>Clean messages</TooltipContent>
                    </Tooltip>

                    <Button onClick={handleCloseClick} size="icon" variant="ghost">
                        <XIcon />
                    </Button>
                </div>
            </div>

            <div className="absolute inset-x-0 bottom-0 top-16 -mx-1">
                <CopilotRuntimeProvider>
                    <Thread />
                </CopilotRuntimeProvider>
            </div>
        </div>
    );
};

export default CopilotPanel;
