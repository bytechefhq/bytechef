import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useAiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {PinIcon} from 'lucide-react';

/**
 * Pin control shown in the "AI Hub" sidebar header ONLY while the hidden Chats sidebar is being
 * previewed on hover (a "peek"). Clicking promotes the transient, floating preview to a docked,
 * space-reserving sidebar via `setChatsSidebarCollapsed(false)`.
 *
 * It exists because the peek overlay floats exactly over the panel-header toggle
 * ({@link AiHubChatsSidebarToggle}) that opened it, so that toggle can't be clicked while the preview is
 * up. Once the sidebar is pinned open, this control disappears and the header toggle (now visible again
 * beside the chat title, like the sidebar toggle on every other page) is the way to hide it.
 */
const AiHubChatsSidebarPinButton = () => {
    const setChatsSidebarCollapsed = useAiHubTabsStore((state) => state.setChatsSidebarCollapsed);

    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <Button
                    aria-label="Pin chats sidebar open"
                    // Negative right margin pulls the button toward the sidebar edge, counteracting
                    // the shared Header's padding so the icon sits closer to the right edge.
                    className="-mr-2"
                    icon={<PinIcon />}
                    // `setChatsSidebarCollapsed(false)` also clears the peek flag.
                    onClick={() => setChatsSidebarCollapsed(false)}
                    size="icon"
                    variant="ghost"
                />
            </TooltipTrigger>

            <TooltipContent>Pin open</TooltipContent>
        </Tooltip>
    );
};

export default AiHubChatsSidebarPinButton;
