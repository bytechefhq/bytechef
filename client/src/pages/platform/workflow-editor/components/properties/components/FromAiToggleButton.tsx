import Button from '@/components/Button/Button';
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
        <Button
            className="self-center"
            icon={icon}
            onClick={() => onToggle(!isFromAi)}
            size="iconSm"
            title={title}
            type="button"
            variant={variant}
        />
    );
};

export default FromAiToggleButton;
