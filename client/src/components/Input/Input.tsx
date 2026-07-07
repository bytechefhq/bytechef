import {Input as ShadcnInput} from '@/components/ui/input';
import {type ComponentProps} from 'react';
import {twMerge} from 'tailwind-merge';

type InputPropsType = ComponentProps<typeof ShadcnInput>;

/**
 * Wrapper around the shadcn Input that removes the base `shadow-xs` and the
 * `focus-visible:border-ring` border shift.
 *
 * The shadcn base applies `shadow-xs` and swaps the border color to `ring` on focus, both of
 * which are inconsistent with the flat inputs used across the property panels. `shadow-none` and
 * `focus-visible:border-input` win the twMerge cascade, and can still be overridden per usage.
 */
function Input({className, ...props}: InputPropsType) {
    return (
        <ShadcnInput
            className={twMerge('shadow-none focus-visible:border-input', className)}
            {...props}
        />
    );
}

Input.displayName = 'Input';

export {Input};
export type {InputPropsType as InputProps};
