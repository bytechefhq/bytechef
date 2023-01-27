import React from 'react';
import * as ToggleGroupPrimitive from '@radix-ui/react-toggle-group';

export interface ToggleItem {
    value: string;
    label: string;
}

type ToggleGroupProps = {
    defaultValue?: string;
    toggleItems: ToggleItem[];
    onValueChange?(value: string): void;
};

const ToggleGroup: React.FC<ToggleGroupProps> = ({
    defaultValue,
    toggleItems,
    onValueChange,
}: ToggleGroupProps) => (
    <ToggleGroupPrimitive.Root
        type="single"
        aria-label="Items"
        defaultValue={defaultValue}
        onValueChange={onValueChange}
        className="flex px-2 py-4 hover:cursor-pointer"
    >
        {toggleItems.map(({value, label}) => (
            <ToggleGroupPrimitive.Item
                key={`group-item-${value}-${label}`}
                value={value}
                aria-label={label}
                className="group w-full border border-solid border-white bg-white px-2.5 py-2 text-center text-sm font-medium text-gray-600 first:rounded-l-md last:rounded-r-md hover:border-gray-100 hover:bg-gray-100 focus:relative focus:outline-none focus-visible:z-20 focus-visible:ring focus-visible:ring-blue-500/75 radix-state-on:border-gray-100 radix-state-on:bg-gray-100 radix-state-on:text-gray-900 dark:border-gray-600 dark:bg-gray-800 dark:radix-state-on:border-transparent dark:radix-state-on:bg-gray-900"
            >
                {label}
            </ToggleGroupPrimitive.Item>
        ))}
    </ToggleGroupPrimitive.Root>
);

export default ToggleGroup;
