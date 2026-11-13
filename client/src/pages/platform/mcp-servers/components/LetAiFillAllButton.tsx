import Button from '@/components/Button/Button';
import {SparklesIcon} from 'lucide-react';

interface LetAiFillAllButtonProps {
    disabled?: boolean;
    onClick: () => void;
}

const LetAiFillAllButton = ({disabled, onClick}: LetAiFillAllButtonProps) => (
    <div className="flex justify-end">
        <Button
            disabled={disabled}
            icon={<SparklesIcon />}
            label="Let AI fill all"
            onClick={onClick}
            size="sm"
            type="button"
            variant="ghost"
        />
    </div>
);

export default LetAiFillAllButton;
