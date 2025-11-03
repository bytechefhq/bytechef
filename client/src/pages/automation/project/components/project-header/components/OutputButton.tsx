import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {SquareChevronRightIcon} from 'lucide-react';

const OutputButton = ({onShowOutputClick}: {onShowOutputClick: () => void}) => {
    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <Button icon={<SquareChevronRightIcon />} onClick={onShowOutputClick} size="icon" variant="ghost" />
            </TooltipTrigger>

            <TooltipContent>Show the current workflow test execution output</TooltipContent>
        </Tooltip>
    );
};

export default OutputButton;
