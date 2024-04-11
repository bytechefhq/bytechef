import LoadingIcon from '@/components/LoadingIcon';
import {Button} from '@/components/ui/button';
import {Command, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList} from '@/components/ui/command';
import {Label} from '@/components/ui/label';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import useWorkflowDataStore from '@/pages/automation/project/stores/useWorkflowDataStore';
import {useWorkflowNodeDetailsPanelStore} from '@/pages/automation/project/stores/useWorkflowNodeDetailsPanelStore';
import {useGetWorkflowNodeOptionsQuery} from '@/queries/platform/workflowNodeOptions.queries';
import {CaretSortIcon, CheckIcon, QuestionMarkCircledIcon} from '@radix-ui/react-icons';
import {FocusEventHandler, ReactNode, useEffect, useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {twMerge} from 'tailwind-merge';

import type {OptionModel, OptionsDataSourceModel} from '@/middleware/platform/configuration';

type ComboBoxItemType = {
    description?: string;
    icon?: string;
    label: string | ReactNode;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    value: any;
    [key: string]: unknown;
};

interface PropertyComboBoxProps {
    description?: string;
    label?: string;
    loadDependency?: any;
    leadingIcon?: ReactNode;
    name?: string;
    onBlur?: FocusEventHandler;
    onValueChange?: (value: string) => void;
    options: Array<OptionModel>;
    optionsDataSource?: OptionsDataSourceModel;
    placeholder?: string;
    required?: boolean;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    value?: any;
}

const PropertyComboBox = ({
    description,
    label,
    leadingIcon,
    loadDependency,
    name,
    onBlur,
    onValueChange,
    options,
    optionsDataSource,
    placeholder = 'Select...',
    required,
    value,
}: PropertyComboBoxProps) => {
    const [open, setOpen] = useState(false);
    const [loadDependencyValues, setLoadDependencyValues] = useState<Array<string>>(
        Object.values(loadDependency ?? {})
    );

    const {workflow} = useWorkflowDataStore();
    const {currentNode} = useWorkflowNodeDetailsPanelStore();

    const {
        data: optionsData,
        isLoading,
        isRefetching,
        refetch,
    } = useGetWorkflowNodeOptionsQuery(
        {
            id: workflow.id!,
            propertyName: name!,
            workflowNodeName: currentNode.name!,
        },
        !!optionsDataSource &&
            loadDependencyValues.reduce(
                (enabled: boolean, loadDependencyValue: string) => enabled && loadDependencyValue !== undefined,
                true
            )
    );

    if (optionsData) {
        options = optionsData.map((option) => ({
            description: option.description,
            label: option.label ?? option.value,
            value: option.value.toString(),
        }));
    }

    const currentOption = (options as Array<ComboBoxItemType>)?.find((option) => option.value === value);

    if (loadDependencyValues?.length && !options.length) {
        placeholder = `${Object.keys(loadDependency)} is not defined`;
    }

    useEffect(() => {
        if (loadDependency && typeof loadDependency === 'object') {
            setLoadDependencyValues(Object.values(loadDependency));
        }
    }, [loadDependency]);

    const loadDependencyValuesString = loadDependencyValues.join('');

    useEffect(() => {
        if (loadDependencyValuesString?.length) {
            refetch();
        }
    }, [loadDependencyValuesString, options.length, refetch]);

    return (
        <fieldset className="w-full space-y-2">
            {label && (
                <div className="flex items-center">
                    <Label className={twMerge(description && 'mr-1', 'leading-normal')} htmlFor={name}>
                        {label}

                        {required && <span className="leading-3 text-red-500">*</span>}
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
            )}

            <Popover onOpenChange={setOpen} open={open}>
                <PopoverTrigger asChild onBlur={onBlur}>
                    <Button
                        aria-expanded={open}
                        className={twMerge('relative w-full justify-between', leadingIcon && 'relative')}
                        disabled={isRefetching}
                        name={name}
                        role="combobox"
                        variant="outline"
                    >
                        {leadingIcon && (
                            <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center rounded-l-md border-r border-gray-300 bg-gray-100 px-3">
                                {leadingIcon}
                            </div>
                        )}

                        {optionsDataSource && loadDependency && isRefetching && !currentOption?.label && (
                            <span className={twMerge('flex items-center', leadingIcon && 'ml-9')}>
                                <LoadingIcon /> Refetching...
                            </span>
                        )}

                        {optionsDataSource && isLoading && (
                            <span className={twMerge('flex items-center', leadingIcon && 'ml-9')}>
                                <LoadingIcon /> Loading...
                            </span>
                        )}

                        {!optionsDataSource && !options.length && (
                            <span className="rounded-md border p-2 text-sm text-muted-foreground">
                                No options available
                            </span>
                        )}

                        {((optionsDataSource && !isLoading) || !optionsDataSource) && (
                            <>
                                {currentOption ? (
                                    <span className={twMerge('flex w-full items-center', leadingIcon && 'ml-9')}>
                                        {currentOption?.icon && (
                                            <InlineSVG className="mr-2 size-6 flex-none" src={currentOption?.icon} />
                                        )}

                                        {currentOption?.label}
                                    </span>
                                ) : (
                                    <span
                                        className={twMerge(
                                            leadingIcon && 'ml-9',
                                            loadDependencyValues?.length && !options.length && 'text-red-600'
                                        )}
                                    >
                                        {placeholder}
                                    </span>
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
                                {(options as Array<ComboBoxItemType>)?.map((option) => (
                                    <CommandItem
                                        key={option.value}
                                        onSelect={() => {
                                            setOpen(false);

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
                                                <div>{option.label}</div>

                                                <div className="text-xs text-muted-foreground">
                                                    {option.description}
                                                </div>
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
