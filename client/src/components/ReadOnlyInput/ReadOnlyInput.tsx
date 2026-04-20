import {Label} from '@/components/ui/label';
import React, {ReactNode} from 'react';
import {twMerge} from 'tailwind-merge';

interface ReadOnlyInputProps {
    className?: string;
    inlineIcon?: ReactNode;
    label: string;
    text: string;
}

const ReadOnlyInput = React.forwardRef<HTMLDivElement, ReadOnlyInputProps>(
    ({className, inlineIcon, label, text}, ref) => (
        <fieldset className={twMerge('space-y-1', className)}>
            <Label>{label}</Label>

            <div
                className="flex h-9 w-full items-center gap-2 rounded-md border border-dashed border-input bg-white px-3 py-1 md:text-sm"
                ref={ref}
            >
                {inlineIcon && <div className="size-5 shrink-0">{inlineIcon}</div>}

                <span className="min-w-0 truncate">{text}</span>
            </div>
        </fieldset>
    )
);

ReadOnlyInput.displayName = 'ReadOnlyInput';

export default ReadOnlyInput;
