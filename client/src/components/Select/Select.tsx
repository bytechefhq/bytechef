import {CheckIcon, ChevronDownIcon, ChevronUpIcon} from '@radix-ui/react-icons';
import {Label} from '@radix-ui/react-label';
import {
    Content,
    Group,
    Icon,
    Item,
    ItemIndicator,
    ItemText,
    Portal,
    Root,
    ScrollDownButton,
    ScrollUpButton,
    Trigger,
    Value,
    Viewport,
} from '@radix-ui/react-select';
import {twMerge} from 'tailwind-merge';

import Button from '../Button/Button';

export interface ISelectOption {
    label: string;
    value: string;
    description?: string;
}

type SelectProps = {
    options: ISelectOption[];
    contentClassName?: string;
    defaultValue?: string | undefined;
    label?: string;
    onValueChange?(value: string): void;
    triggerClassName?: string;
    value?: string;
};

const Select = ({
    contentClassName,
    defaultValue,
    label,
    options,
    onValueChange,
    triggerClassName,
    value,
}: SelectProps): JSX.Element => (
    <fieldset className="w-full">
        {label && (
            <Label className="block px-2 text-sm font-medium leading-6 text-gray-900">
                {label}
            </Label>
        )}

        <Root
            defaultValue={defaultValue || options[0].value}
            onValueChange={onValueChange}
            value={value}
        >
            <Trigger asChild aria-label="Select">
                <Button className={triggerClassName} displayType="light">
                    <Value />

                    <Icon className="ml-auto pl-2">
                        <ChevronDownIcon />
                    </Icon>
                </Button>
            </Trigger>

            <Portal className="z-20">
                <Content
                    className={twMerge(
                        'max-h-select-content-available-height min-w-select-trigger-width',
                        contentClassName
                    )}
                    position="popper"
                    sideOffset={5}
                >
                    <ScrollUpButton className="flex items-center justify-center text-gray-700 dark:text-gray-300">
                        <ChevronUpIcon />
                    </ScrollUpButton>

                    <Viewport className="rounded-lg border border-gray-100 bg-white p-2 shadow-lg dark:bg-gray-800">
                        <Group>
                            {options.map((selectItem) => (
                                <Item
                                    key={selectItem.value}
                                    value={selectItem.value}
                                    // eslint-disable-next-line tailwindcss/classnames-order
                                    className="relative flex cursor-pointer select-none items-center rounded-md px-8 py-2 text-sm font-medium text-gray-700 focus:bg-gray-100 focus:outline-none radix-disabled:opacity-50 dark:text-gray-300 dark:focus:bg-gray-900"
                                >
                                    <ItemIndicator className="absolute left-2 inline-flex items-center">
                                        <CheckIcon />
                                    </ItemIndicator>

                                    <div className="flex flex-col">
                                        <ItemText>{selectItem.label}</ItemText>

                                        {selectItem.description && (
                                            // eslint-disable-next-line tailwindcss/no-custom-classname
                                            <span
                                                className="mt-1 line-clamp-2 text-xs text-gray-500"
                                                title={selectItem.description}
                                            >
                                                {selectItem.description}
                                            </span>
                                        )}
                                    </div>
                                </Item>
                            ))}
                        </Group>
                    </Viewport>

                    <ScrollDownButton className="flex items-center justify-center text-gray-700 dark:text-gray-300">
                        <ChevronDownIcon />
                    </ScrollDownButton>
                </Content>
            </Portal>
        </Root>
    </fieldset>
);

export default Select;
