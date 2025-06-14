import {Button} from '@/components/ui/button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {SquareChevronRightIcon} from 'lucide-react';

const OutputButton = ({onShowOutputClick}: {onShowOutputClick: () => void}) => {
    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <Button
                    className="hover:bg-surface-neutral-primary-hover [&_svg]:size-5"
                    onClick={onShowOutputClick}
                    size="icon"
                    variant="ghost"
                >
                    <SquareChevronRightIcon />
                </Button>
            </TooltipTrigger>

            <TooltipContent>Show the current workflow test execution output</TooltipContent>
        </Tooltip>
    );
};

export default OutputButton;
