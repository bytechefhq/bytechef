import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useAiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {PanelLeftIcon} from 'lucide-react';

/**
 * The AI Hub's left-sidebar toggle, first in the panel header's title row like the shared `Header`'s toggle on other
 * pages. The hub renders its own because it controls `leftSidebarOpen` itself.
 */
const AiHubChatsSidebarToggle = () => {
    const chatsSidebarCollapsed = useAiHubTabsStore((state) => state.chatsSidebarCollapsed);
    const setChatsSidebarCollapsed = useAiHubTabsStore((state) => state.setChatsSidebarCollapsed);

    const label = chatsSidebarCollapsed ? 'Show chats' : 'Hide chats';

    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <Button
                    aria-label={label}
                    icon={<PanelLeftIcon />}
                    onClick={() => setChatsSidebarCollapsed(!chatsSidebarCollapsed)}
                    size="icon"
                    variant="ghost"
                />
            </TooltipTrigger>

            <TooltipContent>{label}</TooltipContent>
        </Tooltip>
    );
};

export default AiHubChatsSidebarToggle;
