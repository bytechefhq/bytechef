import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {VALUE_PROPERTY_CONTROL_TYPES} from '@/shared/constants';
import {ControlType, PropertyType} from '@/shared/middleware/platform/configuration';
import {PropertyAllType, SubPropertyType} from '@/shared/types';
import isObject from 'isobject';
import resolvePath from 'object-resolve-path';
import {Fragment, useCallback, useEffect, useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';

import {useWorkflowNodeParameterMutation} from '../../providers/workflowNodeParameterMutationProvider';
import useWorkflowDataStore from '../../stores/useWorkflowDataStore';
import {encodeParameters, encodePath} from '../../utils/encodingUtils';
import getParameterItemType from '../../utils/getParameterItemType';
import saveProperty from '../../utils/saveProperty';
import Property from './Property';
import DeletePropertyButton from './components/DeletePropertyButton';
import SubPropertyPopover from './components/SubPropertyPopover';

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

    const {currentComponent} = useWorkflowNodeDetailsPanelStore();
    const {workflow} = useWorkflowDataStore();
    const {updateWorkflowNodeParameterMutation} = useWorkflowNodeParameterMutation();

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
    }, [additionalProperties, newPropertyName, newPropertyType]);

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

    const objectParameterKeys: Array<string> = useMemo(() => {
        if (properties?.length) {
            return properties.map((property) => property.name!);
        }

        if (parameterObject) {
            return Object.keys(parameterObject);
        }

        return [];
    }, [properties, parameterObject]);

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

        const preexistingProperties = objectParameterKeys.map((parameterKey, index) => {
            const matchingProperty = (properties as Array<PropertyAllType>)[index] as PropertyAllType;

            let parameterItemType = currentComponent.metadata?.ui?.dynamicPropertyTypes?.[`${path}.${parameterKey}`];

            const parameterKeyValue = parameterObject[parameterKey!];

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
                };
            } else {
                return {
                    ...property,
                    controlType: VALUE_PROPERTY_CONTROL_TYPES[
                        parameterItemType as keyof typeof VALUE_PROPERTY_CONTROL_TYPES
                    ] as ControlType,
                    custom: true,
                    defaultValue: parameterKeyValue,
                    expressionEnabled: true,
                    label: parameterKey,
                    name: parameterKey,
                    type: parameterItemType as PropertyType,
                };
            }
        });

        if (preexistingProperties.length) {
            setSubProperties(preexistingProperties as Array<PropertyAllType>);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [parameterObject, properties]);

    // set default values for subProperties when they are created
    useEffect(() => {
        if (!subProperties || !path || !currentComponent || !updateWorkflowNodeParameterMutation || !workflow.id) {
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
                updateWorkflowNodeParameterMutation,
                value: defaultValueObject,
                workflowId: workflow.id!,
            });
        }, 200);

        return () => clearTimeout(timeoutId);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [subProperties]);

    // set subProperties in cases where the ObjectProperty has predefined properties
    useEffect(() => {
        if (properties?.length) {
            setSubProperties(properties as Array<PropertyAllType>);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
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
