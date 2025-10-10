import RequiredMark from '@/components/RequiredMark';
import {Label} from '@/components/ui/label';
import {Select, SelectContent, SelectGroup, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {SelectOptionType} from '@/shared/types';
import {Item, ItemIndicator, ItemText, Value} from '@radix-ui/react-select';
import {CheckIcon, CircleQuestionMarkIcon} from 'lucide-react';
import {ReactNode} from 'react';
import {twMerge} from 'tailwind-merge';

import InputTypeSwitchButton from './InputTypeSwitchButton';

interface PropertySelectProps {
    defaultValue?: string;
    deletePropertyButton?: ReactNode;
    description?: string;
    handleInputTypeSwitchButtonClick?: () => void;
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
    deletePropertyButton,
    description,
    handleInputTypeSwitchButtonClick,
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
            <div className="flex w-full items-center justify-between">
                <div className="flex items-center">
                    <Label className={twMerge(description && 'mr-1', 'leading-normal')} htmlFor={name}>
                        <span>{label}</span>

                        {required && <RequiredMark />}
                    </Label>

                    {description && (
                        <Tooltip>
                            <TooltipTrigger>
                                <CircleQuestionMarkIcon className="size-4 text-muted-foreground" />
                            </TooltipTrigger>

                            <TooltipContent>{description}</TooltipContent>
                        </Tooltip>
                    )}
                </div>

                <div className="flex items-center">
                    {showInputTypeSwitchButton && handleInputTypeSwitchButtonClick && (
                        <InputTypeSwitchButton handleClick={handleInputTypeSwitchButtonClick} mentionInput={false} />
                    )}

                    {deletePropertyButton}
                </div>
            </div>
        )}

        {options.length ? (
            <Select defaultValue={defaultValue} name={name} onValueChange={onValueChange} value={value || defaultValue}>
                <SelectTrigger aria-label="Select" className={twMerge(leadingIcon && 'relative', 'pl-4')}>
                    <>
                        {leadingIcon ? (
                            <div>
                                <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center rounded-l-md border-r border-gray-200 bg-gray-100 px-3">
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
                        <SelectItem value="null">Select...</SelectItem>

                        {options.map((option) =>
                            option.description ? (
                                <Item
                                    className={twMerge(
                                        'relative flex w-full cursor-default select-none items-center rounded-sm py-1.5 pl-2 pr-8 text-sm outline-none focus:bg-accent focus:text-accent-foreground data-[disabled]:pointer-events-none data-[disabled]:opacity-50',
                                        option.value === value && 'px-2'
                                    )}
                                    key={`${option.value}_${option.label}`}
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
                                <SelectItem key={`${option.value}_${option.label}`} value={option.value}>
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
