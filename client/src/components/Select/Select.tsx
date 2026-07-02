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

const Select = ShadcnSelect;
const SelectGroup = ShadcnSelectGroup;
const SelectItem = ShadcnSelectItem;
const SelectLabel = ShadcnSelectLabel;
const SelectScrollDownButton = ShadcnSelectScrollDownButton;
const SelectScrollUpButton = ShadcnSelectScrollUpButton;
const SelectSeparator = ShadcnSelectSeparator;
const SelectTrigger = ShadcnSelectTrigger;
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
