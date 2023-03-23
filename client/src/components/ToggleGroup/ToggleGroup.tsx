import * as ToggleGroupPrimitive from '@radix-ui/react-toggle-group';
import React from 'react';

export interface IToggleItem {
    label: string;
    value: string;
}

type ToggleGroupProps = {
    defaultValue?: string;
    toggleItems: IToggleItem[];
    onValueChange?(value: string): void;
};

const ToggleGroup = ({
    defaultValue,
    toggleItems,
    onValueChange,
}: ToggleGroupProps): JSX.Element => (
    <ToggleGroupPrimitive.Root
        aria-label="Items"
        className="flex px-2 py-4"
        defaultValue={defaultValue}
        type="single"
        onValueChange={onValueChange}
    >
        {toggleItems.map(({label, value}) => (
            <ToggleGroupPrimitive.Item
                aria-label={label}
                className="border-y border-gray-100 bg-white px-2.5 py-2 first:rounded-l-md first:border-x last:rounded-r-md last:border-x-2 focus:relative focus:outline-none focus-visible:z-20 focus-visible:ring focus-visible:ring-purple-500/75 radix-state-on:pointer-events-none radix-state-on:bg-gray-50 dark:border-gray-600 dark:bg-gray-800 dark:radix-state-on:bg-gray-900 [&:not(:last-child)]:border-r-transparent"
                key={`group-item-${value}-${label}`}
                value={value}
            >
                {label}
            </ToggleGroupPrimitive.Item>
        ))}
    </ToggleGroupPrimitive.Root>
);

export default ToggleGroup;
