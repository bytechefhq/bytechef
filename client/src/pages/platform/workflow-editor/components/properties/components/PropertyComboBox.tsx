import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import RequiredMark from '@/components/RequiredMark';
import {Command, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList} from '@/components/ui/command';
import {Label} from '@/components/ui/label';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {
    useGetClusterElementNodeOptionsQuery,
    useGetWorkflowNodeOptionsQuery,
} from '@/shared/queries/platform/workflowNodeOptions.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {CheckIcon, ChevronsUpDownIcon, CircleQuestionMarkIcon} from 'lucide-react';
import {FocusEventHandler, ReactNode, useCallback, useEffect, useMemo, useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/shallow';

import useWorkflowEditorStore from '../../../stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '../../../stores/useWorkflowNodeDetailsPanelStore';
import getFormattedDependencyKey from '../../../utils/getFormattedDependencyKey';
import InputTypeSwitchButton from './InputTypeSwitchButton';

import type {
    GetClusterElementNodeOptionsRequest,
    GetWorkflowNodeOptionsRequest,
    Option,
    OptionsDataSource,
} from '@/shared/middleware/platform/configuration';

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
    lookupDependsOnValues?: Array<unknown>;
    leadingIcon?: ReactNode;
    name?: string;
    onBlur?: FocusEventHandler;
    onValueChange?: (value: string) => void;
    options: Array<Option>;
    optionsDataSource?: OptionsDataSource;
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
    optionsDataSource,
    path: initialPath,
    placeholder = 'Select...',
    required,
    showInputTypeSwitchButton,
    value: initialValue,
    workflowId,
    workflowNodeName,
}: PropertyComboBoxProps) => {
    const [open, setOpen] = useState(false);
    const [value, setValue] = useState(initialValue !== undefined ? initialValue.toString() : defaultValue);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {currentNode, operationChangeInProgress} = useWorkflowNodeDetailsPanelStore(
        useShallow((state) => ({
            currentNode: state.currentNode,
            operationChangeInProgress: state.operationChangeInProgress,
        }))
    );

    const {rootClusterElementNodeData} = useWorkflowEditorStore(
        useShallow((state) => ({
            rootClusterElementNodeData: state.rootClusterElementNodeData,
        }))
    );

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

    const connectionRequirementMet = useMemo(() => {
        if (currentNode?.connections?.length || currentNode?.connection) {
            return !!currentNode.connectionId;
        }

        return true;
    }, [currentNode?.connections?.length, currentNode?.connection, currentNode?.connectionId]);

    const lookupDependsOnValuesKey = getFormattedDependencyKey(lookupDependsOnValues);

    const queryOptions: {
        loadDependencyValueKey: string;
        request: GetWorkflowNodeOptionsRequest;
    } = useMemo(
        () => ({
            loadDependencyValueKey: lookupDependsOnValuesKey,
            request: {
                environmentId: currentEnvironmentId,
                id: workflowId,
                lookupDependsOnPaths,
                propertyName: path!,
                workflowNodeName,
            },
        }),
        [currentEnvironmentId, lookupDependsOnPaths, lookupDependsOnValuesKey, path, workflowId, workflowNodeName]
    );

    const clusterElementQueryOptions: {
        loadDependencyValueKey: string;
        request: GetClusterElementNodeOptionsRequest;
    } = useMemo(
        () => ({
            loadDependencyValueKey: lookupDependsOnValuesKey,
            request: {
                clusterElementType: currentNode?.clusterElementType || '',
                clusterElementWorkflowNodeName: currentNode?.workflowNodeName || '',
                environmentId: currentEnvironmentId,
                id: workflowId,
                lookupDependsOnPaths,
                propertyName: path!,
                workflowNodeName: rootClusterElementNodeData?.workflowNodeName || '',
            },
        }),
        [
            lookupDependsOnValuesKey,
            currentEnvironmentId,
            currentNode?.clusterElementType,
            currentNode?.workflowNodeName,
            workflowId,
            lookupDependsOnPaths,
            path,
            rootClusterElementNodeData?.workflowNodeName,
        ]
    );

    const queryEnabled = useMemo(
        () =>
            !!currentNode &&
            (lookupDependsOnPaths?.length
                ? lookupDependsOnValues?.every((loadDependencyValue) => !!loadDependencyValue)
                : true) &&
            !!connectionRequirementMet &&
            optionsDataSource &&
            !operationChangeInProgress,
        [
            connectionRequirementMet,
            currentNode,
            lookupDependsOnPaths?.length,
            lookupDependsOnValues,
            operationChangeInProgress,
            optionsDataSource,
        ]
    );

    const {
        data: optionsData,
        isLoading,
        isRefetching,
    } = useGetWorkflowNodeOptionsQuery(queryOptions, Boolean(queryEnabled && !currentNode?.clusterElementType));

    const {
        data: clusterElementOptionsData,
        isLoading: isClusterElementOptionsLoading,
        isRefetching: isClusterElementOptionsRefetching,
    } = useGetClusterElementNodeOptionsQuery(
        clusterElementQueryOptions,
        Boolean(queryEnabled && currentNode?.clusterElementType)
    );

    const options = useMemo(() => {
        if (optionsData) {
            return optionsData.map((option) => ({
                description: option.description,
                label: option.label ?? option.value,
                value: option.value.toString(),
            }));
        } else if (clusterElementOptionsData) {
            return clusterElementOptionsData.map((option) => ({
                description: option.description,
                label: option.label ?? option.value,
                value: option.value.toString(),
            }));
        }

        return initialOptions.map((option) => ({
            ...option,
            value: option.value?.toString() || '',
        }));
    }, [optionsData, clusterElementOptionsData, initialOptions]);

    const currentOption = useMemo(
        () => (options as Array<ComboBoxItemType>)?.find((option) => String(option.value) === String(value)),
        [options, value]
    );

    const missingConnection = useMemo(
        () =>
            currentNode?.connections?.length &&
            !currentNode.connectionId &&
            lookupDependsOnPaths?.length === 0 &&
            lookupDependsOnValues?.length === 0,
        [currentNode?.connectionId, currentNode?.connections?.length, lookupDependsOnPaths, lookupDependsOnValues]
    );

    const noOptionsAvailable = useMemo(() => {
        const hasValidLookupValues = lookupDependsOnValues?.every((value) => value !== undefined);

        if (options.length) {
            return false;
        }

        return !lookupDependsOnValues || !hasValidLookupValues;
    }, [lookupDependsOnValues, options]);

    const dependencyMissing = useMemo(
        () => lookupDependsOnPaths?.length && lookupDependsOnValues?.some((value) => value === undefined),
        [lookupDependsOnPaths, lookupDependsOnValues]
    );

    const memoizedPlaceholder = useMemo(() => {
        const conditions = [
            {
                condition: !!missingConnection && !connectionRequirementMet && !options.length,
                placeholder: 'Connection missing...',
            },
            {
                condition: !missingConnection && dependencyMissing,
                placeholder: `${lookupDependsOnPaths} is not defined`,
            },
            {
                condition: !!missingConnection && (!lookupDependsOnValues?.length || !lookupDependsOnPaths?.length),
                placeholder: `${lookupDependsOnPaths} is not defined`,
            },
            {
                condition: lookupDependsOnValues?.length && lookupDependsOnPaths?.length && !options.length,
                placeholder: placeholder,
            },
            {
                condition: options.length,
                placeholder: placeholder,
            },
            {
                condition: noOptionsAvailable,
                placeholder: 'No options available',
            },
        ];

        const matchingCondition = conditions.find(({condition}) => condition);

        return matchingCondition ? matchingCondition.placeholder : placeholder;
    }, [
        connectionRequirementMet,
        dependencyMissing,
        lookupDependsOnPaths,
        lookupDependsOnValues?.length,
        missingConnection,
        noOptionsAvailable,
        options.length,
        placeholder,
    ]);

    const placeholderClassName = twMerge(
        leadingIcon && 'ml-9',
        (!!(lookupDependsOnValues?.length && !options.length) || !!missingConnection || !connectionRequirementMet) &&
            'text-destructive',
        options.length && 'text-normal',
        lookupDependsOnValues?.length && lookupDependsOnPaths?.length && !options.length && 'text-normal',
        dependencyMissing && 'text-destructive'
    );

    const handleValueChange = useCallback(
        (value: string) => {
            setOpen(false);

            setValue(value);

            if (onValueChange) {
                onValueChange(value);
            }
        },
        [onValueChange]
    );

    useEffect(() => {
        if (initialValue !== undefined) {
            setValue(initialValue.toString());
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
                                    <CircleQuestionMarkIcon className="size-4 text-muted-foreground" />
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

            <Popover modal onOpenChange={setOpen} open={open}>
                <PopoverTrigger asChild onBlur={onBlur}>
                    <Button
                        aria-expanded={open}
                        className={twMerge(
                            'relative w-full justify-between whitespace-normal font-normal',
                            showInputTypeSwitchButton && 'mt-0'
                        )}
                        disabled={
                            !options.length &&
                            (isRefetching ||
                                isClusterElementOptionsRefetching ||
                                noOptionsAvailable ||
                                !!missingConnection ||
                                !connectionRequirementMet)
                        }
                        name={name}
                        role="combobox"
                        variant="outline"
                    >
                        {leadingIcon && (
                            <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center rounded-l-md border-r border-gray-200 bg-gray-100 px-3">
                                {leadingIcon}
                            </div>
                        )}

                        {lookupDependsOnValues &&
                            (isRefetching || isClusterElementOptionsRefetching) &&
                            !currentOption?.label && (
                                <span className={twMerge('flex items-center', leadingIcon && 'ml-9')}>
                                    <LoadingIcon /> Refetching...
                                </span>
                            )}

                        {lookupDependsOnValues && (isLoading || isClusterElementOptionsLoading) && (
                            <span className={twMerge('flex items-center', leadingIcon && 'ml-9')}>
                                <LoadingIcon /> Loading...
                            </span>
                        )}

                        {((lookupDependsOnValues && !(isLoading || isClusterElementOptionsLoading)) ||
                            !lookupDependsOnValues) && (
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
                                    !(isRefetching || isClusterElementOptionsRefetching) && (
                                        <span className={placeholderClassName}>{memoizedPlaceholder}</span>
                                    )
                                )}
                            </>
                        )}

                        <ChevronsUpDownIcon className="ml-2 size-4 shrink-0 opacity-50" />
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

                                {(options as Array<ComboBoxItemType>)?.map((option) => {
                                    const labelAndDescription = [
                                        typeof option.label === 'string' ? option.label : undefined,
                                        option.description,
                                    ];

                                    const nonEmptyStrings = labelAndDescription.filter(
                                        (value): value is string => typeof value === 'string' && value.length > 0
                                    );

                                    return (
                                        <CommandItem
                                            className="cursor-pointer font-normal hover:bg-muted"
                                            key={option.value.toString()}
                                            keywords={nonEmptyStrings}
                                            onSelect={() => handleValueChange(option.value.toString())}
                                            value={option.value.toString()}
                                        >
                                            {option.icon && (
                                                <InlineSVG className="mr-2 size-6 flex-none" src={option.icon} />
                                            )}

                                            {option.description ? (
                                                <div className="flex flex-col gap-1">
                                                    <span>{option.label}</span>

                                                    <p className="text-xs text-muted-foreground">
                                                        {option.description}
                                                    </p>
                                                </div>
                                            ) : (
                                                <span>{option.label}</span>
                                            )}

                                            {option.value === value && <CheckIcon className="ml-auto size-4" />}
                                        </CommandItem>
                                    );
                                })}
                            </CommandGroup>
                        </CommandList>
                    </Command>
                </PopoverContent>
            </Popover>
        </fieldset>
    );
};

export default PropertyComboBox;
