import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useAiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {PanelLeftOpenIcon} from 'lucide-react';

/**
 * Collapsed form of the AI Hub Tasks sidebar, shown only while the resource (right) panel is open.
 *
 * Without it, opening the resource panel hides the full Tasks sidebar entirely — the sidebar
 * vanishes without a trace. This thin rail keeps a single, visible affordance to bring it back,
 * so the chat + resource panels still reclaim most of the width while the user retains an obvious
 * path to their tasks. It deliberately carries nothing else (no nav, no task list): the rail is a
 * re-open handle, and the full sidebar is one click away.
 *
 * Border + background match `LayoutContainer`'s left `aside` so the rail reads as the same sidebar,
 * just collapsed.
 */
interface AiHubTasksSidebarRailProps {
    onMouseEnter?: () => void;
}

const AiHubTasksSidebarRail = ({onMouseEnter}: AiHubTasksSidebarRailProps) => {
    const setTasksSidebarCollapsed = useAiHubTabsStore((state) => state.setTasksSidebarCollapsed);

    return (
        <div
            // py-3 (not py-2) so the toggle icon's vertical center lines up with the task title in the
            // panel header to the right, which sits in a `px-3 py-3` row (see AiHubPanel). Both are
            // same-height icon buttons, so matching the vertical padding aligns their centers.
            className="flex w-12 shrink-0 flex-col items-center border-r border-r-border/50 bg-muted/50 py-3"
            onMouseEnter={onMouseEnter}
        >
            <Tooltip>
                <TooltipTrigger asChild>
                    <Button
                        aria-label="Show tasks sidebar"
                        icon={<PanelLeftOpenIcon />}
                        onClick={() => setTasksSidebarCollapsed(false)}
                        size="icon"
                        variant="ghost"
                    />
                </TooltipTrigger>

                <TooltipContent side="right">Show tasks</TooltipContent>
            </Tooltip>
        </div>
    );
};

export default AiHubTasksSidebarRail;
