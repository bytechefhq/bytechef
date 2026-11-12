import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ListIcon} from 'lucide-react';

interface ConfigurationsListButtonProps {
    label: string;
    onClick: () => void;
}

const ConfigurationsListButton = ({label, onClick}: ConfigurationsListButtonProps) => (
    <Tooltip>
        <TooltipTrigger asChild>
            <Button
                aria-label={label}
                className="rounded-l-none border-l-0 px-2"
                icon={<ListIcon />}
                onClick={onClick}
                size="icon"
                variant="outline"
            />
        </TooltipTrigger>

        <TooltipContent>{label}</TooltipContent>
    </Tooltip>
);

export default ConfigurationsListButton;
