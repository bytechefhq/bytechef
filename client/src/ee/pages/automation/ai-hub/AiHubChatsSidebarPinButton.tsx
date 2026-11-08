import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useAiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {PinIcon} from 'lucide-react';

const AiHubChatsSidebarPinButton = () => {
    const setChatsSidebarCollapsed = useAiHubTabsStore((state) => state.setChatsSidebarCollapsed);

    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <Button
                    aria-label="Pin chats sidebar open"
                    className="-mr-2"
                    icon={<PinIcon />}
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
