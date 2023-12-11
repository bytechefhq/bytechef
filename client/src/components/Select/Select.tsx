import {Button} from '@/components/ui/button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {CheckIcon, ChevronDownIcon, ChevronUpIcon, QuestionMarkCircledIcon} from '@radix-ui/react-icons';
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
import {ReactNode} from 'react';
import {twMerge} from 'tailwind-merge';

export interface ISelectOption {
    label: string;
    value: string;
    description?: string;
}

type SelectProps = {
    options: ISelectOption[];
    contentClassName?: string;
    defaultValue?: string | undefined;
    description?: string;
    fieldsetClassName?: string;
    label?: string;
    leadingIcon?: ReactNode;
    name?: string;
    onValueChange?(value: string): void;
    placeholder?: string;
    triggerClassName?: string;
    value?: string;
};

const Select = ({
    contentClassName,
    defaultValue,
    description,
    fieldsetClassName,
    label,
    leadingIcon,
    name,
    onValueChange,
    options,
    placeholder = 'Select...',
    triggerClassName,
    value,
}: SelectProps) => (
    <fieldset className={twMerge('w-full', fieldsetClassName)}>
        {label && (
            <Label className={twMerge('flex items-center text-sm font-medium', description && 'space-x-1')}>
                <span>{label}</span>

                {description && (
                    <Tooltip>
                        <TooltipTrigger>
                            <QuestionMarkCircledIcon />
                        </TooltipTrigger>

                        <TooltipContent>{description}</TooltipContent>
                    </Tooltip>
                )}
            </Label>
        )}

        <Root defaultValue={defaultValue} name={name} onValueChange={onValueChange} value={value || defaultValue}>
            <Trigger aria-label="Select" asChild>
                <Button
                    className={twMerge('mt-1', leadingIcon && 'relative pl-12 h-9.5', triggerClassName)}
                    variant="ghost"
                >
                    {leadingIcon && (
                        <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center rounded-l-md border-r border-gray-300 bg-gray-100 px-3">
                            {leadingIcon}
                        </div>
                    )}

                    {options.length ? (
                        <>
                            <Value placeholder={placeholder} />

                            <Icon className="ml-auto pl-2">
                                <ChevronDownIcon />
                            </Icon>
                        </>
                    ) : (
                        <span>No options available</span>
                    )}
                </Button>
            </Trigger>

            <Portal>
                <Content
                    align="start"
                    className={twMerge(
                        'flex max-h-select-content-available-height min-w-select-trigger-width z-50',
                        contentClassName
                    )}
                    position="popper"
                    sideOffset={5}
                >
                    <ScrollUpButton className="flex items-center justify-center text-gray-700">
                        <ChevronUpIcon />
                    </ScrollUpButton>

                    <Viewport className="rounded-lg border border-gray-100 bg-white p-2 shadow-lg">
                        <Group>
                            {options.map((option) => (
                                <Item
                                    className={twMerge(
                                        'radix-disabled:opacity-50 flex cursor-pointer select-none items-center overflow-hidden rounded-md px-8 py-2 text-sm font-medium text-gray-700 focus:bg-gray-100 focus:outline-none',
                                        option.value === (value || defaultValue) && 'px-2'
                                    )}
                                    key={option.value}
                                    value={option.value}
                                >
                                    <ItemIndicator className="inline-flex items-center pl-0 pr-2">
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

                    <ScrollDownButton className="flex items-center justify-center text-gray-700">
                        <ChevronDownIcon />
                    </ScrollDownButton>
                </Content>
            </Portal>
        </Root>
    </fieldset>
);

export default Select;
