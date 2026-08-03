import {
    Select as ShadcnSelect,
    SelectContent as ShadcnSelectContent,
    SelectGroup as ShadcnSelectGroup,
    SelectItem as ShadcnSelectItem,
    SelectLabel as ShadcnSelectLabel,
    SelectScrollDownButton as ShadcnSelectScrollDownButton,
    SelectScrollUpButton as ShadcnSelectScrollUpButton,
    SelectSeparator as ShadcnSelectSeparator,
    SelectTrigger as ShadcnSelectTrigger,
    SelectValue as ShadcnSelectValue,
} from '@/components/ui/select';
import {type ComponentProps} from 'react';
import {twMerge} from 'tailwind-merge';

type SelectContentPropsType = ComponentProps<typeof ShadcnSelectContent>;

type SelectTriggerPropsType = ComponentProps<typeof ShadcnSelectTrigger>;

/**
 * Wrapper around the shadcn SelectContent that defaults to `position="popper"`.
 *
 * The shadcn base defaults to `position="item-aligned"`, which floats the dropdown over the
 * trigger and sizes it to its content. `popper` instead renders the dropdown below (or above,
 * on collision) the trigger and exposes the `--radix-select-trigger-width` variable, so the
 * dropdown matches the trigger width by default. Both the `position` and the width class can be
 * overridden per usage.
 */
function SelectContent({className, position = 'popper', ...props}: SelectContentPropsType) {
    return (
        <ShadcnSelectContent
            className={twMerge('w-(--radix-select-trigger-width)', className)}
            position={position}
            {...props}
        />
    );
}

SelectContent.displayName = 'SelectContent';

/**
 * Wrapper around the shadcn SelectTrigger that overrides the base `shadow-xs` with `shadow-none`,
 * and replaces the base's `dark:hover:bg-input/50` with the semantic hover token.
 *
 * The shadcn base applies `shadow-xs`, which is inconsistent with the flat inputs used across the
 * property panels. `shadow-none` wins the twMerge cascade, and can still be overridden per usage.
 *
 * The base also carries `dark:hover:bg-input/50`, which compiles to `:is(.dark *)` at specificity
 * (0,3,0) — higher than a call site's own `hover:bg-surface-neutral-primary-hover:hover` at (0,2,0).
 * Because that call-site class has no `dark:` prefix, tailwind-merge does not see it and the base's
 * `dark:hover:` class as the same conflict group, so both compile and the base wins dark-mode hover
 * on every `SelectTrigger` regardless of what a call site asks for. Adding
 * `dark:hover:bg-surface-neutral-primary-hover` here — same `dark:hover:` variant, same `bg-color`
 * group — puts tailwind-merge's own conflict detection to work instead: it dedupes against the
 * base's `dark:hover:bg-input/50` and drops it from the rendered class list entirely, so there is no
 * specificity contest left to win at runtime.
 */
function SelectTrigger({className, ...props}: SelectTriggerPropsType) {
    return (
        <ShadcnSelectTrigger
            className={twMerge('shadow-none dark:hover:bg-surface-neutral-primary-hover', className)}
            {...props}
        />
    );
}

SelectTrigger.displayName = 'SelectTrigger';

const Select = ShadcnSelect;
const SelectGroup = ShadcnSelectGroup;
const SelectItem = ShadcnSelectItem;
const SelectLabel = ShadcnSelectLabel;
const SelectScrollDownButton = ShadcnSelectScrollDownButton;
const SelectScrollUpButton = ShadcnSelectScrollUpButton;
const SelectSeparator = ShadcnSelectSeparator;
const SelectValue = ShadcnSelectValue;

export {
    Select,
    SelectContent,
    SelectGroup,
    SelectItem,
    SelectLabel,
    SelectScrollDownButton,
    SelectScrollUpButton,
    SelectSeparator,
    SelectTrigger,
    SelectValue,
};
export type {SelectContentPropsType as SelectContentProps};
