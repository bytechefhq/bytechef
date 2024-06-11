import {Button} from '@/components/ui/button';
import {Command, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList} from '@/components/ui/command';
import {Popover, PopoverTrigger} from '@/components/ui/popover';
import {cn} from '@/shared/util/cn-utils';
import {CaretSortIcon, CheckIcon} from '@radix-ui/react-icons';
import * as PopoverPrimitive from '@radix-ui/react-popover';
import {FocusEventHandler, ReactNode, useState} from 'react';
import InlineSVG from 'react-inlinesvg';

export type ComboBoxItemType = {
    icon?: string;
    label: string | ReactNode;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    value: any;
    [key: string]: unknown;
};

export interface ComboBoxProps {
    disabled?: boolean;
    items: ComboBoxItemType[];
    maxHeight?: boolean;
    name?: string;
    onBlur?: FocusEventHandler;
    onChange?: (item?: ComboBoxItemType) => void;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    value?: any;
}

const ComboBox = ({disabled, items, name, onBlur, onChange, value}: ComboBoxProps) => {
    const [open, setOpen] = useState(false);

    const commandItems = items.map((comboBoxItem) => (
        <CommandItem
            key={comboBoxItem.value}
            onSelect={() => {
                setOpen(false);

                if (onChange) {
                    onChange(comboBoxItem);
                }
            }}
            value={comboBoxItem.value}
        >
            {comboBoxItem.icon && <InlineSVG className="mr-2 size-6 flex-none" src={comboBoxItem.icon} />}

            {comboBoxItem.label}

            <CheckIcon className={cn('ml-auto size-4', comboBoxItem.value === value ? 'opacity-100' : 'opacity-0')} />
        </CommandItem>
    ));

    const item = items.find((item) => item.value === value);

    return (
        <Popover onOpenChange={setOpen} open={open}>
            <PopoverTrigger asChild onBlur={onBlur}>
                <Button
                    aria-expanded={open}
                    className="w-full justify-between"
                    disabled={disabled}
                    name={name}
                    role="combobox"
                    variant="outline"
                >
                    {value ? (
                        <span className="flex w-full items-center">
                            {item?.icon && <InlineSVG className="mr-2 size-6 flex-none" src={item?.icon} />}

                            {item?.label}
                        </span>
                    ) : (
                        'Select...'
                    )}

                    <CaretSortIcon className="ml-2 size-4 shrink-0 opacity-50" />
                </Button>
            </PopoverTrigger>

            <PopoverPrimitive.Content
                align="start"
                className="z-50 w-72 min-w-combo-box-popper-anchor-width rounded-md border bg-popover p-0 text-popover-foreground shadow-md outline-none data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95 data-[side=bottom]:slide-in-from-top-2 data-[side=left]:slide-in-from-right-2 data-[side=right]:slide-in-from-left-2 data-[side=top]:slide-in-from-bottom-2"
            >
                <Command>
                    <CommandInput className="h-9 border-none ring-0" placeholder="Search..." />

                    <CommandList>
                        <CommandEmpty>No item found.</CommandEmpty>

                        <CommandGroup>{commandItems}</CommandGroup>
                    </CommandList>
                </Command>
            </PopoverPrimitive.Content>
        </Popover>
    );
};

export default ComboBox;
