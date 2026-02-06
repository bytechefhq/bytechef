import Button from '@/components/Button/Button';
import {Thread} from '@/components/assistant-ui/thread';
import {ToggleGroup, ToggleGroupItem} from '@/components/ui/toggle-group';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {CopilotRuntimeProvider} from '@/shared/components/copilot/runtime-providers/CopilotRuntimeProvider';
import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import {MODE, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {BotMessageSquareIcon, MessageSquareOffIcon, XIcon} from 'lucide-react';
import {useEffect, useRef} from 'react';
import {useLocation} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

interface CopilotPanelProps {
    className?: string;
    onClose?: () => void;
}

const CopilotPanel = ({className, onClose}: CopilotPanelProps) => {
    const {context, generateConversationId, resetMessages, setContext} = useCopilotStore(
        useShallow((state) => ({
            context: state.context,
            generateConversationId: state.generateConversationId,
            resetMessages: state.resetMessages,
            setContext: state.setContext,
        }))
    );
    const setCopilotPanelOpen = useCopilotPanelStore((state) => state.setCopilotPanelOpen);

    const location = useLocation();

    const handleCleanMessages = () => {
        resetMessages();
        generateConversationId();
    };

    const handleCloseClick = () => {
        if (onClose) {
            onClose();
        } else {
            setContext(undefined);
            setCopilotPanelOpen(false);
        }
    };

    const previousPathnameRef = useRef(location.pathname);

    useEffect(() => {
        if (previousPathnameRef.current !== location.pathname) {
            previousPathnameRef.current = location.pathname;

            generateConversationId();
            resetMessages();
        }
    }, [generateConversationId, location.pathname, resetMessages]);

    return (
        <div className={twMerge('relative h-full min-h-[50vh] w-[450px] bg-surface-main', className)}>
            <div className="flex items-center justify-between p-3">
                <div className="flex items-center space-x-1">
                    <BotMessageSquareIcon className="size-6" /> <h4>AI Copilot</h4>
                </div>

                <div className="flex items-center gap-2">
                    <ToggleGroup
                        onValueChange={(value) => {
                            if (value) {
                                setContext({
                                    ...context,
                                    mode: value as MODE,
                                });
                            }
                        }}
                        type="single"
                        value={context?.mode}
                    >
                        <ToggleGroupItem value={MODE.ASK}>
                            {MODE.ASK.charAt(0) + MODE.ASK.slice(1).toLowerCase()}
                        </ToggleGroupItem>

                        <ToggleGroupItem value={MODE.BUILD}>
                            {MODE.BUILD.charAt(0) + MODE.BUILD.slice(1).toLowerCase()}
                        </ToggleGroupItem>
                    </ToggleGroup>

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
