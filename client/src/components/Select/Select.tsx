import {CheckIcon, ChevronDownIcon, ChevronUpIcon} from '@radix-ui/react-icons';
import * as SelectPrimitive from '@radix-ui/react-select';
import React from 'react';
import Button from '../Button/Button';

export interface SelectItem {
    value: string;
    label: string;
}

type SelectProps = {
    defaultValue?: string | undefined;
    selectItems: SelectItem[];
    onValueChange?(value: string): void;
};

const Select: React.FC<SelectProps> = ({
    defaultValue,
    selectItems,
    onValueChange,
}: SelectProps) => {
    return (
        <SelectPrimitive.Root
            defaultValue={defaultValue}
            onValueChange={onValueChange}
        >
            <SelectPrimitive.Trigger asChild aria-label="Select">
                <Button displayType={'light'}>
                    <SelectPrimitive.Value />
                    <SelectPrimitive.Icon className="ml-2">
                        <ChevronDownIcon />
                    </SelectPrimitive.Icon>
                </Button>
            </SelectPrimitive.Trigger>
            <SelectPrimitive.Content>
                <SelectPrimitive.ScrollUpButton className="flex items-center justify-center text-gray-700 dark:text-gray-300">
                    <ChevronUpIcon />
                </SelectPrimitive.ScrollUpButton>
                <SelectPrimitive.Viewport className="rounded-lg bg-white p-2 shadow-lg dark:bg-gray-800">
                    <SelectPrimitive.Group>
                        {selectItems.map((selectItem) => (
                            <SelectPrimitive.Item
                                key={selectItem.value}
                                value={selectItem.value}
                                className="relative flex select-none items-center rounded-md px-8 py-2 text-sm font-medium text-gray-700 focus:bg-gray-100 focus:outline-none radix-disabled:opacity-50 dark:text-gray-300 dark:focus:bg-gray-900"
                            >
                                <SelectPrimitive.ItemText>
                                    {selectItem.label}
                                </SelectPrimitive.ItemText>
                                <SelectPrimitive.ItemIndicator className="absolute left-2 inline-flex items-center">
                                    <CheckIcon />
                                </SelectPrimitive.ItemIndicator>
                            </SelectPrimitive.Item>
                        ))}
                    </SelectPrimitive.Group>
                </SelectPrimitive.Viewport>
                <SelectPrimitive.ScrollDownButton className="flex items-center justify-center text-gray-700 dark:text-gray-300">
                    <ChevronDownIcon />
                </SelectPrimitive.ScrollDownButton>
            </SelectPrimitive.Content>
        </SelectPrimitive.Root>
    );
};

export default Select;
