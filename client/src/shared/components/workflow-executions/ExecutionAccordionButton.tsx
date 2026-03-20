import Button from '@/components/Button/Button';
import {type ReactNode} from 'react';
import {twMerge} from 'tailwind-merge';

interface ExecutionAccordionButtonProps {
    children: ReactNode;
    isSelected: boolean;
    onClick: () => void;
}

const ExecutionAccordionButton = ({children, isSelected, onClick}: ExecutionAccordionButtonProps) => (
    <Button
        className={twMerge(
            'active:text-content-primary h-auto w-full justify-between rounded-md border border-stroke-neutral-primary p-2 text-left transition-colors hover:border-stroke-brand-primary hover:bg-transparent focus-visible:outline focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-stroke-brand-focus focus-visible:ring-0 focus-visible:transition-colors active:bg-transparent [&_svg]:size-5',
            isSelected &&
                'border-stroke-brand-primary bg-surface-neutral-secondary hover:bg-surface-neutral-secondary active:bg-surface-neutral-secondary'
        )}
        onClick={onClick}
        type="button"
        variant="ghost"
    >
        {children}
    </Button>
);

export default ExecutionAccordionButton;
