import LoadingIcon from '@/components/LoadingIcon';
import RequiredMark from '@/components/RequiredMark';
import {Button} from '@/components/ui/button';
import {Command, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList} from '@/components/ui/command';
import {Label} from '@/components/ui/label';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useGetWorkflowNodeOptionsQuery} from '@/shared/queries/platform/workflowNodeOptions.queries';
import {CaretSortIcon, CheckIcon, QuestionMarkCircledIcon} from '@radix-ui/react-icons';
import {FocusEventHandler, ReactNode, useEffect, useMemo, useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {twMerge} from 'tailwind-merge';

import useWorkflowNodeDetailsPanelStore from '../../../stores/useWorkflowNodeDetailsPanelStore';
import InputTypeSwitchButton from './InputTypeSwitchButton';

import type {Option} from '@/shared/middleware/platform/configuration';

type ComboBoxItemType = {
    description?: string;
    icon?: string;
    label: string | ReactNode;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    value: any;
    [key: string]: unknown;
};

interface PropertyComboBoxProps {
    arrayIndex?: number;
    defaultValue?: string;
    deletePropertyButton?: ReactNode;
    description?: string;
    handleInputTypeSwitchButtonClick?: () => void;
    label?: string;
    lookupDependsOnPaths?: Array<string>;
    lookupDependsOnValues?: Array<string>;
    leadingIcon?: ReactNode;
    name?: string;
    onBlur?: FocusEventHandler;
    onValueChange?: (value: string) => void;
    options: Array<Option>;
    path?: string;
    placeholder?: string;
    required?: boolean;
    showInputTypeSwitchButton?: boolean;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    value?: any;
    workflowId: string;
    workflowNodeName: string;
}

const PropertyComboBox = ({
    arrayIndex,
    defaultValue,
    deletePropertyButton,
    description,
    handleInputTypeSwitchButtonClick,
    label,
    leadingIcon,
    lookupDependsOnPaths,
    lookupDependsOnValues,
    name,
    onBlur,
    onValueChange,
    options: initialOptions,
    path: initialPath,
    placeholder = 'Select...',
    required,
    showInputTypeSwitchButton,
    value: initialValue,
    workflowId,
    workflowNodeName,
}: PropertyComboBoxProps) => {
    const [open, setOpen] = useState(false);
    const [value, setValue] = useState(initialValue ?? defaultValue);

    const {currentNode} = useWorkflowNodeDetailsPanelStore();

    const path = useMemo(() => {
        let updatedPath = initialPath;
        if (updatedPath) {
            updatedPath = updatedPath.replace('parameters.', '').replace('parameters', '');

            if (updatedPath.endsWith(`_${arrayIndex}`)) {
                updatedPath = updatedPath.substring(0, updatedPath.lastIndexOf('.')) + `[${arrayIndex}]`;
            }
        } else {
            updatedPath = name;
        }

        return updatedPath;
    }, [initialPath, name, arrayIndex]);

    const connectionRequirementMet = useMemo(
        () => (currentNode?.connections?.length ? !!currentNode.connectionId : true),
        [currentNode]
    );

    const queryOptions = useMemo(
        () => ({
            loadDependencyValueKey: (lookupDependsOnValues ?? []).join(''),
            request: {
                id: workflowId,
                lookupDependsOnPaths,
                propertyName: path!,
                workflowNodeName,
            },
        }),
        [lookupDependsOnValues, workflowId, lookupDependsOnPaths, path, workflowNodeName]
    );

    const queryEnabled = useMemo(
        () =>
            !!currentNode &&
            (lookupDependsOnValues
                ? lookupDependsOnValues.every((loadDependencyValue) => !!loadDependencyValue)
                : false) &&
            !!connectionRequirementMet,
        [currentNode, lookupDependsOnValues, connectionRequirementMet]
    );

    const {data: optionsData, isLoading, isRefetching} = useGetWorkflowNodeOptionsQuery(queryOptions, queryEnabled);

    const options = useMemo(() => {
        if (optionsData) {
            return optionsData.map((option) => ({
                description: option.description,
                label: option.label ?? option.value,
                value: option.value.toString(),
            }));
        }

        return initialOptions;
    }, [optionsData, initialOptions]);

    const currentOption = useMemo(
        () => (options as Array<ComboBoxItemType>)?.find((option) => option.value === value),
        [options, value]
    );

    const missingConnection = useMemo(
        () => currentNode?.connections?.length && !currentNode.connectionId,
        [currentNode]
    );

    const noOptionsAvailable = useMemo(
        () => !lookupDependsOnValues && !options.length && !missingConnection,
        [lookupDependsOnValues, options, missingConnection]
    );

    const memoizedPlaceholder = useMemo(() => {
        if (lookupDependsOnValues?.length && !options.length) {
            return `${lookupDependsOnPaths} is not defined`;
        } else if (missingConnection) {
            return 'Connection missing...';
        }

        return placeholder;
    }, [lookupDependsOnValues?.length, options.length, missingConnection, placeholder, lookupDependsOnPaths]);

    useEffect(() => {
        if (initialValue !== undefined) {
            setValue(initialValue);
        }
    }, [initialValue]);

    return (
        <fieldset className="w-full space-y-1">
            {label && (
                <div className="flex w-full items-center justify-between">
                    <div className="flex items-center">
                        <Label className={twMerge(description && 'mr-1', 'leading-normal')} htmlFor={name}>
                            {label}

                            {required && <RequiredMark />}
                        </Label>

                        {description && (
                            <Tooltip>
                                <TooltipTrigger>
                                    <QuestionMarkCircledIcon />
                                </TooltipTrigger>

                                <TooltipContent>{description}</TooltipContent>
                            </Tooltip>
                        )}
                    </div>

                    <div className="flex items-center">
                        {showInputTypeSwitchButton && handleInputTypeSwitchButtonClick && (
                            <InputTypeSwitchButton
                                handleClick={handleInputTypeSwitchButtonClick}
                                mentionInput={false}
                            />
                        )}

                        {deletePropertyButton}
                    </div>
                </div>
            )}

            <Popover onOpenChange={setOpen} open={open}>
                <PopoverTrigger asChild onBlur={onBlur}>
                    <Button
                        aria-expanded={open}
                        className={twMerge(
                            'relative w-full justify-between whitespace-normal font-normal',
                            showInputTypeSwitchButton && 'mt-0'
                        )}
                        disabled={isRefetching || noOptionsAvailable || !!missingConnection}
                        name={name}
                        role="combobox"
                        variant="outline"
                    >
                        {leadingIcon && (
                            <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center rounded-l-md border-r border-gray-200 bg-gray-100 px-3">
                                {leadingIcon}
                            </div>
                        )}

                        {lookupDependsOnValues && isRefetching && !currentOption?.label && (
                            <span className={twMerge('flex items-center', leadingIcon && 'ml-9')}>
                                <LoadingIcon /> Refetching...
                            </span>
                        )}

                        {lookupDependsOnValues && isLoading && (
                            <span className={twMerge('flex items-center', leadingIcon && 'ml-9')}>
                                <LoadingIcon /> Loading...
                            </span>
                        )}

                        {noOptionsAvailable && (
                            <span className="w-full p-2 text-sm text-muted-foreground">No options available</span>
                        )}

                        {((lookupDependsOnValues && !isLoading) || !lookupDependsOnValues) && (
                            <>
                                {currentOption ? (
                                    <span
                                        className={twMerge(
                                            'flex w-full items-center font-normal',
                                            leadingIcon && 'ml-9'
                                        )}
                                    >
                                        {currentOption?.icon && (
                                            <InlineSVG className="mr-2 size-6 flex-none" src={currentOption?.icon} />
                                        )}

                                        {currentOption?.label}
                                    </span>
                                ) : (
                                    !isRefetching &&
                                    !noOptionsAvailable && (
                                        <span
                                            className={twMerge(
                                                leadingIcon && 'ml-9',
                                                ((lookupDependsOnValues?.length && !options.length) ||
                                                    missingConnection) &&
                                                    'text-destructive'
                                            )}
                                        >
                                            {memoizedPlaceholder}
                                        </span>
                                    )
                                )}
                            </>
                        )}

                        <CaretSortIcon className="ml-2 size-4 shrink-0 opacity-50" />
                    </Button>
                </PopoverTrigger>

                <PopoverContent align="start" className="min-w-combo-box-popper-anchor-width p-0" side="bottom">
                    <Command>
                        <CommandInput className="h-9 border-none ring-0" placeholder="Search..." />

                        <CommandList>
                            <CommandEmpty>No item found.</CommandEmpty>

                            <CommandGroup>
                                <CommandItem
                                    className="cursor-pointer font-normal hover:bg-muted"
                                    key="resetOption"
                                    onSelect={() => {
                                        setOpen(false);

                                        if (onValueChange) {
                                            onValueChange('');
                                        }
                                    }}
                                    value=""
                                >
                                    <span>Select...</span>

                                    {value === '' && <CheckIcon className="ml-auto size-4" />}
                                </CommandItem>

                                {(options as Array<ComboBoxItemType>)?.map((option) => (
                                    <CommandItem
                                        className="cursor-pointer font-normal hover:bg-muted"
                                        key={option.value}
                                        onSelect={() => {
                                            setOpen(false);

                                            setValue(option.value);

                                            if (onValueChange) {
                                                onValueChange(option.value);
                                            }
                                        }}
                                        value={option.value}
                                    >
                                        {option.icon && (
                                            <InlineSVG className="mr-2 size-6 flex-none" src={option.icon} />
                                        )}

                                        {option.description ? (
                                            <div className="flex flex-col gap-1">
                                                <span>{option.label}</span>

                                                <p className="text-xs text-muted-foreground">{option.description}</p>
                                            </div>
                                        ) : (
                                            <span>{option.label}</span>
                                        )}

                                        {option.value === value && <CheckIcon className="ml-auto size-4" />}
                                    </CommandItem>
                                ))}
                            </CommandGroup>
                        </CommandList>
                    </Command>
                </PopoverContent>
            </Popover>
        </fieldset>
    );
};

export default PropertyComboBox;
