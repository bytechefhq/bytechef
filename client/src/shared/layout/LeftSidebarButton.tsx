import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {PanelLeftIcon} from 'lucide-react';

/**
 * `tooltip` names what the sidebar lists where a page knows something more useful than "the sidebar" — the
 * project editor says "See projects". The shared `Header` renders this for every page with a sidebar, so the
 * default has to hold for all of them.
 */
const LeftSidebarButton = ({
    onLeftSidebarOpenClick,
    tooltip = 'Toggle sidebar',
}: {
    onLeftSidebarOpenClick: () => void;
    tooltip?: string;
}) => {
    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <Button
                    aria-label={tooltip}
                    icon={<PanelLeftIcon />}
                    onClick={onLeftSidebarOpenClick}
                    size="icon"
                    variant="ghost"
                />
            </TooltipTrigger>

            <TooltipContent>{tooltip}</TooltipContent>
        </Tooltip>
    );
};

export default LeftSidebarButton;
