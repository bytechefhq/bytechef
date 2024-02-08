import {Button} from '@/components/ui/button';
import {Command, CommandEmpty, CommandGroup, CommandInput, CommandItem} from '@/components/ui/command';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {ScrollArea} from '@/components/ui/scroll-area';
import {cn} from '@/lib/utils';
import {CaretSortIcon, CheckIcon} from '@radix-ui/react-icons';
import {FocusEventHandler, ReactNode, useState} from 'react';
import InlineSVG from 'react-inlinesvg';

export type ComboBoxItemType = {
    icon?: string;
    label: string | ReactNode;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    value: any;
    [key: string]: unknown;
};

export type ComboBoxProps = {
    disabled?: boolean;
    items: ComboBoxItemType[];
    maxHeight?: boolean;
    name?: string;
    onBlur?: FocusEventHandler;
    onChange: (item?: ComboBoxItemType) => void;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    value?: any;
};

const ComboBox = ({disabled, items, maxHeight = false, name, onBlur, onChange, value}: ComboBoxProps) => {
    const [open, setOpen] = useState(false);

    const commandItems = items.map((comboBoxItem) => (
        <CommandItem
            key={comboBoxItem.value}
            onSelect={() => {
                setOpen(false);
                onChange(comboBoxItem);
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

            <PopoverContent align="start" className="min-w-combo-box-popper-anchor-width p-0">
                <Command>
                    <CommandInput className="h-9 border-none ring-0" placeholder="Search..." />

                    <CommandEmpty>No item found.</CommandEmpty>

                    <CommandGroup>
                        {maxHeight ? <ScrollArea className="h-72 w-full">{commandItems}</ScrollArea> : commandItems}
                    </CommandGroup>
                </Command>
            </PopoverContent>
        </Popover>
    );
};

export default ComboBox;
