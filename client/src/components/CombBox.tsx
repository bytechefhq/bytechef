import {Button} from '@/components/ui/button';
import {
    Command,
    CommandEmpty,
    CommandGroup,
    CommandInput,
    CommandItem,
} from '@/components/ui/command';
import {Label} from '@/components/ui/label';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {ScrollArea} from '@/components/ui/scroll-area';
import {cn} from '@/lib/utils';
import {CaretSortIcon, CheckIcon} from '@radix-ui/react-icons';
import {ReactNode, useState} from 'react';
import {FieldValues} from 'react-hook-form';
import {FieldPath} from 'react-hook-form/dist/types';
import {ControllerRenderProps} from 'react-hook-form/dist/types/controller';
import InlineSVG from 'react-inlinesvg';

export type ComboBoxItem = {
    icon?: string;
    label: string | ReactNode;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    value: any;
    [key: string]: unknown;
};

export type ComboBoxProps<
    TFieldValues extends FieldValues = FieldValues,
    TName extends FieldPath<TFieldValues> = FieldPath<TFieldValues>,
> = {
    field?: ControllerRenderProps<TFieldValues, TName>;
    items: ComboBoxItem[];
    label?: string | ReactNode;
    maxHeight?: boolean;
    name?: string;
    onChange: (item?: ComboBoxItem) => void;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    value?: any;
};

const ComboBox = <
    TFieldValues extends FieldValues = FieldValues,
    TName extends FieldPath<TFieldValues> = FieldPath<TFieldValues>,
>({
    field,
    items,
    label,
    maxHeight = false,
    name,
    onChange,
    value,
}: ComboBoxProps<TFieldValues, TName>) => {
    const [open, setOpen] = useState(false);

    const commandItems = items.map((item) => (
        <CommandItem
            key={item.value}
            value={item.value}
            onSelect={() => {
                setOpen(false);
                onChange(item);
            }}
        >
            {item.icon && (
                <InlineSVG className="mr-2 h-6 w-6 flex-none" src={item.icon} />
            )}

            {item.label}

            <CheckIcon
                className={cn(
                    'ml-auto h-4 w-4',
                    item.value === (value || field?.value)
                        ? 'opacity-100'
                        : 'opacity-0'
                )}
            />
        </CommandItem>
    ));

    const item = items.find((item) => item.value === (value || field?.value));

    return (
        <fieldset className="mb-3">
            <Label htmlFor={name || field?.value}>{label}</Label>

            <Popover open={open} onOpenChange={setOpen}>
                <PopoverTrigger asChild>
                    <Button
                        aria-expanded={open}
                        className="mt-1 w-full justify-between"
                        name={name}
                        role="combobox"
                        variant="outline"
                    >
                        {value || field?.value ? (
                            <span className="flex w-full items-center">
                                {item?.icon && (
                                    <InlineSVG
                                        className="mr-2 h-6 w-6 flex-none"
                                        src={item?.icon}
                                    />
                                )}

                                {item?.label}
                            </span>
                        ) : (
                            'Select...'
                        )}

                        <CaretSortIcon className="ml-2 h-4 w-4 shrink-0 opacity-50" />
                    </Button>
                </PopoverTrigger>

                <PopoverContent
                    align="start"
                    /* eslint-disable tailwindcss/no-custom-classname */
                    className="min-w-[var(--radix-popper-anchor-width)) p-0"
                >
                    <Command>
                        <CommandInput placeholder="Search..." className="h-9" />

                        <CommandEmpty>No item found.</CommandEmpty>

                        <CommandGroup>
                            {maxHeight ? (
                                <ScrollArea className="h-72 w-full">
                                    {commandItems}
                                </ScrollArea>
                            ) : (
                                commandItems
                            )}
                        </CommandGroup>
                    </Command>
                </PopoverContent>
            </Popover>
        </fieldset>
    );
};

export default ComboBox;
