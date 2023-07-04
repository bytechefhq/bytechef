import * as ToggleGroupPrimitive from '@radix-ui/react-toggle-group';
import {twMerge} from 'tailwind-merge';

export interface IToggleItem {
    label: string;
    value: string;
}

type ToggleGroupProps = {
    containerClassName?: string;
    defaultValue?: string;
    toggleItems: IToggleItem[];
    onValueChange?(value: string): void;
};

const ToggleGroup = ({
    containerClassName,
    defaultValue,
    onValueChange,
    toggleItems,
}: ToggleGroupProps): JSX.Element => (
    <ToggleGroupPrimitive.Root
        aria-label="Items"
        className={twMerge('flex px-2 py-4', containerClassName)}
        defaultValue={defaultValue}
        type="single"
        onValueChange={onValueChange}
    >
        {toggleItems.map(({label, value}) => (
            <ToggleGroupPrimitive.Item
                aria-label={label}
                className="radix-state-on:pointer-events-none radix-state-on:bg-gray-50 border-y border-gray-100 bg-white px-2.5 py-2 first:rounded-l-md first:border-x last:rounded-r-md last:border-x-2 focus:relative focus:outline-none focus-visible:z-20 focus-visible:ring focus-visible:ring-purple-500/75 [&:not(:last-child)]:border-r-transparent"
                key={`group-item-${value}-${label}`}
                value={value}
            >
                {label}
            </ToggleGroupPrimitive.Item>
        ))}
    </ToggleGroupPrimitive.Root>
);

export default ToggleGroup;
