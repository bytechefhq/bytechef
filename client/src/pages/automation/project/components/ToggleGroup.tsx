import * as ToggleGroupPrimitive from '@radix-ui/react-toggle-group';
import {twMerge} from 'tailwind-merge';

export type ToggleItemType = {
    label: string;
    value: string;
};

interface ToggleGroupProps {
    containerClassName?: string;
    defaultValue?: string;
    onValueChange?(value: string): void;
    toggleItems: Array<ToggleItemType>;
    value?: string;
}

const ToggleGroup = ({containerClassName, defaultValue, onValueChange, toggleItems, value}: ToggleGroupProps) => (
    <ToggleGroupPrimitive.Root
        aria-label="Items"
        className={twMerge('flex shadow-sm', containerClassName)}
        defaultValue={defaultValue}
        onValueChange={onValueChange}
        type="single"
        value={value}
    >
        {toggleItems.map(({label, value}) => (
            <ToggleGroupPrimitive.Item
                aria-label={label}
                className="border-y border-input bg-white px-2.5 py-2 text-sm first:rounded-l-md first:border-x last:rounded-r-md last:border-x-2 focus:relative focus:outline-none focus-visible:z-20 focus-visible:ring-1 focus-visible:ring-ring data-[state=on]:pointer-events-none data-[state=on]:bg-gray-100 [&:not(:last-child)]:border-r-transparent"
                key={`group-item-${value}-${label}`}
                value={value}
            >
                {label}
            </ToggleGroupPrimitive.Item>
        ))}
    </ToggleGroupPrimitive.Root>
);

export default ToggleGroup;
