import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useAiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {PanelLeftCloseIcon, PinIcon} from 'lucide-react';

/**
 * Header control for the AI Hub Tasks sidebar while the resource panel is open. It has two modes,
 * driven by whether the sidebar is currently a hover "peek" or pinned open:
 *
 *  - Peeking (collapsed to the rail, floated in on hover): acts as a PIN — clicking promotes the
 *    transient preview to a docked, space-reserving sidebar via `setTasksSidebarCollapsed(false)`.
 *  - Pinned open: acts as the COLLAPSE control — clicking returns the sidebar to the thin rail.
 *
 * Rendered in the "AI Hub" sidebar header's right slot so it sits in line with the title, and only
 * while the resource panel is open — collapsing/pinning has no meaning otherwise. Mirrors the
 * re-open control on {@link AiHubTasksSidebarRail}.
 */
const AiHubTasksSidebarCollapseButton = () => {
    const tasksSidebarPeeking = useAiHubTabsStore((state) => state.tasksSidebarPeeking);
    const setTasksSidebarCollapsed = useAiHubTabsStore((state) => state.setTasksSidebarCollapsed);

    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <Button
                    aria-label={tasksSidebarPeeking ? 'Pin tasks sidebar open' : 'Collapse tasks sidebar'}
                    // Negative right margin pulls the button toward the sidebar edge, counteracting
                    // the shared Header's px-4 padding so the icon sits closer to the right edge.
                    className="-mr-2"
                    icon={tasksSidebarPeeking ? <PinIcon /> : <PanelLeftCloseIcon />}
                    // Peek → pin open (collapsed=false); pinned → collapse to rail (collapsed=true).
                    // `setTasksSidebarCollapsed` clears the peek either way.
                    onClick={() => setTasksSidebarCollapsed(!tasksSidebarPeeking)}
                    size="icon"
                    variant="ghost"
                />
            </TooltipTrigger>

            <TooltipContent>{tasksSidebarPeeking ? 'Pin open' : 'Collapse to rail'}</TooltipContent>
        </Tooltip>
    );
};

export default AiHubTasksSidebarCollapseButton;
