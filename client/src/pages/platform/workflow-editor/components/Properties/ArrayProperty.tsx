import {Button} from '@/components/ui/button';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {VALUE_PROPERTY_CONTROL_TYPES} from '@/shared/constants';
import {ControlTypeModel, ObjectPropertyModel, PropertyTypeModel} from '@/shared/middleware/platform/configuration';
import {ArrayPropertyType, PropertyType} from '@/shared/types';
import getRandomId from '@/shared/util/random-utils';
import {PlusIcon} from '@radix-ui/react-icons';
import {useEffect, useState} from 'react';

import getArrayParameterValueByPath from '../../utils/getArrayParameterValueByPath';
import getObjectParameterValueByPath from '../../utils/getObjectParameterValueByPath';
import getParameterType from '../../utils/getParameterType';
import ArrayPropertyItem from './components/ArrayPropertyItem';
import SubPropertyPopover from './components/SubPropertyPopover';

interface ArrayPropertyProps {
    onDeleteClick: (path: string) => void;
    path: string;
    property: PropertyType;
}

const ArrayProperty = ({onDeleteClick, path, property}: ArrayPropertyProps) => {
    const [arrayItems, setArrayItems] = useState<Array<ArrayPropertyType | Array<ArrayPropertyType>>>([]);
    const [newPropertyType, setNewPropertyType] = useState<keyof typeof VALUE_PROPERTY_CONTROL_TYPES>(
        (property.additionalProperties?.[0]?.type as keyof typeof VALUE_PROPERTY_CONTROL_TYPES) || 'STRING'
    );

    const {currentComponent} = useWorkflowNodeDetailsPanelStore();

    const {items, name} = property;

    const handleAddItemClick = () => {
        const matchingItem: ArrayPropertyType | undefined = items?.find((item) => item.type === newPropertyType);

        if (!currentComponent || !name) {
            return;
        }

        const newItem = {
            ...matchingItem,
            controlType: matchingItem
                ? matchingItem?.controlType
                : (VALUE_PROPERTY_CONTROL_TYPES[newPropertyType!] as ControlTypeModel),
            custom: true,
            expressionEnabled: true,
            key: getRandomId(),
            label: `Item ${arrayItems.length.toString()}`,
            name: `${name}__${arrayItems.length.toString()}`,
            type: matchingItem?.type || newPropertyType || 'STRING',
        };

        setArrayItems([...arrayItems, newItem]);
    };

    const handleDeleteClick = (path: string) => {
        if (!currentComponent || !path) {
            return;
        }

        const clickedItemParameterValue = getArrayParameterValueByPath(path, currentComponent.parameters);

        if (clickedItemParameterValue !== undefined || clickedItemParameterValue !== null) {
            onDeleteClick(path);
        }
    };

    const availablePropertyTypes = items?.length
        ? items?.reduce((types: Array<{label: string; value: string}>, item) => {
              if (item.type) {
                  types.push({
                      label: item.type,
                      value: item.type,
                  });
              }

              return types;
          }, [])
        : Object.keys(VALUE_PROPERTY_CONTROL_TYPES).map((type) => ({
              label: type,
              value: type,
          }));

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

        let params = currentComponent.parameters;

        if (path) {
            params = getArrayParameterValueByPath(path, currentComponent.parameters);

            if (path.includes('.')) {
                params = getObjectParameterValueByPath(path, currentComponent.parameters);
            }
        }

        if (!params) {
            return;
        }

        const currentParameterValues: Array<ArrayPropertyType> = params.filter(
            (param: ArrayPropertyType) => param !== null
        );

        if (!currentParameterValues) {
            return;
        }

        if (items?.length && name && items[0].type === 'OBJECT' && Array.isArray(currentParameterValues)) {
            const parameterArrayItems = currentParameterValues.map(
                (parameterItem: ArrayPropertyType, index: number) => {
                    const subProperties = (items[0] as ObjectPropertyModel).properties?.map((property) =>
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
                }
            );

            if (parameterArrayItems?.length) {
                setArrayItems(parameterArrayItems);
            }
        } else if (name && Array.isArray(currentParameterValues)) {
            const parameterArrayItems = currentParameterValues.map(
                (parameterItemValue: ArrayPropertyType, index: number) => {
                    const parameterItemType = getParameterType(parameterItemValue);

                    const newSubProperty = {
                        arrayName: name,
                        controlType: VALUE_PROPERTY_CONTROL_TYPES[
                            parameterItemType as keyof typeof VALUE_PROPERTY_CONTROL_TYPES
                        ] as ControlTypeModel,
                        custom: true,
                        defaultValue: parameterItemValue,
                        expressionEnabled: true,
                        name: index.toString(),
                        type: parameterItemType,
                    };

                    if (parameterItemType === 'OBJECT') {
                        const customSubProperties = Object.keys(parameterItemValue).map((key) => {
                            const subPropertyParameterValue = parameterItemValue[key as keyof ArrayPropertyType];

                            const subPropertyParameterItemType = getParameterType(subPropertyParameterValue);

                            const subPropertyType =
                                subPropertyParameterItemType ||
                                (typeof subPropertyParameterValue === 'boolean' ? 'BOOLEAN' : 'STRING');

                            return {
                                controlType: VALUE_PROPERTY_CONTROL_TYPES[
                                    subPropertyType as keyof typeof VALUE_PROPERTY_CONTROL_TYPES
                                ] as ControlTypeModel,
                                custom: true,
                                defaultValue: subPropertyParameterValue,
                                expressionEnabled: true,
                                label: key,
                                name: key,
                                type: subPropertyType as PropertyTypeModel,
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
                }
            );

            if (parameterArrayItems?.length) {
                setArrayItems(parameterArrayItems);
            }
        }

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    return (
        <>
            <ul className="ml-2 w-full space-y-4 border-l">
                {arrayItems?.map((arrayItem, index) =>
                    Array.isArray(arrayItem) ? (
                        arrayItem.map((subItem: ArrayPropertyType, subItemIndex: number) => (
                            <ArrayPropertyItem
                                arrayItem={subItem}
                                arrayName={name}
                                currentComponent={currentComponent}
                                index={index}
                                key={subItemIndex}
                                onDeleteClick={handleDeleteClick}
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
                            key={arrayItem.key}
                            onDeleteClick={handleDeleteClick}
                            path={`${path}[${index}]`}
                            setArrayItems={setArrayItems}
                        />
                    )
                )}
            </ul>

            {availablePropertyTypes.length > 1 ? (
                <SubPropertyPopover
                    array
                    availablePropertyTypes={availablePropertyTypes}
                    handleClick={handleAddItemClick}
                    newPropertyType={newPropertyType}
                    setNewPropertyType={setNewPropertyType}
                />
            ) : (
                <Button
                    className="mt-3 rounded-sm bg-gray-100 text-xs font-medium hover:bg-gray-200"
                    onClick={handleAddItemClick}
                    size="sm"
                    variant="ghost"
                >
                    <PlusIcon className="size-4" /> Add Array item
                </Button>
            )}
        </>
    );
};

export default ArrayProperty;
