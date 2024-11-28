import {Button} from '@/components/ui/button';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {VALUE_PROPERTY_CONTROL_TYPES} from '@/shared/constants';
import {ControlType, ObjectProperty, PropertyType} from '@/shared/middleware/platform/configuration';
import {ArrayPropertyType, PropertyAllType} from '@/shared/types';
import {PlusIcon} from '@radix-ui/react-icons';
import isObject from 'isobject';
import resolvePath from 'object-resolve-path';
import {Fragment, useEffect, useState} from 'react';

import ArrayPropertyItem from './components/ArrayPropertyItem';
import SubPropertyPopover from './components/SubPropertyPopover';

interface ArrayPropertyProps {
    onDeleteClick: (path: string) => void;
    parentArrayItems?: Array<ArrayPropertyType>;
    path: string;
    property: PropertyAllType;
}

const initialAvailablePropertyTypes = Object.keys(VALUE_PROPERTY_CONTROL_TYPES).map((type) => ({
    label: type as string,
    value: type as string,
}));

const ArrayProperty = ({onDeleteClick, parentArrayItems, path, property}: ArrayPropertyProps) => {
    const [arrayItems, setArrayItems] = useState<Array<ArrayPropertyType | Array<ArrayPropertyType>>>([]);
    const [availablePropertyTypes, setAvailablePropertyTypes] =
        useState<Array<{label: string; value: string}>>(initialAvailablePropertyTypes);
    const [newPropertyType, setNewPropertyType] = useState<string>();

    const {currentComponent} = useWorkflowNodeDetailsPanelStore();

    const {additionalProperties, name} = property;

    let items = property.items;

    if (!items?.length && parentArrayItems?.[0].items?.length) {
        items = parentArrayItems?.[0].items;
    }

    const handleAddItemClick = () => {
        if (!currentComponent || !name) {
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

        const newItem = {
            ...matchingItem,
            controlType,
            custom: true,
            label: `${matchingItem?.label ?? 'Item'} ${arrayItems.length.toString()}`,
            name: `${matchingItem?.label ?? name}__${arrayItems.length.toString()}`,
            path: `${path}[${arrayItems.length.toString()}]`,
            type: (matchingItem?.type as PropertyType) || (newPropertyType as PropertyType) || 'STRING',
        };

        setArrayItems([...arrayItems, newItem]);
    };

    const handleDeleteClick = (path: string) => {
        if (!currentComponent || !path) {
            return;
        }

        const clickedItemParameterValue = resolvePath(currentComponent.parameters, path);

        if (clickedItemParameterValue !== undefined) {
            onDeleteClick(path);
        }
    };

    // get available property types from items and additional properties
    useEffect(() => {
        let propertyTypes: Array<{label: string; value: string}> = [];

        const hasDuplicateTypes = items?.some(
            (item, index) => items.findIndex((otherItem) => otherItem.type === item.type) !== index
        );

        const processItems = (items: Array<PropertyAllType>) =>
            items.reduce((types: Array<{label: string; value: string}>, item) => {
                if (item.type) {
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
                .map((property: PropertyAllType) => property.items)
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

    // render individual array items with data gathered from parameters
    useEffect(() => {
        if (
            !name ||
            !currentComponent ||
            !currentComponent.parameters ||
            !Object.keys(currentComponent.parameters).length
        ) {
            return;
        }

        const parameterValue = resolvePath(currentComponent.parameters, path);

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

                const subProperties = (matchingItem as ObjectProperty).properties?.map((property) =>
                    Object.keys(parameterItem).includes(property.name as keyof ArrayPropertyType)
                        ? {
                              ...property,
                              defaultValue: parameterItem[property.name as keyof ArrayPropertyType],
                          }
                        : property
                );

                return {
                    ...matchingItem,
                    custom: true,
                    name: index.toString(),
                    properties: subProperties,
                };
            });

            if (parameterArrayItems?.length) {
                setArrayItems(parameterArrayItems as Array<ArrayPropertyType>);
            }
        } else if (items?.length && items[0].type === 'OBJECT' && Array.isArray(parameterValue)) {
            const parameterArrayItems = parameterValue.map((parameterItem: ArrayPropertyType, index: number) => {
                const subProperties = (items[0] as ObjectProperty).properties?.map((property) =>
                    Object.keys(parameterItem).includes(property.name as keyof ArrayPropertyType)
                        ? {
                              ...property,
                              defaultValue: parameterItem[property.name as keyof ArrayPropertyType],
                          }
                        : property
                );

                return {
                    ...items[0],
                    custom: true,
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

                if (isObject(parameterItemValue) && !parameterItemType) {
                    parameterItemType = 'OBJECT';
                }

                if (Array.isArray(parameterItemValue) && !parameterItemType) {
                    parameterItemType = 'ARRAY';
                }

                const controlType: ControlType =
                    parameterItemType && parameterItemType in VALUE_PROPERTY_CONTROL_TYPES
                        ? (VALUE_PROPERTY_CONTROL_TYPES[
                              parameterItemType as keyof typeof VALUE_PROPERTY_CONTROL_TYPES
                          ] as ControlType)
                        : ('STRING' as ControlType);

                let label = `Item ${index}`;

                if (property.name === 'conditions') {
                    label = `AND Condition ${index}`;
                }

                const newSubProperty = {
                    ...subProperty,
                    arrayName: name,
                    controlType,
                    custom: true,
                    defaultValue: parameterItemValue,
                    label,
                    name: index.toString(),
                    path: subPropertyPath,
                    type: parameterItemType as PropertyType,
                };

                if (parameterItemType === 'OBJECT') {
                    const customSubProperties = Object.keys(parameterItemValue).map((key) => {
                        const subPropertyParameterValue = parameterItemValue[key as keyof ArrayPropertyType];

                        const subPropertyParameterItemType =
                            currentComponent.metadata?.ui?.dynamicPropertyTypes?.[`${path}[${index}].${key}`];

                        return {
                            controlType: VALUE_PROPERTY_CONTROL_TYPES[
                                subPropertyParameterItemType as keyof typeof VALUE_PROPERTY_CONTROL_TYPES
                            ] as ControlType,
                            custom: true,
                            defaultValue: subPropertyParameterValue,
                            expressionEnabled: true,
                            label: key,
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

                if (parameterItemType === 'BOOLEAN') {
                    return {
                        ...newSubProperty,
                        defaultValue: parameterItemValue.toString(),
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

    return (
        <Fragment key={`${path}_${name}_arrayProperty`}>
            <ul className="ml-2 flex flex-col space-y-4 border-l border-l-border/50">
                {arrayItems?.map((arrayItem, index) =>
                    Array.isArray(arrayItem) ? (
                        arrayItem.map((subItem: ArrayPropertyType, subItemIndex: number) => (
                            <ArrayPropertyItem
                                arrayItem={subItem}
                                arrayName={name}
                                currentComponent={currentComponent}
                                index={index}
                                key={`${(arrayItem as unknown as ArrayPropertyType).key}_${subItem.name}_${subItemIndex}`}
                                onDeleteClick={handleDeleteClick}
                                parentArrayItems={items}
                                path={path}
                                setArrayItems={setArrayItems}
                            />
                        ))
                    ) : (
                        <ArrayPropertyItem
                            arrayItem={arrayItem}
                            arrayName={name}
                            currentComponent={currentComponent}
                            index={index}
                            key={arrayItem.key || `${path}_${name}_${arrayItem.name}_${index}`}
                            onDeleteClick={handleDeleteClick}
                            parentArrayItems={items}
                            path={`${path}[${index}]`}
                            setArrayItems={setArrayItems}
                        />
                    )
                )}
            </ul>

            {availablePropertyTypes.length > 1 && !!newPropertyType ? (
                <SubPropertyPopover
                    array
                    availablePropertyTypes={availablePropertyTypes}
                    buttonLabel={property.placeholder ?? parentArrayItems?.[0].placeholder}
                    condition={currentComponent?.componentName === 'condition'}
                    handleClick={handleAddItemClick}
                    key={`${path}_${name}_subPropertyPopoverButton`}
                    newPropertyType={newPropertyType}
                    setNewPropertyType={setNewPropertyType}
                />
            ) : (
                <Button
                    className="mt-3 rounded-sm bg-gray-100 text-xs font-medium hover:bg-gray-200"
                    key={`${path}_${name}_addPropertyPopoverButton`}
                    onClick={handleAddItemClick}
                    size="sm"
                    variant="ghost"
                >
                    <PlusIcon className="size-4" />

                    {property.placeholder || 'Add array item'}
                </Button>
            )}
        </Fragment>
    );
};

export default ArrayProperty;
