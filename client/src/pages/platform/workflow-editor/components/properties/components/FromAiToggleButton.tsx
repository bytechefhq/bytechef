import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {SparklesIcon, XIcon} from 'lucide-react';

interface FromAiToggleButtonProps {
    isFromAi: boolean;
    onToggle: (fromAi: boolean) => void;
}

const FromAiToggleButton = ({isFromAi, onToggle}: FromAiToggleButtonProps) => {
    const icon = isFromAi ? <XIcon /> : <SparklesIcon />;
    const title = isFromAi ? 'Customize AI generation' : 'Generate content with AI';
    const variant = isFromAi ? 'destructiveGhost' : 'ghost';

    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <Button
                    className="self-center"
                    icon={icon}
                    onClick={() => onToggle(!isFromAi)}
                    size="iconSm"
                    type="button"
                    variant={variant}
                />
            </TooltipTrigger>

            <TooltipContent>{title}</TooltipContent>
        </Tooltip>
    );
};

export default FromAiToggleButton;
