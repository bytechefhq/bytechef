import Button from '@/components/Button/Button';
import {Thread} from '@/components/assistant-ui/thread';
import {Select, SelectContent, SelectItem, SelectTrigger} from '@/components/ui/select';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {CopilotRuntimeProvider} from '@/shared/components/copilot/runtime-providers/CopilotRuntimeProvider';
import {MODE, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
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

                <div className="flex items-center space-x-3">
                    <Select
                        onValueChange={(value) =>
                            setContext({
                                ...context,
                                mode: value as MODE,
                            })
                        }
                        value={context?.mode}
                    >
                        <SelectTrigger className="min-w-24">
                            {context?.mode.charAt(0) + context?.mode.slice(1).toLowerCase()}
                        </SelectTrigger>

                        <SelectContent className="min-w-24">
                            <SelectItem value={MODE.ASK}>
                                <span className="text-muted-foreground">
                                    {MODE.ASK.charAt(0) + MODE.ASK.slice(1).toLowerCase()}
                                </span>
                            </SelectItem>

                            <SelectItem value={MODE.BUILD}>
                                <span className="text-muted-foreground">
                                    {MODE.BUILD.charAt(0) + MODE.BUILD.slice(1).toLowerCase()}
                                </span>
                            </SelectItem>
                        </SelectContent>
                    </Select>

                    <Tooltip>
                        <TooltipTrigger asChild>
                            <Button
                                icon={<MessageSquareOffIcon />}
                                onClick={handleCleanMessages}
                                size="icon"
                                variant="ghost"
                            />
                        </TooltipTrigger>

                        <TooltipContent>Clean messages</TooltipContent>
                    </Tooltip>

                    <Button icon={<XIcon />} onClick={handleCloseClick} size="icon" variant="ghost" />
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
