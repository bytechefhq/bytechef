import {CheckIcon} from 'lucide-react';

interface StepDoneTextProps {
    text: string;
}

const StepDoneText = ({text}: StepDoneTextProps) => (
    <span className="flex items-center gap-1 text-xs text-content-success-primary">
        <CheckIcon aria-hidden="true" className="size-3" />

        {text}
    </span>
);

export default StepDoneText;
