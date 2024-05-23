import {Label} from '@/components/ui/label';
import {Select, SelectContent, SelectGroup, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {CheckIcon, QuestionMarkCircledIcon} from '@radix-ui/react-icons';
import {Item, ItemIndicator, ItemText, Value} from '@radix-ui/react-select';
import {ReactNode} from 'react';
import {twMerge} from 'tailwind-merge';

import InputTypeSwitchButton from './InputTypeSwitchButton';

export type SelectOptionType = {
    description?: string;
    label: string;
    value: string;
};

interface PropertySelectProps {
    defaultValue?: string;
    description?: string;
    handleInputTypeSwitchButtonClick?: () => void;
    inputTypeSwitchButtonClassName?: string;
    label?: string;
    leadingIcon?: ReactNode;
    name?: string;
    onValueChange?: (value: string) => void;
    options: Array<SelectOptionType>;
    placeholder?: string;
    required?: boolean;
    showInputTypeSwitchButton?: boolean;
    value?: string;
}

const PropertySelect = ({
    defaultValue,
    description,
    handleInputTypeSwitchButtonClick,
    inputTypeSwitchButtonClassName,
    label,
    leadingIcon,
    name,
    onValueChange,
    options,
    placeholder = 'Select...',
    required,
    showInputTypeSwitchButton,
    value,
}: PropertySelectProps) => (
    <fieldset className="w-full space-y-1">
        {label && (
            <div className="flex items-center">
                <Label className={twMerge(description && 'mr-1', 'leading-normal')} htmlFor={name}>
                    <span>{label}</span>

                    {required && <span className="ml-0.5 leading-3 text-red-500">*</span>}
                </Label>

                {description && (
                    <Tooltip>
                        <TooltipTrigger>
                            <QuestionMarkCircledIcon />
                        </TooltipTrigger>

                        <TooltipContent>{description}</TooltipContent>
                    </Tooltip>
                )}

                {showInputTypeSwitchButton && handleInputTypeSwitchButtonClick && (
                    <InputTypeSwitchButton
                        className={twMerge('ml-auto', inputTypeSwitchButtonClassName)}
                        handleClick={handleInputTypeSwitchButtonClick}
                        mentionInput={false}
                    />
                )}
            </div>
        )}

        {options.length ? (
            <Select defaultValue={defaultValue} name={name} onValueChange={onValueChange} value={value}>
                <SelectTrigger aria-label="Select" className={twMerge(leadingIcon && 'relative', 'pl-4')}>
                    <>
                        {leadingIcon ? (
                            <div>
                                <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center rounded-l-md border-r border-gray-300 bg-gray-100 px-3">
                                    {leadingIcon}
                                </div>

                                <div className="ml-9">
                                    <Value placeholder={placeholder} />
                                </div>
                            </div>
                        ) : (
                            <SelectValue placeholder={placeholder} />
                        )}
                    </>
                </SelectTrigger>

                <SelectContent
                    align="start"
                    className="max-h-select-content-available-height min-w-select-trigger-width"
                    position="popper"
                    sideOffset={5}
                >
                    <SelectGroup>
                        {options.map((option) =>
                            option.description ? (
                                <Item
                                    className={twMerge(
                                        'relative flex w-full cursor-default select-none items-center rounded-sm py-1.5 pl-2 pr-8 text-sm outline-none focus:bg-accent focus:text-accent-foreground data-[disabled]:pointer-events-none data-[disabled]:opacity-50',
                                        option.value === value && 'px-2'
                                    )}
                                    key={option.value}
                                    value={option.value}
                                >
                                    <span className="absolute right-2 flex size-3.5 items-center justify-center">
                                        <ItemIndicator>
                                            <CheckIcon className="size-4" />
                                        </ItemIndicator>
                                    </span>

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
                            ) : (
                                <SelectItem key={option.value} value={option.value}>
                                    {option.label}
                                </SelectItem>
                            )
                        )}
                    </SelectGroup>
                </SelectContent>
            </Select>
        ) : (
            <div className="rounded-md border p-2 text-sm text-muted-foreground">No options available</div>
        )}
    </fieldset>
);

export default PropertySelect;
