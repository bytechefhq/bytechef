import {Input as ShadcnInput} from '@/components/ui/input';
import {type ComponentProps} from 'react';
import {twMerge} from 'tailwind-merge';

type InputPropsType = ComponentProps<typeof ShadcnInput>;

/**
 * Wrapper around the shadcn Input that removes the base `shadow-xs`, the
 * `focus-visible:border-ring` border shift, and restyles the `aria-invalid` error ring.
 *
 * The shadcn base applies `shadow-xs` and swaps the border color to `ring` on focus, both of
 * which are inconsistent with the flat inputs used across the property panels. `shadow-none` and
 * `focus-visible:border-input` win the twMerge cascade, and can still be overridden per usage.
 *
 * The base `aria-invalid` state uses `border-destructive` with a faint `ring-destructive/20`. This
 * wrapper instead renders a full-width `stroke-destructive-secondary` ring at the same `3px` width
 * as the focus ring, matching the Figma error state.
 */
function Input({className, ...props}: InputPropsType) {
    return (
        <ShadcnInput
            className={twMerge(
                'shadow-none focus:border-input focus-visible:border-input aria-invalid:border-stroke-destructive-secondary aria-invalid:ring-[3px] aria-invalid:ring-stroke-destructive-secondary',
                className
            )}
            {...props}
        />
    );
}

Input.displayName = 'Input';

export {Input};
export type {InputPropsType as InputProps};
