import {NewSubPropertyI} from '@/pages/platform/workflow-editor/components/properties/components/SubPropertyPopover';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {VALUE_PROPERTY_CONTROL_TYPES} from '@/shared/constants';
import {ControlType, PropertyType} from '@/shared/middleware/platform/configuration';
import {PropertyAllType, SubPropertyType} from '@/shared/types';
import isObject from 'isobject';
import {useCallback, useEffect, useMemo, useRef, useState} from 'react';

import useWorkflowDataStore from '../../../stores/useWorkflowDataStore';
import {decodePath, encodeParameters, encodePath, safeResolvePath} from '../../../utils/encodingUtils';
import getParameterItemType from '../../../utils/getParameterItemType';
import saveProperty from '../../../utils/saveProperty';

interface BuildOrderedPropertyKeysProps {
    dynamicPropertyTypes: Record<string, string> | undefined;
    parameterObject: {[key: string]: unknown};
    path: string;
    properties: Array<PropertyAllType> | undefined;
}

interface BuildPropertyFromParameterKeyProps {
    baseProperty: PropertyAllType;
    displayCondition?: string;
    dynamicPropertyTypes: Record<string, string> | undefined;
    parameterKey: string;
    parameterObject: {[key: string]: unknown};
    path: string;
    properties?: Array<PropertyAllType>;
}

const getPropertyKey = (name: string | undefined, displayCondition?: string): string => {
    if (!name) {
        return '';
    }

    return displayCondition ? `${name}::${displayCondition}` : name;
};

interface UseObjectPropertyProps {
    onDeleteClick?: (path: string) => void;
    path?: string;
    property: PropertyAllType;
}

export const useObjectProperty = ({onDeleteClick, path, property}: UseObjectPropertyProps) => {
    const [subProperties, setSubProperties] = useState<Array<PropertyAllType>>();

    const defaultValueSavedRef = useRef(false);

    const currentNode = useWorkflowNodeDetailsPanelStore((state) => state.currentNode);
    const workflow = useWorkflowDataStore((state) => state.workflow);

    const {updateClusterElementParameterMutation, updateWorkflowNodeParameterMutation} = useWorkflowEditor();

    const {additionalProperties, label, name, placeholder, properties} = property;

    const isContainerObject = name === '__item';

    const availablePropertyTypes = useMemo(() => {
        if (properties?.length) {
            const hasCustomProperty = (properties as Array<PropertyAllType>).find((property) => property.custom);

            if (!hasCustomProperty) {
                return [];
            }
        }

        return additionalProperties?.length
            ? additionalProperties?.reduce((types: Array<{label: string; value: string}>, property) => {
                  if (property.type) {
                      types.push({
                          label: property.type,
                          value: property.type,
                      });
                  }

                  return types;
              }, [])
            : Object.keys(VALUE_PROPERTY_CONTROL_TYPES).map((type) => ({
                  label: type,
                  value: type,
              }));
    }, [additionalProperties, properties]);

    if (!path && name) {
        path = name;
    }

    if (isContainerObject && path) {
        path = path.replace('.__item', '');
    }

    const existingSubPropertyNames = useMemo(
        () =>
            (subProperties ?? [])
                .map((subProperty) => subProperty.name)
                .filter((subPropertyName): subPropertyName is string => !!subPropertyName),
        [subProperties]
    );

    const handleAddItemClick = useCallback(
        ({name: rawNewPropertyName, type: newPropertyType}: NewSubPropertyI) => {
            if (!rawNewPropertyName) {
                return;
            }

            const encodedNewPropertyName = encodePath(rawNewPropertyName);

            const isDuplicateName = !!subProperties?.some(
                (subProperty) => !!subProperty.name && encodePath(subProperty.name) === encodedNewPropertyName
            );

            if (isDuplicateName) {
                return;
            }

            const resolvedPropertyType = (newPropertyType ||
                additionalProperties?.[0].type ||
                'STRING') as keyof typeof VALUE_PROPERTY_CONTROL_TYPES;

            const newItem: SubPropertyType = {
                additionalProperties,
                controlType: VALUE_PROPERTY_CONTROL_TYPES[resolvedPropertyType] as ControlType,
                custom: true,
                expressionEnabled: true,
                label: rawNewPropertyName,
                name: encodedNewPropertyName,
                type: resolvedPropertyType,
            };

            setSubProperties((previousSubProperties) => [...(previousSubProperties || []), newItem]);

            if (updateWorkflowNodeParameterMutation || updateClusterElementParameterMutation) {
                saveProperty({
                    includeInMetadata: true,
                    path: `${path}.${encodedNewPropertyName}`,
                    type: resolvedPropertyType,
                    updateClusterElementParameterMutation,
                    updateWorkflowNodeParameterMutation,
                    workflowId: workflow.id!,
                });
            }
        },
        [
            additionalProperties,
            path,
            subProperties,
            updateClusterElementParameterMutation,
            updateWorkflowNodeParameterMutation,
            workflow.id,
        ]
    );

    const handleDeleteClick = useCallback(
        (subProperty: SubPropertyType) => {
            if (!path) {
                return;
            }

            setSubProperties((previousSubProperties) =>
                previousSubProperties?.filter((property) => {
                    const compositePropertyKey = getPropertyKey(property.name, property.displayCondition);

                    const compositeSubPropertyKey = getPropertyKey(subProperty.name, subProperty.displayCondition);

                    return compositePropertyKey !== compositeSubPropertyKey;
                })
            );

            if (onDeleteClick) {
                onDeleteClick(`${path}.${subProperty.name}`);
            }
        },
        [onDeleteClick, path]
    );

    const buildOrderedPropertyKeys = useCallback(
        ({dynamicPropertyTypes, parameterObject, path, properties}: BuildOrderedPropertyKeysProps): string[] => {
            const subPropertyKeySet = new Set<string>();
            const orderedKeys: string[] = [];

            if (properties?.length) {
                properties.forEach((property) => {
                    if (property.name) {
                        orderedKeys.push(property.name);

                        const compositePropertyKey = getPropertyKey(property.name, property.displayCondition);

                        subPropertyKeySet.add(compositePropertyKey);
                    }
                });
            }

            if (path && dynamicPropertyTypes) {
                const pathPrefix = `${path}.`;

                Object.keys(dynamicPropertyTypes).forEach((dynamicKey) => {
                    if (dynamicKey.startsWith(pathPrefix)) {
                        const subPropertyName = dynamicKey.substring(pathPrefix.length);

                        const isArrayIndexPattern = /\[\d+\]/.test(subPropertyName);

                        if (subPropertyName && !subPropertyName.includes('.') && !isArrayIndexPattern) {
                            const compositePropertyKey = getPropertyKey(subPropertyName);

                            if (!subPropertyKeySet.has(compositePropertyKey)) {
                                orderedKeys.push(subPropertyName);

                                subPropertyKeySet.add(compositePropertyKey);
                            }
                        }
                    }
                });
            }

            if (parameterObject) {
                Object.keys(parameterObject).forEach((key) => {
                    const compositePropertyKey = getPropertyKey(key);

                    if (!subPropertyKeySet.has(compositePropertyKey)) {
                        orderedKeys.push(key);

                        subPropertyKeySet.add(compositePropertyKey);
                    }
                });
            }

            return orderedKeys;
        },
        []
    );

    const buildPropertyFromParameterKey = useCallback(
        ({
            baseProperty,
            displayCondition,
            dynamicPropertyTypes,
            parameterKey,
            parameterObject,
            path,
            properties,
        }: BuildPropertyFromParameterKeyProps): PropertyAllType => {
            let matchingProperty = properties?.find(
                (property) => property.name === parameterKey && property.displayCondition === displayCondition
            ) as PropertyAllType | undefined;

            if (!matchingProperty) {
                matchingProperty = !displayCondition
                    ? (properties?.find((property) => property.name === parameterKey) as PropertyAllType | undefined)
                    : undefined;
            }

            let parameterItemType = dynamicPropertyTypes?.[`${path}.${parameterKey}`];
            const parameterKeyValue = parameterObject[parameterKey];

            if (Array.isArray(parameterKeyValue) && !parameterItemType) {
                parameterItemType = 'ARRAY';
            }

            if (isObject(parameterKeyValue) && !parameterItemType) {
                parameterItemType = 'OBJECT';
            }

            if (!parameterItemType) {
                parameterItemType = getParameterItemType(parameterKeyValue);
            }

            if (matchingProperty) {
                const matchingPropertyType = matchingProperty.type || parameterItemType;

                const matchingPropertyControlType =
                    matchingProperty.controlType ||
                    (VALUE_PROPERTY_CONTROL_TYPES[
                        matchingPropertyType as keyof typeof VALUE_PROPERTY_CONTROL_TYPES
                    ] as ControlType);

                return {
                    ...matchingProperty,
                    controlType: matchingPropertyControlType,
                    defaultValue: parameterKeyValue,
                    expressionEnabled: matchingProperty.expressionEnabled ?? matchingPropertyType !== 'STRING',
                    type: matchingPropertyType,
                } as PropertyAllType;
            } else {
                return {
                    ...baseProperty,
                    controlType: VALUE_PROPERTY_CONTROL_TYPES[
                        parameterItemType as keyof typeof VALUE_PROPERTY_CONTROL_TYPES
                    ] as ControlType,
                    custom: true,
                    defaultValue: parameterKeyValue,
                    displayCondition,
                    expressionEnabled: true,
                    label: parameterKey,
                    name: encodePath(parameterKey),
                    type: parameterItemType as PropertyType,
                } as PropertyAllType;
            }
        },
        []
    );

    // render individual object items with data gathered from parameters
    useEffect(() => {
        if (!name || !path || !currentNode?.parameters) {
            return;
        }

        const encodedParameters = encodeParameters(currentNode.parameters);
        const encodedPath = encodePath(path);

        const resolvedParameterObject = safeResolvePath(encodedParameters, encodedPath);

        const parameterObject: {[key: string]: unknown} = {};

        if (resolvedParameterObject && isObject(resolvedParameterObject)) {
            for (const encodedKey of Object.keys(resolvedParameterObject)) {
                parameterObject[decodePath(encodedKey)] = (resolvedParameterObject as {[key: string]: unknown})[
                    encodedKey
                ];
            }
        }

        const dynamicPropertyTypes = currentNode?.metadata?.ui?.dynamicPropertyTypes;

        const objectParameterKeys = buildOrderedPropertyKeys({
            dynamicPropertyTypes,
            parameterObject,
            path,
            properties: properties as Array<PropertyAllType>,
        });

        if (!objectParameterKeys.length && !properties?.length) {
            return;
        }

        const preexistingProperties: Array<PropertyAllType> = [];
        const processedKeys = new Set<string>();

        if (properties?.length) {
            properties.forEach((propertyDefinition) => {
                if (!propertyDefinition.name) {
                    return;
                }

                const parameterKeyValue = parameterObject[propertyDefinition.name];

                if (
                    parameterKeyValue !== undefined ||
                    objectParameterKeys.includes(propertyDefinition.name) ||
                    propertyDefinition.displayCondition
                ) {
                    const builtProperty = buildPropertyFromParameterKey({
                        baseProperty: property,
                        displayCondition: propertyDefinition.displayCondition,
                        dynamicPropertyTypes,
                        parameterKey: propertyDefinition.name,
                        parameterObject,
                        path,
                        properties: properties as Array<PropertyAllType>,
                    });

                    const finalProperty = {
                        ...builtProperty,
                        ...propertyDefinition,
                        defaultValue: parameterKeyValue !== undefined ? parameterKeyValue : builtProperty.defaultValue,
                    } as PropertyAllType;

                    preexistingProperties.push(finalProperty);

                    const compositePropertyKey = getPropertyKey(
                        propertyDefinition.name,
                        propertyDefinition.displayCondition
                    );

                    processedKeys.add(compositePropertyKey);
                }
            });
        }

        objectParameterKeys.forEach((parameterKey) => {
            const parameterKeyValue = parameterObject[parameterKey];

            if (parameterKeyValue !== undefined) {
                const builtProperty = buildPropertyFromParameterKey({
                    baseProperty: property,
                    dynamicPropertyTypes,
                    parameterKey,
                    parameterObject,
                    path,
                    properties: properties as Array<PropertyAllType>,
                });

                const compositePropertyKey = getPropertyKey(builtProperty.name, builtProperty.displayCondition);

                if (!processedKeys.has(compositePropertyKey)) {
                    preexistingProperties.push(builtProperty);

                    processedKeys.add(compositePropertyKey);
                }
            }
        });

        if (preexistingProperties.length) {
            setSubProperties(preexistingProperties as Array<PropertyAllType>);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [
        currentNode?.parameters,
        properties,
        path,
        currentNode?.metadata?.ui?.dynamicPropertyTypes,
        buildOrderedPropertyKeys,
        buildPropertyFromParameterKey,
    ]);

    // set default values for subProperties when they are created
    useEffect(() => {
        if (
            !subProperties ||
            !path ||
            !(updateWorkflowNodeParameterMutation || updateClusterElementParameterMutation) ||
            !workflow.id
        ) {
            return;
        }

        if (defaultValueSavedRef.current) {
            return;
        }

        const currentNode = useWorkflowNodeDetailsPanelStore.getState().currentNode;

        if (!currentNode) {
            return;
        }

        if (path.includes('.') && (!currentNode.parameters || !Object.keys(currentNode.parameters).length)) {
            return;
        }

        const encodedParameters = encodeParameters(currentNode.parameters ?? {});
        const encodedPath = encodePath(path);

        const existingObject = safeResolvePath(encodedParameters, encodedPath);

        if (existingObject && isObject(existingObject)) {
            return;
        }

        const buildObject = (properties: Array<PropertyAllType>) =>
            properties.reduce<Record<string, unknown>>((accumulator, subProperty) => {
                const {defaultValue, name, properties, type} = subProperty;

                if (!name) {
                    return accumulator;
                }

                const decodedName = decodePath(name);

                if (type === 'OBJECT' && properties) {
                    accumulator[decodedName] = buildObject(properties);
                } else {
                    accumulator[decodedName] = defaultValue;
                }
                return accumulator;
            }, {});

        const defaultValueObject = buildObject(subProperties);

        const timeoutId = setTimeout(() => {
            defaultValueSavedRef.current = true;

            saveProperty({
                path,
                type: 'OBJECT',
                updateClusterElementParameterMutation,
                updateWorkflowNodeParameterMutation,
                value: defaultValueObject,
                workflowId: workflow.id!,
            });
        }, 600);

        return () => clearTimeout(timeoutId);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [subProperties]);

    // set subProperties in cases where the ObjectProperty has predefined properties
    useEffect(() => {
        if (properties?.length) {
            setSubProperties(properties as Array<PropertyAllType>);
        }
    }, [properties]);

    useEffect(() => {
        defaultValueSavedRef.current = false;
    }, [path]);

    return {
        availablePropertyTypes,
        calculatedPath: path,
        defaultPropertyType: (property.additionalProperties?.[0]?.type ??
            'STRING') as keyof typeof VALUE_PROPERTY_CONTROL_TYPES,
        existingSubPropertyNames,
        getPropertyKey,
        handleAddItemClick,
        handleDeleteClick,
        isContainerObject,
        label,
        name,
        placeholder,
        subProperties,
    };
};
