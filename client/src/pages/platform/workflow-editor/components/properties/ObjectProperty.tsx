import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {VALUE_PROPERTY_CONTROL_TYPES} from '@/shared/constants';
import {ControlType, PropertyType} from '@/shared/middleware/platform/configuration';
import {PropertyAllType, SubPropertyType} from '@/shared/types';
import isObject from 'isobject';
import resolvePath from 'object-resolve-path';
import {Fragment, useCallback, useEffect, useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';

import useWorkflowDataStore from '../../stores/useWorkflowDataStore';
import {encodeParameters, encodePath} from '../../utils/encodingUtils';
import getParameterItemType from '../../utils/getParameterItemType';
import saveProperty from '../../utils/saveProperty';
import Property from './Property';
import DeletePropertyButton from './components/DeletePropertyButton';
import SubPropertyPopover from './components/SubPropertyPopover';

interface BuildOrderedPropertyKeysProps {
    dynamicPropertyTypes: Record<string, string> | undefined;
    parameterObject: {[key: string]: object[] | undefined};
    path: string;
    properties: Array<PropertyAllType> | undefined;
}

interface BuildPropertyFromParameterKeyProps {
    baseProperty: PropertyAllType;
    dynamicPropertyTypes: Record<string, string> | undefined;
    parameterKey: string;
    parameterObject: {[key: string]: unknown};
    path: string;
    properties: Array<PropertyAllType>;
}

interface ObjectPropertyProps {
    operationName?: string;
    arrayIndex?: number;
    arrayName?: string;
    onDeleteClick?: (path: string) => void;
    path?: string;
    property: PropertyAllType;
}

const ObjectProperty = ({arrayIndex, arrayName, onDeleteClick, operationName, path, property}: ObjectPropertyProps) => {
    const [subProperties, setSubProperties] = useState<Array<PropertyAllType>>();
    const [newPropertyName, setNewPropertyName] = useState('');
    const [newPropertyType, setNewPropertyType] = useState<keyof typeof VALUE_PROPERTY_CONTROL_TYPES>(
        (property.additionalProperties?.[0]?.type as keyof typeof VALUE_PROPERTY_CONTROL_TYPES) || 'STRING'
    );
    const [parameterObject, setParameterObject] = useState<{[key: string]: object[] | undefined}>({});

    const currentComponent = useWorkflowNodeDetailsPanelStore((state) => state.currentComponent);
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

    if (!path) {
        path = name;
    }

    if (isContainerObject && path) {
        path = path.replace('.__item', '');
    }

    const handleAddItemClick = useCallback(() => {
        const newItem: SubPropertyType = {
            additionalProperties,
            controlType: VALUE_PROPERTY_CONTROL_TYPES[newPropertyType] as ControlType,
            custom: true,
            expressionEnabled: true,
            label: newPropertyName,
            name: newPropertyName,
            type: (newPropertyType ||
                additionalProperties?.[0].type ||
                'STRING') as keyof typeof VALUE_PROPERTY_CONTROL_TYPES,
        };

        setSubProperties((previousSubProperties) => [...(previousSubProperties || []), newItem]);

        setNewPropertyName('');

        if (updateWorkflowNodeParameterMutation || updateClusterElementParameterMutation) {
            saveProperty({
                includeInMetadata: true,
                path: `${path}.${newPropertyName}`,
                type: newPropertyType,
                updateClusterElementParameterMutation,
                updateWorkflowNodeParameterMutation,
                workflowId: workflow.id!,
            });
        }
    }, [
        additionalProperties,
        newPropertyName,
        newPropertyType,
        path,
        updateClusterElementParameterMutation,
        updateWorkflowNodeParameterMutation,
        workflow.id,
    ]);

    const handleDeleteClick = useCallback(
        (subProperty: SubPropertyType) => {
            if (!path) {
                return;
            }

            setSubProperties((previousSubProperties) =>
                previousSubProperties?.filter((property) => property.name !== subProperty.name)
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

                        subPropertyKeySet.add(property.name);
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
                            if (!subPropertyKeySet.has(subPropertyName)) {
                                orderedKeys.push(subPropertyName);

                                subPropertyKeySet.add(subPropertyName);
                            }
                        }
                    }
                });
            }

            if (parameterObject) {
                Object.keys(parameterObject).forEach((key) => {
                    if (!subPropertyKeySet.has(key)) {
                        orderedKeys.push(key);

                        subPropertyKeySet.add(key);
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
            dynamicPropertyTypes,
            parameterKey,
            parameterObject,
            path,
            properties,
        }: BuildPropertyFromParameterKeyProps): PropertyAllType => {
            const matchingProperty = properties.find((property) => property.name === parameterKey) as
                | PropertyAllType
                | undefined;

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
                    expressionEnabled: true,
                    label: parameterKey,
                    name: parameterKey,
                    type: parameterItemType as PropertyType,
                } as PropertyAllType;
            }
        },
        []
    );

    // render individual object items with data gathered from parameters
    useEffect(() => {
        if (
            !name ||
            !path ||
            !currentComponent?.parameters ||
            !properties ||
            !parameterObject ||
            !isObject(parameterObject)
        ) {
            return;
        }

        const dynamicPropertyTypes = currentComponent?.metadata?.ui?.dynamicPropertyTypes;

        const objectParameterKeys = buildOrderedPropertyKeys({
            dynamicPropertyTypes,
            parameterObject,
            path,
            properties: properties as Array<PropertyAllType>,
        });

        if (!objectParameterKeys.length) {
            return;
        }

        const preexistingProperties = objectParameterKeys.map((parameterKey) =>
            buildPropertyFromParameterKey({
                baseProperty: property,
                dynamicPropertyTypes,
                parameterKey,
                parameterObject,
                path,
                properties: properties as Array<PropertyAllType>,
            })
        );

        if (preexistingProperties.length) {
            setSubProperties(preexistingProperties as Array<PropertyAllType>);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [
        parameterObject,
        properties,
        path,
        currentComponent?.metadata?.ui?.dynamicPropertyTypes,
        buildOrderedPropertyKeys,
        buildPropertyFromParameterKey,
    ]);

    // set default values for subProperties when they are created
    useEffect(() => {
        if (
            !subProperties ||
            !path ||
            !currentComponent ||
            !(updateWorkflowNodeParameterMutation || updateClusterElementParameterMutation) ||
            !workflow.id
        ) {
            return;
        }

        if (path.includes('.') && (!currentComponent.parameters || !Object.keys(currentComponent.parameters).length)) {
            return;
        }

        const encodedParameters = encodeParameters(currentComponent.parameters ?? {});
        const encodedPath = encodePath(path);

        const existingObject = resolvePath(encodedParameters, encodedPath);

        if (existingObject && isObject(existingObject)) {
            return;
        }

        const buildObject = (properties: Array<PropertyAllType>) =>
            properties.reduce<Record<string, unknown>>((acc, subProperty) => {
                const {defaultValue, name, properties, type} = subProperty;

                if (!name) {
                    return acc;
                }

                if (type === 'OBJECT' && properties) {
                    acc[name] = buildObject(properties);
                } else {
                    acc[name] = defaultValue;
                }
                return acc;
            }, {});

        const defaultValueObject = buildObject(subProperties);

        const timeoutId = setTimeout(() => {
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

    // update parameterObject when workflowDefinition changes
    useEffect(() => {
        if (workflow.definition && path) {
            const resolvedParameterObject = resolvePath(currentComponent?.parameters ?? {}, path);

            setParameterObject(resolvedParameterObject);
        }
    }, [workflow.definition, path, currentComponent?.parameters]);

    return (
        <Fragment key={name}>
            <ul
                className={twMerge(
                    'space-y-4',
                    label && !isContainerObject && 'ml-2 border-l border-l-border/50',
                    arrayName && !isContainerObject && 'pl-2'
                )}
            >
                {(subProperties as unknown as Array<SubPropertyType>)?.map((subProperty, index) => (
                    <Property
                        arrayIndex={arrayIndex}
                        arrayName={arrayName}
                        customClassName={twMerge(
                            'w-full last-of-type:pb-0',
                            label && 'mb-0',
                            isContainerObject && 'pb-0',
                            !arrayName && !isContainerObject && 'pl-2'
                        )}
                        deletePropertyButton={
                            subProperty.custom && name && subProperty.name && currentComponent ? (
                                <DeletePropertyButton
                                    onClick={() => handleDeleteClick(subProperty)}
                                    propertyName={subProperty.name}
                                />
                            ) : undefined
                        }
                        key={`${property.name}_${subProperty.name}_${index}`}
                        objectName={arrayName ? '' : name}
                        operationName={operationName}
                        path={`${path}.${subProperty.name}`}
                        property={subProperty}
                    />
                ))}
            </ul>

            {!!availablePropertyTypes?.length && (
                <SubPropertyPopover
                    availablePropertyTypes={availablePropertyTypes}
                    buttonLabel={placeholder}
                    handleClick={handleAddItemClick}
                    newPropertyName={newPropertyName}
                    newPropertyType={newPropertyType}
                    setNewPropertyName={setNewPropertyName}
                    setNewPropertyType={setNewPropertyType}
                />
            )}
        </Fragment>
    );
};

export default ObjectProperty;
