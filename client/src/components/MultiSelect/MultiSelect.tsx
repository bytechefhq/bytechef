/* eslint-disable tailwindcss/no-custom-classname */

import {Badge} from '@/components/ui/badge';
import {Button} from '@/components/ui/button';
import {
    Command,
    CommandEmpty,
    CommandGroup,
    CommandInput,
    CommandItem,
    CommandList,
    CommandSeparator,
} from '@/components/ui/command';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {Separator} from '@/components/ui/separator';
import {type VariantProps, cva} from 'class-variance-authority';
import {CheckIcon, ChevronDown, CircleXIcon, XIcon} from 'lucide-react';
import {ButtonHTMLAttributes, ComponentType, ReactNode, forwardRef, useState} from 'react';
import {twMerge} from 'tailwind-merge';

import {ScrollArea} from '../ui/scroll-area';

export type MultiSelectOptionType = {
    label: string;
    value: string;
    description?: string;
    icon?: ComponentType<{className?: string}>;
};

/**
 * Variants for the multi-select component to handle different styles.
 * Uses class-variance-authority (cva) to define different styles based on "variant" prop.
 */
const multiSelectVariants = cva(
    'mr-1 cursor-default text-xs font-normal shadow-none transition delay-150 duration-300 ease-in-out',
    {
        defaultVariants: {
            variant: 'default',
        },
        variants: {
            variant: {
                default: 'bg-surface-brand-secondary text-content-neutral-primary hover:bg-surface-brand-secondary',
                destructive: 'border-transparent bg-destructive text-destructive-foreground hover:bg-destructive/80',
                inverted: 'inverted',
                secondary: 'border-foreground/10 bg-secondary text-secondary-foreground hover:bg-secondary/80',
            },
        },
    }
);

/**
 * Props for MultiSelect component
 */
interface MultiSelectProps extends ButtonHTMLAttributes<HTMLButtonElement>, VariantProps<typeof multiSelectVariants> {
    /**
     * Icon component to display on the left side of the multi-select component.
     * Optional, defaults to undefined.
     */
    leadingIcon?: ReactNode;
    /**
     * An array of option objects to be displayed in the multi-select component.
     * Each option object has a label, value, and an optional icon.
     */
    options: MultiSelectOptionType[];

    /**
     * Callback function triggered when the selected values change.
     * Receives an array of the new selected values.
     */
    onValueChange: (value: string[]) => void;

    /** The default selected values when the component mounts. */
    defaultValue?: string[];

    /**
     * Placeholder text to be displayed when no values are selected.
     * Optional, defaults to "Select options".
     */
    placeholder?: string;

    /**
     * Maximum number of items to display. Extra selected items will be summarized.
     * Optional, defaults to 3.
     */
    maxCount?: number;

    /**
     * The modality of the popover. When set to true, interaction with outside elements
     * will be disabled and only popover content will be visible to screen readers.
     * Optional, defaults to false.
     */
    modalPopover?: boolean;

    /**
     * If true, shows a loading indicator on the multi-select component.
     * Optional, defaults to false.
     */
    optionsLoading?: boolean;

    /**
     * Additional class names to apply custom styles to the multi-select component.
     * Optional, can be used to add custom styles.
     */
    className?: string;

    /**
     * Additional class name to apply custom styles to the placeholder.
     * Optional, can be used to add custom styles
     */
    placeholderClassName?: string;

    /**
     * If true, enables the search functionality in the multi-select component.
     * Optional, defaults to false.
     */
    searchable?: boolean;

    /**
     * If true, shows the footer with clear and close buttons.
     * Optional, defaults to false.
     */
    showFooter?: boolean;

    /**
     * The selected values in the multi-select component.
     * Optional, defaults to an empty array.
     */
    value?: string[];
}

export const MultiSelect = forwardRef<HTMLButtonElement, MultiSelectProps>(
    (
        {
            className,
            defaultValue = [],
            leadingIcon,
            maxCount = 3,
            modalPopover = false,
            onValueChange,
            options,
            optionsLoading = false,
            placeholder = 'Select...',
            placeholderClassName,
            searchable = false,
            showFooter = false,
            value,
            variant,
            ...props
        },
        ref
    ) => {
        const isControlled = value !== undefined;

        const [uncontrolledValues, setUncontrolledValues] = useState(defaultValue);

        const selectedValues = isControlled ? value : uncontrolledValues;

        const [isPopoverOpen, setIsPopoverOpen] = useState(false);

        const updateValues = (newValues: string[]) => {
            if (!isControlled) {
                setUncontrolledValues(newValues);
            }

            onValueChange(newValues);
        };

        const toggleOption = (option: string) => {
            const newSelectedValues = selectedValues.includes(option)
                ? selectedValues.filter((value) => value !== option)
                : [...selectedValues, option];

            updateValues(newSelectedValues);
        };

        const handleClear = () => {
            updateValues([]);
        };

        const handleTogglePopover = () => setIsPopoverOpen((prev) => !prev);

        const clearExtraOptions = () => {
            const newSelectedValues = selectedValues.slice(0, maxCount);
            updateValues(newSelectedValues);
        };

        const toggleAll = () => {
            if (selectedValues.length === options.length) {
                handleClear();
            } else {
                const allValues = options.map((option) => option.value);

                updateValues(allValues);
            }
        };

        return (
            <Popover modal={modalPopover} onOpenChange={setIsPopoverOpen} open={isPopoverOpen}>
                <PopoverTrigger asChild>
                    <Button
                        ref={ref}
                        {...props}
                        className={twMerge(
                            'flex h-auto w-full items-center justify-between rounded-md border border-input bg-background px-4 py-2 text-sm font-normal text-primary shadow-sm hover:bg-inherit [&_svg]:pointer-events-auto',
                            leadingIcon && 'relative',
                            optionsLoading && 'min-h-10 cursor-not-allowed',
                            className
                        )}
                        disabled={!options.length || optionsLoading}
                        onClick={handleTogglePopover}
                    >
                        {leadingIcon && (
                            <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center rounded-l-md border-r border-gray-200 bg-gray-100 px-3">
                                {leadingIcon}
                            </div>
                        )}

                        {selectedValues?.length > 0 && !optionsLoading && (
                            <div className="flex w-full items-center justify-between">
                                <div className={twMerge('flex flex-wrap items-center', leadingIcon && 'pl-9')}>
                                    {selectedValues.slice(0, maxCount).map((value) => {
                                        const option = options.find((o) => o.value === value);
                                        const IconComponent = option?.icon;

                                        return (
                                            <Badge className={twMerge(multiSelectVariants({variant}))} key={value}>
                                                {IconComponent && <IconComponent className="mr-2 size-4" />}

                                                <span aria-label={`${option?.label ?? option?.value}-selected`}>
                                                    {option?.label ?? option?.value}
                                                </span>

                                                <XIcon
                                                    aria-label="remove-option"
                                                    className="ml-2 size-4 cursor-pointer text-content-neutral-secondary hover:text-destructive"
                                                    onClick={(event) => {
                                                        event.stopPropagation();
                                                        toggleOption(value);
                                                    }}
                                                />
                                            </Badge>
                                        );
                                    })}

                                    {selectedValues.length > maxCount && (
                                        <Badge
                                            aria-label="more-count"
                                            className={twMerge(
                                                multiSelectVariants({variant}),
                                                'border-stroke-neutral-secondary bg-white hover:bg-white'
                                            )}
                                        >
                                            {`+ ${selectedValues.length - maxCount} more`}

                                            <XIcon
                                                aria-label="clear-extra-options"
                                                className="ml-2 size-4 cursor-pointer text-content-neutral-secondary hover:text-destructive"
                                                onClick={(event) => {
                                                    event.stopPropagation();
                                                    clearExtraOptions();
                                                }}
                                            />
                                        </Badge>
                                    )}
                                </div>

                                <div className="flex items-center justify-between">
                                    <CircleXIcon
                                        aria-label="clear-all"
                                        className="mr-1 cursor-pointer fill-content-neutral-secondary text-white hover:fill-destructive"
                                        onClick={(event) => {
                                            event.stopPropagation();
                                            handleClear();
                                        }}
                                    />

                                    <ChevronDown
                                        className={twMerge(
                                            'cursor-pointer text-content-neutral-secondary transition-transform duration-200',
                                            isPopoverOpen && 'rotate-180 transform'
                                        )}
                                    />
                                </div>
                            </div>
                        )}

                        {(!selectedValues || selectedValues.length === 0) && (
                            <div className="mx-auto flex w-full items-center justify-between">
                                <span
                                    aria-label={placeholder}
                                    className={twMerge(
                                        'flex items-center text-sm text-primary',
                                        leadingIcon && 'ml-9',
                                        placeholderClassName
                                    )}
                                >
                                    {placeholder}
                                </span>

                                <ChevronDown
                                    className={twMerge(
                                        'h-4 cursor-pointer text-content-neutral-secondary transition-transform duration-200',
                                        isPopoverOpen && 'rotate-180 transform'
                                    )}
                                />
                            </div>
                        )}
                    </Button>
                </PopoverTrigger>

                <PopoverContent
                    align="start"
                    className="w-[--radix-popover-trigger-width] p-0"
                    onEscapeKeyDown={() => setIsPopoverOpen(false)}
                >
                    <Command>
                        {searchable && <CommandInput className="my-1 h-8 border-0" placeholder="Search..." />}

                        <CommandList className="overflow-hidden">
                            <CommandEmpty>No results found.</CommandEmpty>

                            <CommandGroup>
                                <CommandItem className="cursor-pointer" key="all" onSelect={toggleAll}>
                                    <div
                                        className={twMerge(
                                            'flex size-4 items-center justify-center rounded-xs border border-stroke-neutral-tertiary',
                                            selectedValues?.length === options.length
                                                ? 'border-content-brand-primary bg-content-brand-primary text-primary-foreground'
                                                : '[&_svg]:invisible'
                                        )}
                                    >
                                        <CheckIcon className="size-4" />
                                    </div>

                                    <span aria-label="Select All">Select All</span>
                                </CommandItem>

                                <CommandSeparator className="my-1.5" />

                                <ScrollArea className="h-48 pr-3">
                                    {options.map((option) => {
                                        const isSelected = selectedValues?.includes(option.value);

                                        return (
                                            <CommandItem
                                                className="cursor-pointer"
                                                key={option.value}
                                                onSelect={() => toggleOption(option.value)}
                                            >
                                                <div
                                                    className={twMerge(
                                                        'flex h-4 w-4 items-center justify-center rounded-xs border border-stroke-neutral-tertiary',
                                                        isSelected
                                                            ? 'border-content-brand-primary bg-content-brand-primary text-primary-foreground'
                                                            : '[&_svg]:invisible'
                                                    )}
                                                >
                                                    <CheckIcon className="size-4" />
                                                </div>

                                                {option.icon && (
                                                    <option.icon className="mr-2 size-4 text-muted-foreground" />
                                                )}

                                                <span aria-label={option.label ?? option.value}>
                                                    {option.label ?? option.value}
                                                </span>
                                            </CommandItem>
                                        );
                                    })}
                                </ScrollArea>
                            </CommandGroup>

                            {showFooter && (
                                <>
                                    <CommandSeparator className="my-1.5" />

                                    <CommandGroup>
                                        <div className="flex items-center justify-between">
                                            {selectedValues?.length > 0 && (
                                                <>
                                                    <CommandItem
                                                        aria-label="clear-all"
                                                        className="flex-1 cursor-pointer justify-center"
                                                        onSelect={handleClear}
                                                    >
                                                        Clear
                                                    </CommandItem>

                                                    <Separator className="flex h-full min-h-6" orientation="vertical" />
                                                </>
                                            )}

                                            <CommandItem
                                                className="max-w-full flex-1 cursor-pointer justify-center"
                                                onSelect={() => setIsPopoverOpen(false)}
                                            >
                                                Close
                                            </CommandItem>
                                        </div>
                                    </CommandGroup>
                                </>
                            )}
                        </CommandList>
                    </Command>
                </PopoverContent>
            </Popover>
        );
    }
);

MultiSelect.displayName = 'MultiSelect';
