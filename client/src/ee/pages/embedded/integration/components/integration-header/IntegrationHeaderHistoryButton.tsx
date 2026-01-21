import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {HistoryIcon} from 'lucide-react';

interface IntegrationHeaderHistoryButtonProps {
    onClick: () => void;
}

const IntegrationHeaderHistoryButton = ({onClick}: IntegrationHeaderHistoryButtonProps) => {
    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <Button
                    className="hover:bg-background/70 [&_svg]:size-5"
                    icon={<HistoryIcon />}
                    onClick={onClick}
                    size="icon"
                    variant="ghost"
                />
            </TooltipTrigger>

            <TooltipContent>Integration Version History</TooltipContent>
        </Tooltip>
    );
};

export default IntegrationHeaderHistoryButton;
