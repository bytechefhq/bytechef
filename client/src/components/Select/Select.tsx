import {CheckIcon, ChevronDownIcon, ChevronUpIcon} from '@radix-ui/react-icons';
import {
    Content,
    Group,
    Icon,
    Item,
    ItemIndicator,
    ItemText,
    Root,
    ScrollDownButton,
    ScrollUpButton,
    Trigger,
    Value,
    Viewport,
} from '@radix-ui/react-select';
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
        <Root defaultValue={defaultValue} onValueChange={onValueChange}>
            <Trigger asChild aria-label="Select">
                <Button displayType="light">
                    <Value />

                    <Icon className="ml-2">
                        <ChevronDownIcon />
                    </Icon>
                </Button>
            </Trigger>

            <Content>
                <ScrollUpButton className="flex items-center justify-center text-gray-700 dark:text-gray-300">
                    <ChevronUpIcon />
                </ScrollUpButton>

                <Viewport className="rounded-lg bg-white p-2 shadow-lg dark:bg-gray-800">
                    <Group>
                        {selectItems.map((selectItem) => (
                            <Item
                                key={selectItem.value}
                                value={selectItem.value}
                                className="radix-disabled:opacity-50 relative flex select-none items-center rounded-md px-8 py-2 text-sm font-medium text-gray-700 focus:bg-gray-100 focus:outline-none dark:text-gray-300 dark:focus:bg-gray-900"
                            >
                                <ItemText>{selectItem.label}</ItemText>

                                <ItemIndicator className="absolute left-2 inline-flex items-center">
                                    <CheckIcon />
                                </ItemIndicator>
                            </Item>
                        ))}
                    </Group>
                </Viewport>

                <ScrollDownButton className="flex items-center justify-center text-gray-700 dark:text-gray-300">
                    <ChevronDownIcon />
                </ScrollDownButton>
            </Content>
        </Root>
    );
};

export default Select;
