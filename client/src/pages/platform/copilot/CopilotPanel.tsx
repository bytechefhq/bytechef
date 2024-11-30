import {Button} from '@/components/ui/button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useCopilotStore} from '@/pages/platform/copilot/stores/useCopilotStore';
import {Thread, useAssistantRuntime} from '@assistant-ui/react';
import {LocalRuntime} from '@assistant-ui/react/dist/runtimes/local/useLocalRuntime';
import {Cross2Icon} from '@radix-ui/react-icons';
import {BotMessageSquareIcon, MessageSquareOffIcon} from 'lucide-react';

export type MessageType = {text: string; sender: 'ai' | 'user'};

const CopilotPanel = () => {
    const {setCopilotPanelOpen} = useCopilotStore();

    const runtime = useAssistantRuntime();

    return (
        <div className="relative h-full min-h-[50vh] w-[450px]">
            <div className="flex items-center justify-between px-3 py-5">
                <div className="flex items-center space-x-1">
                    <BotMessageSquareIcon className="size-6" /> <h4>AI Copilot</h4>
                </div>

                <div className="flex">
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <Button onClick={() => (runtime as LocalRuntime).reset()} size="icon" variant="ghost">
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
                <Thread />
            </div>
        </div>
    );
};

export default CopilotPanel;
