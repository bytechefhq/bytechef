import {Button} from '@/components/ui/button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {CopilotRuntimeProvider} from '@/shared/components/copilot/CopilotRuntimeProvider';
import {useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {Thread} from '@assistant-ui/react';
import {Cross2Icon} from '@radix-ui/react-icons';
import {BotMessageSquareIcon, MessageSquareOffIcon} from 'lucide-react';
import {useEffect} from 'react';

const CopilotPanel = () => {
    const {generateConversationId, resetMessages, setCopilotPanelOpen} = useCopilotStore();

    const handleCleanMessages = () => {
        resetMessages();

        generateConversationId();
    };

    useEffect(() => {
        generateConversationId();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    return (
        <div className="relative h-full min-h-[50vh] w-[450px]">
            <div className="flex items-center justify-between px-3 py-5">
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

                    <Button onClick={() => setCopilotPanelOpen(false)} size="icon" variant="ghost">
                        <Cross2Icon />
                    </Button>
                </div>
            </div>

            <div className="absolute inset-x-0 bottom-0 top-16">
                <CopilotRuntimeProvider>
                    <Thread />
                </CopilotRuntimeProvider>
            </div>
        </div>
    );
};

export default CopilotPanel;
