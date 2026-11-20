import Button from '@/components/Button/Button';
import {SparklesIcon} from 'lucide-react';

interface LetAiFillRequiredButtonProps {
    disabled?: boolean;
    onClick: () => void;
}

const LetAiFillRequiredButton = ({disabled, onClick}: LetAiFillRequiredButtonProps) => (
    <div className="flex justify-end">
        <Button
            disabled={disabled}
            icon={<SparklesIcon />}
            label="Let AI fill required"
            onClick={onClick}
            size="sm"
            type="button"
            variant="ghost"
        />
    </div>
);

export default LetAiFillRequiredButton;
