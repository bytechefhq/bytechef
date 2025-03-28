import {MultiSelect, MultiSelectOptionType} from '@/components/MultiSelect';
import RequiredMark from '@/components/RequiredMark';
import {Label} from '@/components/ui/label';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {OptionsDataSource} from '@/shared/middleware/platform/configuration';
import {useGetWorkflowNodeOptionsQuery} from '@/shared/queries/platform/workflowNodeOptions.queries';
import {PropertyAllType} from '@/shared/types';
import {QuestionMarkCircledIcon} from '@radix-ui/react-icons';
import {ReactNode, useEffect, useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';

import useWorkflowNodeDetailsPanelStore from '../../../stores/useWorkflowNodeDetailsPanelStore';
import InputTypeSwitchButton from './InputTypeSwitchButton';

interface PropertyMultiSelectProps {
    defaultValue?: string[];
    deletePropertyButton: ReactNode;
    handleInputTypeSwitchButtonClick?: () => void;
    leadingIcon?: ReactNode;
    lookupDependsOnPaths?: string[];
    lookupDependsOnValues?: string[];
    onChange?: (value: string[]) => void;
    options?: MultiSelectOptionType[];
    optionsDataSource?: OptionsDataSource;
    path?: string;
    property: PropertyAllType;
    showInputTypeSwitchButton: boolean;
    value: string[];
    workflowId: string;
}

const PropertyMultiSelect = ({
    defaultValue,
    deletePropertyButton,
    handleInputTypeSwitchButtonClick,
    leadingIcon,
    lookupDependsOnPaths,
    lookupDependsOnValues,
    onChange,
    options,
    optionsDataSource,
    path,
    property,
    showInputTypeSwitchButton,
    value: initialValue,
    workflowId,
}: PropertyMultiSelectProps) => {
    const [value, setValue] = useState<string[]>(initialValue ?? defaultValue ?? []);

    console.log('value', value);

    const {currentNode} = useWorkflowNodeDetailsPanelStore();
    const {description, label, name, placeholder, required} = property;

    const workflowNodeName = currentNode?.name;

    const connectionRequirementMet = useMemo(() => {
        if (currentNode?.connections?.length || currentNode?.connection) {
            return !!currentNode.connectionId;
        }

        return true;
    }, [currentNode?.connections?.length, currentNode?.connection, currentNode?.connectionId]);

    const queryOptions = useMemo(
        () => ({
            loadDependencyValueKey: (lookupDependsOnValues ?? []).join(''),
            request: {
                id: workflowId,
                lookupDependsOnPaths,
                propertyName: path!,
                workflowNodeName: workflowNodeName!,
            },
        }),
        [lookupDependsOnPaths, lookupDependsOnValues, path, workflowId, workflowNodeName]
    );

    const queryEnabled = useMemo(
        () =>
            !!currentNode &&
            (lookupDependsOnPaths?.length
                ? lookupDependsOnValues?.every((loadDependencyValue) => !!loadDependencyValue)
                : true) &&
            !!connectionRequirementMet &&
            optionsDataSource,
        [connectionRequirementMet, currentNode, lookupDependsOnPaths?.length, lookupDependsOnValues, optionsDataSource]
    );

    const {data: optionsData, isPending: isOptionsDataPending} = useGetWorkflowNodeOptionsQuery(
        queryOptions,
        Boolean(queryEnabled)
    );

    const mappedOptionsData: MultiSelectOptionType[] | undefined = useMemo(() => {
        if (!optionsData) {
            return undefined;
        }
        return optionsData.map((option) => ({
            ...option,
            label: option.label || '',
            value: option.value ?? option.label,
        }));
    }, [optionsData]);

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

        if (options?.length) {
            return false;
        }

        return !lookupDependsOnValues || !hasValidLookupValues;
    }, [lookupDependsOnValues, options]);

    const dependencyMissing = useMemo(
        () => lookupDependsOnPaths?.length && lookupDependsOnValues?.every((value) => value === undefined),
        [lookupDependsOnPaths, lookupDependsOnValues]
    );

    const memoizedPlaceholder = useMemo(() => {
        const conditions = [
            {
                condition: !!missingConnection && !connectionRequirementMet && !options?.length,
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
                condition: lookupDependsOnValues?.length && lookupDependsOnPaths?.length && !options?.length,
                placeholder,
            },
            {
                condition: options?.length,
                placeholder,
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
        options?.length,
        placeholder,
    ]);

    const placeholderClassName = twMerge(
        leadingIcon && 'ml-9',
        (!!(lookupDependsOnValues?.length && !options?.length) || !!missingConnection || !connectionRequirementMet) &&
            'text-destructive',
        options?.length && 'text-normal',
        lookupDependsOnValues?.length && lookupDependsOnPaths?.length && !options?.length && 'text-normal',
        dependencyMissing && 'text-destructive'
    );

    useEffect(() => {
        if (defaultValue) {
            setValue(defaultValue);
        }
    }, [defaultValue]);

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

            <MultiSelect
                defaultValue={value}
                leadingIcon={leadingIcon as ReactNode}
                onValueChange={(value) => {
                    setValue(value);

                    if (onChange) {
                        onChange(value);
                    }
                }}
                options={mappedOptionsData ?? options ?? []}
                optionsLoading={isOptionsDataPending}
                placeholder={memoizedPlaceholder}
                placeholderClassName={placeholderClassName}
                value={value}
            />
        </fieldset>
    );
};

export default PropertyMultiSelect;
