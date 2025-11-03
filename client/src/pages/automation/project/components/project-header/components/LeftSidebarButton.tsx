import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {PanelLeftIcon} from 'lucide-react';

const LeftSidebarButton = ({onLeftSidebarOpenClick}: {onLeftSidebarOpenClick: () => void}) => {
    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <Button icon={<PanelLeftIcon />} onClick={onLeftSidebarOpenClick} size="icon" variant="ghost" />
            </TooltipTrigger>

            <TooltipContent>See projects</TooltipContent>
        </Tooltip>
    );
};

export default LeftSidebarButton;
