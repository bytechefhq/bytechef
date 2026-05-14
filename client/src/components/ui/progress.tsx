import * as React from 'react';
import {Progress as ProgressPrimitive} from 'radix-ui';
import {twMerge} from 'tailwind-merge';

const Progress = React.forwardRef<
    React.ElementRef<typeof ProgressPrimitive.Root>,
    React.ComponentPropsWithoutRef<typeof ProgressPrimitive.Root>
>(({className, value, ...props}, ref) => (
    <ProgressPrimitive.Root
        className={twMerge('relative h-2 w-full overflow-hidden rounded-full bg-gray-200', className)}
        ref={ref}
        {...props}
    >
        <ProgressPrimitive.Indicator
            className="size-full flex-1 bg-stroke-brand-primary transition-all"
            style={{transform: `translateX(-${100 - (value || 0)}%)`}}
        />
    </ProgressPrimitive.Root>
));

Progress.displayName = ProgressPrimitive.Root.displayName;

export {Progress};
