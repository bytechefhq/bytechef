import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useAiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {PanelLeftIcon} from 'lucide-react';
import {useEffect, useRef, useState} from 'react';

/**
 * How long the pointer has to rest on the toggle before the hidden sidebar is previewed. A short
 * hover-intent delay keeps a plain click (which docks the sidebar open) from being pre-empted by the
 * preview: the peek overlay floats exactly over this button, so once it is up the click would land on
 * the overlay instead.
 */
export const PEEK_DELAY_MS = 300;

/**
 * The AI Hub's left-sidebar toggle. Sits first in the panel header's title row — the same
 * PanelLeftIcon ghost button the shared `Header` renders before the title on every other page (see
 * `LeftSidebarToggle`). The AI Hub can't use that one because it controls `leftSidebarOpen` itself
 * (it needs to, for the hover peek), which opts a page out of `LayoutContainer`'s automatic toggle.
 *
 * Click toggles the sidebar between docked-open and hidden. While hidden, resting the pointer on the
 * button previews ("peeks") the sidebar as a floating overlay — the same preview the old collapsed
 * rail offered — which slides back out when the pointer leaves it, or is pinned open via
 * {@link AiHubChatsSidebarPinButton} in its header.
 */
const AiHubChatsSidebarToggle = () => {
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
        // Nothing to preview while the sidebar is already docked open.
        if (!chatsSidebarCollapsed) {
            return;
        }

        cancelPendingPeek();

        peekTimerRef.current = setTimeout(() => {
            peekTimerRef.current = undefined;

            setChatsSidebarPeeking(true);
        }, PEEK_DELAY_MS);
    };

    // A pending peek must not fire into an unmounted toggle (e.g. the home → chat swap re-mounts the
    // header while the pointer is resting on the button).
    useEffect(() => () => clearTimeout(peekTimerRef.current), []);

    return (
        // Controlled so the tooltip can be forced shut while a peek is up: the overlay pops in over this
        // button without any pointer movement, so the trigger never sees the pointer leave and the tooltip
        // would otherwise linger on top of the previewed sidebar.
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
};

export default AiHubChatsSidebarToggle;
