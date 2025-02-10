import {Button} from '@/components/ui/button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {PanelLeftIcon} from 'lucide-react';

const LeftLeftSidebarButton = ({onLeftSidebarOpenClick}: {onLeftSidebarOpenClick: () => void}) => {
    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <Button
                    className="hover:bg-surface-neutral-primary-hover [&_svg]:size-5"
                    onClick={onLeftSidebarOpenClick}
                    size="icon"
                    variant="ghost"
                >
                    <PanelLeftIcon />
                </Button>
            </TooltipTrigger>

            <TooltipContent>See projects</TooltipContent>
        </Tooltip>
    );
};

export default LeftLeftSidebarButton;
