import {
    CheckIcon,
    ChevronDownIcon,
    ChevronUpIcon,
    QuestionMarkCircledIcon,
} from '@radix-ui/react-icons';
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
import Tooltip from 'components/Tooltip/Tooltip';
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
    customValueComponent?: React.ReactNode;
    defaultValue?: string | undefined;
    description?: string;
    label?: string;
    onValueChange?(value: string): void;
    triggerClassName?: string;
    value?: string;
};

const Select = ({
    contentClassName,
    customValueComponent,
    defaultValue,
    description,
    label,
    options,
    onValueChange,
    triggerClassName,
    value,
}: SelectProps): JSX.Element => (
    <fieldset className="w-full">
        {label && (
            <Label
                className={twMerge(
                    'flex items-center text-sm font-medium leading-6 text-gray-900',
                    description && 'space-x-1'
                )}
            >
                <span>{label}</span>

                {description && (
                    <Tooltip text={description}>
                        <QuestionMarkCircledIcon />
                    </Tooltip>
                )}
            </Label>
        )}

        <Root
            defaultValue={defaultValue || options[0].value}
            onValueChange={onValueChange}
            value={value}
        >
            <Trigger asChild aria-label="Select">
                <Button className={triggerClassName} displayType="light">
                    {customValueComponent ? (
                        <Value aria-label={value}>{customValueComponent}</Value>
                    ) : (
                        <Value />
                    )}

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
                            {options.map((option) => (
                                <Item
                                    key={option.value}
                                    value={option.value}
                                    className="radix-disabled:opacity-50 relative cursor-pointer select-none items-center overflow-hidden rounded-md px-8 py-2 text-sm font-medium text-gray-700 focus:bg-gray-100 focus:outline-none dark:text-gray-300 dark:focus:bg-gray-900"
                                >
                                    <ItemIndicator className="absolute left-2 inline-flex items-center">
                                        <CheckIcon />
                                    </ItemIndicator>

                                    <div className="flex flex-col">
                                        <ItemText>{option.label}</ItemText>

                                        {option.description && (
                                            <span
                                                className="mt-1 line-clamp-2 w-full text-xs text-gray-500"
                                                title={option.description}
                                            >
                                                {option.description}
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
