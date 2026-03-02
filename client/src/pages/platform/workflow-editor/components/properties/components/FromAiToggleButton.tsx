import Button from '@/components/Button/Button';
import {SparklesIcon, XIcon} from 'lucide-react';

interface FromAiToggleButtonProps {
    isFromAi: boolean;
    onToggle: (fromAi: boolean) => void;
}

const FromAiToggleButton = ({isFromAi, onToggle}: FromAiToggleButtonProps) => {
    if (isFromAi) {
        return (
            <Button
                className="self-center"
                icon={<XIcon />}
                onClick={() => onToggle(false)}
                size="iconSm"
                title="Customize AI generation"
                type="button"
                variant="destructiveGhost"
            />
        );
    }

    return (
        <Button
            className="self-center"
            icon={<SparklesIcon />}
            onClick={() => onToggle(true)}
            size="iconSm"
            title="Generate content with AI"
            type="button"
            variant="ghost"
        />
    );
};

export default FromAiToggleButton;
