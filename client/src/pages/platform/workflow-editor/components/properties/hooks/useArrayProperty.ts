import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {VALUE_PROPERTY_CONTROL_TYPES} from '@/shared/constants';
import {ControlType, ObjectProperty, PropertyType} from '@/shared/middleware/platform/configuration';
import {ArrayPropertyType, ComponentType, PropertyAllType} from '@/shared/types';
import resolvePath from 'object-resolve-path';
import {Dispatch, SetStateAction, useCallback, useEffect, useMemo, useState} from 'react';

import useWorkflowDataStore from '../../../stores/useWorkflowDataStore';
import {decodePath, encodeParameters, encodePath} from '../../../utils/encodingUtils';
import getParameterItemType from '../../../utils/getParameterItemType';
import saveProperty from '../../../utils/saveProperty';

const NESTED_STRUCTURE_PROPERTY_TYPES = new Set(['ARRAY', 'DYNAMIC_PROPERTIES', 'OBJECT', 'TASK']);

const initialAvailablePropertyTypes = Object.keys(VALUE_PROPERTY_CONTROL_TYPES).map((type) => ({
    label: type as string,
    value: type as string,
}));

function emptyExplicitValueForScalarPropertyType(type: string | undefined): unknown {
    if (type === 'FILE_ENTRY' || type === 'NULL') {
        return null;
    }

    return '';
}

function isNestedStructurePropertyType(type: string | undefined): boolean {
    return type !== undefined && NESTED_STRUCTURE_PROPERTY_TYPES.has(type);
}

function getExplicitArrayCellParameterValue(arrayItem: ArrayPropertyType): unknown {
    const {defaultValue, type} = arrayItem;

    if (isNestedStructurePropertyType(type)) {
        return defaultValue;
    }

    if (defaultValue !== undefined && defaultValue !== null) {
        return defaultValue;
    }

    return emptyExplicitValueForScalarPropertyType(type);
}

export interface UseArrayPropertyProps {
    onDeleteClick: (path: string) => void;
    parentArrayItems?: Array<ArrayPropertyType>;
    path: string;
    property: PropertyAllType;
}

export function useArrayProperty({onDeleteClick, parentArrayItems, path, property}: UseArrayPropertyProps) {
    const [arrayItems, setArrayItems] = useState<Array<ArrayPropertyType | Array<ArrayPropertyType>>>([]);
    const [availablePropertyTypes, setAvailablePropertyTypes] =
        useState<Array<{label: string; value: string}>>(initialAvailablePropertyTypes);
    const [newPropertyType, setNewPropertyType] = useState<string>();

    const currentComponent = useWorkflowNodeDetailsPanelStore((state) => state.currentComponent);
    const workflow = useWorkflowDataStore((state) => state.workflow);

    const {updateClusterElementParameterMutation, updateWorkflowNodeParameterMutation} = useWorkflowEditor();

    const {additionalProperties, maxItems, name} = property;

    const items = useMemo(() => {
        let resolvedItems = property.items;

        if (!resolvedItems?.length && parentArrayItems?.[0]?.items?.length) {
            resolvedItems = parentArrayItems[0].items;
        }

        return resolvedItems;
    }, [parentArrayItems, property.items]);

    const isAddDisabled = maxItems != null && arrayItems.length >= maxItems;

    const addButtonTooltip = useMemo(
        () => (isAddDisabled && maxItems != null ? `Maximum number of items (${maxItems}) reached` : undefined),
        [isAddDisabled, maxItems]
    );

    const handleAddItemClick = useCallback(() => {
        if (!currentComponent || !name) {
            return;
        }

        if (maxItems != null && arrayItems.length >= maxItems) {
            return;
        }

        let matchingItem: ArrayPropertyType | undefined = items?.find((item) => item.type === newPropertyType);

        if (!matchingItem) {
            matchingItem = items?.find((item) => item.name === newPropertyType);
        }

        const controlType: ControlType = matchingItem
            ? (matchingItem.controlType as ControlType)
            : newPropertyType && newPropertyType in VALUE_PROPERTY_CONTROL_TYPES
              ? (VALUE_PROPERTY_CONTROL_TYPES[
                    newPropertyType as keyof typeof VALUE_PROPERTY_CONTROL_TYPES
                ] as ControlType)
              : ('STRING' as ControlType);

        const newItemPath = `${path}[${arrayItems.length.toString()}]`;
        const newItemType = (matchingItem?.type as PropertyType) || (newPropertyType as PropertyType) || 'STRING';

        const newItem = {
            ...matchingItem,
            controlType,
            custom: true,
            expressionEnabled: matchingItem?.expressionEnabled ?? true,
            key: crypto.randomUUID(),
            label: `${matchingItem?.label ?? 'Item'} ${arrayItems.length.toString()}`,
            name: `${matchingItem?.label ?? name}__${arrayItems.length.toString()}`,
            path: newItemPath,
            type: newItemType,
        };

        setArrayItems([...arrayItems, newItem]);

        if (updateWorkflowNodeParameterMutation || updateClusterElementParameterMutation) {
            if (newItemType === 'OBJECT') {
                return;
            }

            saveProperty({
                includeInMetadata: true,
                path: newItemPath,
                type: newItemType,
                updateClusterElementParameterMutation,
                updateWorkflowNodeParameterMutation,
                workflowId: workflow.id!,
            });
        }
    }, [
        arrayItems,
        currentComponent,
        items,
        name,
        newPropertyType,
        path,
        updateClusterElementParameterMutation,
        updateWorkflowNodeParameterMutation,
        workflow.id,
        maxItems,
    ]);

    const handleDeleteClick = useCallback(
        (deletePath: string) => {
            if (!currentComponent || !deletePath) {
                return;
            }

            const clickedItemParameterValue = resolvePath(currentComponent.parameters ?? {}, deletePath);

            if (clickedItemParameterValue !== undefined) {
                onDeleteClick(deletePath);
            }
        },
        [currentComponent, onDeleteClick]
    );

    useEffect(() => {
        let propertyTypes: Array<{label: string; value: string}> = [];

        const hasDuplicateTypes = items?.some(
            (item, index) => items.findIndex((otherItem) => otherItem.type === item.type) !== index
        );

        const processItems = (itemList: Array<PropertyAllType>) =>
            itemList.reduce((types: Array<{label: string; value: string}>, item) => {
                if (item && item.type) {
                    if (currentComponent?.componentName === 'condition' && hasDuplicateTypes) {
                        types.push({
                            label: item.label!,
                            value: item.name!,
                        });
                    } else {
                        types.push({
                            label: item.type,
                            value: item.type,
                        });
                    }
                }

                return types;
            }, []);

        if (items?.length) {
            propertyTypes = processItems(items);
        }

        if (additionalProperties?.length) {
            const additionalPropertyItems: Array<PropertyAllType | undefined> = (
                additionalProperties as Array<PropertyAllType>
            )
                .map((propertyItem: PropertyAllType) => propertyItem.items)
                .flat();

            if (additionalPropertyItems) {
                propertyTypes = processItems(additionalPropertyItems as Array<PropertyAllType>);
            }
        }

        if (propertyTypes.length) {
            setAvailablePropertyTypes(propertyTypes);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    useEffect(() => {
        if (availablePropertyTypes.length) {
            setNewPropertyType(availablePropertyTypes[0].value);
        }
    }, [availablePropertyTypes]);

    useEffect(() => {
        if (
            !name ||
            !currentComponent ||
            !currentComponent.parameters ||
            !Object.keys(currentComponent.parameters).length
        ) {
            return;
        }

        const encodedParameters = encodeParameters(currentComponent.parameters);
        const encodedPath = encodePath(path);

        const parameterValue = resolvePath(encodedParameters, encodedPath);

        if (parameterValue === undefined) {
            return;
        }

        if (
            items &&
            items.length > 1 &&
            items.every((item) => item.type === 'OBJECT') &&
            Array.isArray(parameterValue)
        ) {
            const parameterArrayItems = parameterValue.map((parameterItem: ArrayPropertyType, index: number) => {
                const matchingItem = items?.find((item) => item.name === parameterItem.type);

                if (!matchingItem) {
                    if (parameterItem && typeof parameterItem === 'object' && !Array.isArray(parameterItem)) {
                        const parameterItemWithType = parameterItem as ArrayPropertyType;

                        return {
                            ...parameterItemWithType,
                            key: parameterItemWithType.key ?? crypto.randomUUID(),
                        };
                    }

                    return parameterItem;
                }

                const parameterItemIsObject =
                    parameterItem && typeof parameterItem === 'object' && !Array.isArray(parameterItem);

                const subProperties = (matchingItem as ObjectProperty).properties?.map((propertyDefinition) =>
                    parameterItemIsObject &&
                    Object.keys(parameterItem).includes(propertyDefinition.name as keyof ArrayPropertyType)
                        ? {
                              ...propertyDefinition,
                              defaultValue: parameterItem[propertyDefinition.name as keyof ArrayPropertyType],
                              expressionEnabled:
                                  propertyDefinition.expressionEnabled ?? propertyDefinition.type !== 'STRING',
                          }
                        : propertyDefinition
                );

                return {
                    ...matchingItem,
                    custom: true,
                    expressionEnabled: true,
                    key: crypto.randomUUID(),
                    name: index.toString(),
                    properties: subProperties,
                };
            });

            if (parameterArrayItems?.length) {
                setArrayItems(parameterArrayItems);
            }
        } else if (items?.length && items[0].type === 'OBJECT' && Array.isArray(parameterValue)) {
            const parameterArrayItems = parameterValue.map((parameterItem: ArrayPropertyType, index: number) => {
                const parameterItemIsObject =
                    parameterItem && typeof parameterItem === 'object' && !Array.isArray(parameterItem);

                const subProperties = (items[0] as ObjectProperty).properties?.map((propertyDefinition) =>
                    parameterItemIsObject &&
                    Object.keys(parameterItem).includes(propertyDefinition.name as keyof ArrayPropertyType)
                        ? {
                              ...propertyDefinition,
                              defaultValue: parameterItem[propertyDefinition.name as keyof ArrayPropertyType],
                              expressionEnabled:
                                  propertyDefinition.expressionEnabled ?? propertyDefinition.type !== 'STRING',
                          }
                        : propertyDefinition
                );

                return {
                    ...items[0],
                    custom: true,
                    expressionEnabled: true,
                    key: crypto.randomUUID(),
                    name: index.toString(),
                    properties: subProperties,
                };
            });

            if (parameterArrayItems?.length) {
                setArrayItems(parameterArrayItems);
            }
        } else if (Array.isArray(parameterValue)) {
            let subProperty = {};

            if (items?.length === 1 && items[0].type === 'OBJECT') {
                subProperty = items[0];
            }

            const parameterArrayItems = parameterValue.map((parameterItemValue: ArrayPropertyType, index: number) => {
                const subPropertyPath = `${path}[${index}]`;

                let parameterItemType = currentComponent.metadata?.ui?.dynamicPropertyTypes?.[subPropertyPath];

                if (!parameterItemType) {
                    parameterItemType = getParameterItemType(parameterItemValue);
                }

                const matchingItem: ArrayPropertyType | undefined = items?.find(
                    (item) => item.type === parameterItemType || item.name === parameterItemType
                );

                let controlType = 'STRING' as ControlType;

                if (matchingItem) {
                    controlType = matchingItem.controlType as ControlType;
                } else if (parameterItemType && parameterItemType in VALUE_PROPERTY_CONTROL_TYPES) {
                    controlType = VALUE_PROPERTY_CONTROL_TYPES[
                        parameterItemType as keyof typeof VALUE_PROPERTY_CONTROL_TYPES
                    ] as ControlType;
                }

                let label = matchingItem?.label ? `${matchingItem.label} ${index}` : `Item ${index}`;

                if (property.name === 'conditions') {
                    label = `AND Condition ${index}`;
                }

                const newSubProperty = {
                    ...subProperty,
                    ...matchingItem,
                    arrayName: name,
                    controlType,
                    custom: true,
                    defaultValue: parameterItemValue,
                    expressionEnabled: true,
                    key: crypto.randomUUID(),
                    label,
                    name: index.toString(),
                    path: subPropertyPath,
                    type: parameterItemType as PropertyType,
                };

                if (parameterItemType === 'OBJECT') {
                    if (parameterItemValue && typeof parameterItemValue === 'object') {
                        const customSubProperties = Object.keys(parameterItemValue).map((key) => {
                            const decodedKey = decodePath(key);
                            const subPropertyParameterValue = parameterItemValue[key as keyof ArrayPropertyType];

                            let subPropertyParameterItemType =
                                currentComponent.metadata?.ui?.dynamicPropertyTypes?.[
                                    `${path}[${index}].${decodedKey}`
                                ];

                            if (!subPropertyParameterItemType) {
                                subPropertyParameterItemType = getParameterItemType(subPropertyParameterValue);
                            }

                            return {
                                controlType: VALUE_PROPERTY_CONTROL_TYPES[
                                    subPropertyParameterItemType as keyof typeof VALUE_PROPERTY_CONTROL_TYPES
                                ] as ControlType,
                                custom: true,
                                defaultValue: subPropertyParameterValue,
                                expressionEnabled: true,
                                key: crypto.randomUUID(),
                                label: decodedKey,
                                name: key,
                                path: `${subPropertyPath}.${key}`,
                                type: subPropertyParameterItemType as PropertyType,
                            };
                        });

                        return {
                            ...newSubProperty,
                            properties: customSubProperties,
                        };
                    }
                }

                if (parameterItemType === 'BOOLEAN') {
                    return {
                        ...newSubProperty,
                        defaultValue:
                            parameterItemValue !== null && parameterItemValue !== undefined
                                ? parameterItemValue.toString()
                                : '',
                    };
                }

                return newSubProperty;
            });

            if (parameterArrayItems?.length) {
                setArrayItems(parameterArrayItems);
            }
        }

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    return {
        addButtonTooltip,
        arrayItems,
        availablePropertyTypes,
        currentComponent,
        handleAddItemClick,
        handleDeleteClick,
        isAddDisabled,
        items,
        name,
        newPropertyType,
        setArrayItems,
        setNewPropertyType,
    };
}

export interface UseArrayPropertyItemProps {
    arrayItem: ArrayPropertyType;
    currentComponent?: ComponentType;
    index: number;
    onDeleteClick: (path: string) => void;
    path: string;
    setArrayItems: Dispatch<SetStateAction<Array<ArrayPropertyType | Array<ArrayPropertyType>>>>;
}

export function useArrayPropertyItem({
    arrayItem,
    currentComponent,
    index,
    onDeleteClick,
    path,
    setArrayItems,
}: UseArrayPropertyItemProps) {
    const arrayCellParameterValue = getExplicitArrayCellParameterValue(arrayItem);

    const handleOnDeleteClick = useCallback(() => {
        const basePath = path.replace(/\[\d+\]$/, '');

        let parameterArrayAfterDelete: unknown[] = [];

        if (currentComponent?.parameters) {
            const encodedParameters = encodeParameters(currentComponent.parameters);
            const encodedBasePath = encodePath(basePath);

            let resolvedArray = resolvePath(encodedParameters, encodedBasePath);

            if (!Array.isArray(resolvedArray)) {
                resolvedArray = resolvePath(currentComponent.parameters as Record<string, unknown>, basePath);
            }

            if (Array.isArray(resolvedArray)) {
                parameterArrayAfterDelete = [...resolvedArray];

                parameterArrayAfterDelete.splice(index, 1);
            }
        }

        setArrayItems((previousItems) => {
            const remainingItems = previousItems.filter(
                (_previousItem, previousItemIndex) => previousItemIndex !== index
            );

            const storeSliceMatchesRemainingCount = parameterArrayAfterDelete.length === remainingItems.length;

            const reindexedItems = remainingItems.map((remainingItem, newIndex) => {
                if (Array.isArray(remainingItem)) {
                    return remainingItem;
                }

                let nextDefaultValue: unknown;

                if (isNestedStructurePropertyType(remainingItem.type)) {
                    if (storeSliceMatchesRemainingCount && parameterArrayAfterDelete.length > newIndex) {
                        const cellValue = parameterArrayAfterDelete[newIndex];

                        nextDefaultValue =
                            cellValue !== undefined && cellValue !== null ? cellValue : remainingItem.defaultValue;
                    } else if (remainingItem.defaultValue !== undefined && remainingItem.defaultValue !== null) {
                        nextDefaultValue = remainingItem.defaultValue;
                    } else if (parameterArrayAfterDelete.length > newIndex) {
                        const cellValue = parameterArrayAfterDelete[newIndex];

                        nextDefaultValue =
                            cellValue !== undefined && cellValue !== null ? cellValue : remainingItem.defaultValue;
                    } else {
                        nextDefaultValue = remainingItem.defaultValue;
                    }
                } else if (storeSliceMatchesRemainingCount && parameterArrayAfterDelete.length > newIndex) {
                    const cellValue = parameterArrayAfterDelete[newIndex];

                    nextDefaultValue =
                        cellValue === undefined || cellValue === null
                            ? emptyExplicitValueForScalarPropertyType(remainingItem.type)
                            : cellValue;
                } else if (remainingItem.defaultValue !== undefined && remainingItem.defaultValue !== null) {
                    nextDefaultValue = remainingItem.defaultValue;
                } else if (parameterArrayAfterDelete.length > newIndex) {
                    const cellValue = parameterArrayAfterDelete[newIndex];

                    nextDefaultValue =
                        cellValue === undefined || cellValue === null
                            ? emptyExplicitValueForScalarPropertyType(remainingItem.type)
                            : cellValue;
                } else {
                    nextDefaultValue = emptyExplicitValueForScalarPropertyType(remainingItem.type);
                }

                return {
                    ...remainingItem,
                    defaultValue: nextDefaultValue,
                    key: crypto.randomUUID(),
                    name: newIndex.toString(),
                    path: `${basePath}[${newIndex}]`,
                };
            });

            return reindexedItems;
        });

        onDeleteClick(path);
    }, [currentComponent, index, onDeleteClick, path, setArrayItems]);

    return {
        arrayCellParameterValue,
        handleOnDeleteClick,
    };
}
