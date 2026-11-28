import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/Tooltip/Tooltip';
import {useAiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {PanelLeftIcon} from 'lucide-react';
import {useEffect, useRef, useState} from 'react';
import {twMerge} from 'tailwind-merge';

export const PEEK_DELAY_MS = 300;

interface AiHubChatsSidebarToggleProps {
    className?: string;
    placement?: 'content' | 'sidebar';
}

const AiHubChatsSidebarToggle = ({className, placement = 'content'}: AiHubChatsSidebarToggleProps) => {
    const [tooltipOpen, setTooltipOpen] = useState(false);

    const peekTimerRef = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);

    const chatsSidebarCollapsed = useAiHubTabsStore((state) => state.chatsSidebarCollapsed);
    const chatsSidebarPeeking = useAiHubTabsStore((state) => state.chatsSidebarPeeking);
    const setChatsSidebarCollapsed = useAiHubTabsStore((state) => state.setChatsSidebarCollapsed);
    const setChatsSidebarPeeking = useAiHubTabsStore((state) => state.setChatsSidebarPeeking);

    const label = chatsSidebarCollapsed ? 'Show chats' : 'Hide chats';

    const cancelPendingPeek = () => {
        if (peekTimerRef.current !== undefined) {
            clearTimeout(peekTimerRef.current);

            peekTimerRef.current = undefined;
        }
    };

    const schedulePeek = () => {
        if (!chatsSidebarCollapsed) {
            return;
        }

        cancelPendingPeek();

        peekTimerRef.current = setTimeout(() => {
            peekTimerRef.current = undefined;

            setChatsSidebarPeeking(true);
        }, PEEK_DELAY_MS);
    };

    useEffect(() => () => clearTimeout(peekTimerRef.current), []);

    if (placement === 'sidebar' ? chatsSidebarCollapsed : !chatsSidebarCollapsed) {
        return null;
    }

    const toggle = (
        <Tooltip onOpenChange={setTooltipOpen} open={tooltipOpen && !chatsSidebarPeeking}>
            <TooltipTrigger asChild>
                <Button
                    aria-label={label}
                    icon={<PanelLeftIcon />}
                    onClick={() => {
                        cancelPendingPeek();
                        setChatsSidebarCollapsed(!chatsSidebarCollapsed);
                    }}
                    onMouseEnter={schedulePeek}
                    onMouseLeave={cancelPendingPeek}
                    size="icon"
                    variant="ghost"
                />
            </TooltipTrigger>

            <TooltipContent>{label}</TooltipContent>
        </Tooltip>
    );

    if (placement === 'sidebar') {
        return toggle;
    }

    return <div className={twMerge('flex h-header-height items-center', className)}>{toggle}</div>;
};

export default AiHubChatsSidebarToggle;
