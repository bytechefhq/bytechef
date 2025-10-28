import {MultiSelect, MultiSelectOptionType} from '@/components/MultiSelect/MultiSelect';
import RequiredMark from '@/components/RequiredMark';
import {Label} from '@/components/ui/label';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useEnvironmentStore} from '@/pages/automation/stores/useEnvironmentStore';
import {GetClusterElementNodeOptionsRequest, OptionsDataSource} from '@/shared/middleware/platform/configuration';
import {
    useGetClusterElementNodeOptionsQuery,
    useGetWorkflowNodeOptionsQuery,
} from '@/shared/queries/platform/workflowNodeOptions.queries';
import {PropertyAllType} from '@/shared/types';
import {CircleQuestionMarkIcon} from 'lucide-react';
import {ReactNode, useMemo} from 'react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/shallow';

import useWorkflowEditorStore from '../../../stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '../../../stores/useWorkflowNodeDetailsPanelStore';
import getFormattedDependencyKey from '../../../utils/getFormattedDependencyKey';
import InputTypeSwitchButton from './InputTypeSwitchButton';

interface PropertyMultiSelectProps {
    defaultValue?: string[];
    deletePropertyButton: ReactNode;
    handleInputTypeSwitchButtonClick?: () => void;
    leadingIcon?: ReactNode;
    lookupDependsOnPaths?: Array<string>;
    lookupDependsOnValues?: Array<unknown>;
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
    value,
    workflowId,
}: PropertyMultiSelectProps) => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentNode = useWorkflowNodeDetailsPanelStore((state) => state.currentNode);
    const {rootClusterElementNodeData} = useWorkflowEditorStore(
        useShallow((state) => ({
            rootClusterElementNodeData: state.rootClusterElementNodeData,
        }))
    );

    const {description, label, name, placeholder, required} = property;
    const workflowNodeName = currentNode?.name;

    const connectionRequirementMet = useMemo(() => {
        if (currentNode?.connections?.length || currentNode?.connection) {
            return !!currentNode.connectionId;
        }

        return true;
    }, [currentNode?.connections?.length, currentNode?.connection, currentNode?.connectionId]);

    const lookupDependsOnValuesKey = getFormattedDependencyKey(lookupDependsOnValues);

    const queryOptions = useMemo(
        () => ({
            loadDependencyValueKey: lookupDependsOnValuesKey,
            request: {
                environmentId: currentEnvironmentId,
                id: workflowId,
                lookupDependsOnPaths,
                propertyName: path!,
                workflowNodeName: workflowNodeName!,
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
            optionsDataSource,
        [connectionRequirementMet, currentNode, lookupDependsOnPaths?.length, lookupDependsOnValues, optionsDataSource]
    );

    const {data: optionsData, isPending: isOptionsDataPending} = useGetWorkflowNodeOptionsQuery(
        queryOptions,
        Boolean(queryEnabled && !currentNode?.clusterElementType)
    );

    const {data: clusterElementOptionsData, isPending: isClusterElementOptionsDataPending} =
        useGetClusterElementNodeOptionsQuery(
            clusterElementQueryOptions,
            Boolean(queryEnabled && currentNode?.clusterElementType)
        );

    const mappedOptionsData: MultiSelectOptionType[] | undefined = useMemo(() => {
        if (clusterElementOptionsData) {
            return clusterElementOptionsData.map((option) => ({
                ...option,
                label: option.label || '',
                value: option.value ?? option.label,
            }));
        }

        if (optionsData) {
            return optionsData.map((option) => ({
                ...option,
                label: option.label || '',
                value: option.value ?? option.label,
            }));
        }

        return undefined;
    }, [optionsData, clusterElementOptionsData]);

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

            <MultiSelect
                defaultValue={value ?? defaultValue}
                leadingIcon={leadingIcon as ReactNode}
                onValueChange={(value) => {
                    if (onChange) {
                        onChange(value);
                    }
                }}
                options={mappedOptionsData ?? options ?? []}
                optionsLoading={(isOptionsDataPending || isClusterElementOptionsDataPending) && !!queryEnabled}
                placeholder={memoizedPlaceholder}
                placeholderClassName={placeholderClassName}
                value={value}
            />
        </fieldset>
    );
};

export default PropertyMultiSelect;
