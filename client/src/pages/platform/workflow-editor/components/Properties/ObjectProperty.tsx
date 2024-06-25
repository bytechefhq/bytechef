import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {VALUE_PROPERTY_CONTROL_TYPES} from '@/shared/constants';
import {ControlTypeModel, PropertyTypeModel} from '@/shared/middleware/platform/configuration';
import {PropertyType, SubPropertyType} from '@/shared/types';
import isObject from 'isobject';
import resolvePath from 'object-resolve-path';
import {Fragment, useEffect, useState} from 'react';
import {twMerge} from 'tailwind-merge';

import Property from './Property';
import DeletePropertyButton from './components/DeletePropertyButton';
import SubPropertyPopover from './components/SubPropertyPopover';

interface ObjectPropertyProps {
    operationName?: string;
    arrayIndex?: number;
    arrayName?: string;
    onDeleteClick?: (path: string) => void;
    path?: string;
    property: PropertyType;
}

const ObjectProperty = ({arrayIndex, arrayName, onDeleteClick, operationName, path, property}: ObjectPropertyProps) => {
    const [subProperties, setSubProperties] = useState<Array<PropertyType>>(
        (property.properties as Array<PropertyType>) || []
    );
    const [newPropertyName, setNewPropertyName] = useState('');
    const [newPropertyType, setNewPropertyType] = useState<keyof typeof VALUE_PROPERTY_CONTROL_TYPES>(
        (property.additionalProperties?.[0]?.type as keyof typeof VALUE_PROPERTY_CONTROL_TYPES) || 'STRING'
    );

    const {currentComponent} = useWorkflowNodeDetailsPanelStore();

    const {additionalProperties, label, name, properties} = property;

    const isContainerObject = name === '__item';

    let availablePropertyTypes = additionalProperties?.length
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

    if (properties?.length) {
        const hasCustomProperty = (properties as Array<PropertyType>).find((property) => property.custom);

        if (!hasCustomProperty) {
            availablePropertyTypes = [];
        }
    }

    if (!path) {
        path = name;
    }

    if (isContainerObject && path) {
        path = path.replace('.__item', '');
    }

    const handleAddItemClick = () => {
        const newItem: SubPropertyType = {
            additionalProperties,
            controlType: VALUE_PROPERTY_CONTROL_TYPES[newPropertyType] as ControlTypeModel,
            custom: true,
            expressionEnabled: true,
            label: newPropertyName,
            name: newPropertyName,
            type: (newPropertyType ||
                additionalProperties?.[0].type ||
                'STRING') as keyof typeof VALUE_PROPERTY_CONTROL_TYPES,
        };

        setSubProperties([...(subProperties || []), newItem]);

        setNewPropertyName('');
    };

    const handleDeleteClick = (subProperty: SubPropertyType) => {
        if (!path) {
            return;
        }

        setSubProperties((subProperties) => subProperties.filter((property) => property.name !== subProperty.name));

        if (onDeleteClick) {
            onDeleteClick(`${path}.${subProperty.name}`);
        }
    };

    // render individual object items with data gathered from parameters
    useEffect(() => {
        if (!name || !path || !currentComponent?.parameters) {
            return;
        }

        const parameterObject = resolvePath(currentComponent.parameters, path);

        if (!parameterObject) {
            return;
        }

        const objectParameterKeys = properties?.length
            ? properties?.map((property) => property.name)
            : Object.keys(parameterObject);

        const preexistingProperties = objectParameterKeys.map((parameterKey) => {
            const matchingProperty = (properties as Array<PropertyType>)?.find(
                (property) => property.name === parameterKey
            );

            let parameterItemType = currentComponent.metadata?.ui?.dynamicPropertyTypes?.[`${path}.${parameterKey}`];

            const parameterKeyValue = parameterObject[parameterKey!];

            if (Array.isArray(parameterKeyValue) && !parameterItemType) {
                parameterItemType = 'ARRAY';
            }

            if (isObject(parameterKeyValue) && !parameterItemType) {
                parameterItemType = 'OBJECT';
            }

            if (matchingProperty) {
                const matchingPropertyType = matchingProperty.type || parameterItemType;

                const matchingPropertyControlType =
                    matchingProperty.controlType ||
                    (VALUE_PROPERTY_CONTROL_TYPES[
                        matchingPropertyType as keyof typeof VALUE_PROPERTY_CONTROL_TYPES
                    ] as ControlTypeModel);

                return {
                    ...matchingProperty,
                    controlType: matchingPropertyControlType,
                    defaultValue: parameterKeyValue,
                    type: matchingPropertyType,
                };
            } else {
                return {
                    controlType: VALUE_PROPERTY_CONTROL_TYPES[
                        parameterItemType as keyof typeof VALUE_PROPERTY_CONTROL_TYPES
                    ] as ControlTypeModel,
                    custom: true,
                    defaultValue: parameterKeyValue,
                    expressionEnabled: true,
                    label: parameterKey,
                    name: parameterKey,
                    type: parameterItemType as PropertyTypeModel,
                };
            }
        });

        if (preexistingProperties.length) {
            setSubProperties(preexistingProperties);
        }

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    // set subProperties in cases where the ObjectProperty has predefined properties
    useEffect(() => {
        if (properties?.length && !currentComponent?.parameters) {
            setSubProperties(properties as Array<PropertyType>);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [properties]);

    return (
        <Fragment key={name}>
            <ul
                className={twMerge(
                    'space-y-4',
                    label && !isContainerObject && 'ml-2 border-l',
                    arrayName && !isContainerObject && 'pl-2'
                )}
            >
                {(subProperties as unknown as Array<SubPropertyType>)?.map((subProperty, index) => (
                    <div className="relative flex w-full" key={`${property.name}_${subProperty.name}_${index}`}>
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
                            parameterValue={subProperty.defaultValue}
                            path={`${path}.${subProperty.name}`}
                            property={{
                                ...subProperty,
                                name: subProperty.name,
                            }}
                        />
                    </div>
                ))}
            </ul>

            {!!availablePropertyTypes?.length && (
                <SubPropertyPopover
                    availablePropertyTypes={availablePropertyTypes}
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
