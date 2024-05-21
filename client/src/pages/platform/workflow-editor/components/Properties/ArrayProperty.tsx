import {Button} from '@/components/ui/button';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {ControlTypeModel, ObjectPropertyModel} from '@/middleware/platform/configuration';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {VALUE_PROPERTY_CONTROL_TYPES} from '@/shared/constants';
import {ArrayPropertyType, PropertyType} from '@/types/types';
import {Cross2Icon, PlusIcon} from '@radix-ui/react-icons';
import {PopoverClose} from '@radix-ui/react-popover';
import {useEffect, useState} from 'react';

import getParameterByPath from '../../utils/getParameterByPath';
import getParameterType from '../../utils/getParameterType';
import ArrayPropertyItem from './components/ArrayPropertyItem';
import PropertySelect from './components/PropertySelect';

interface ArrayPropertyProps {
    onDeleteClick: (path: string, name?: string, index?: number) => void;
    path?: string;
    property: PropertyType;
}

const ArrayProperty = ({onDeleteClick, path, property}: ArrayPropertyProps) => {
    const [arrayItems, setArrayItems] = useState<Array<ArrayPropertyType | Array<ArrayPropertyType>>>([]);
    const [newItemType, setNewItemType] = useState<keyof typeof VALUE_PROPERTY_CONTROL_TYPES | undefined>(
        property.items?.[0]?.type as keyof typeof VALUE_PROPERTY_CONTROL_TYPES
    );

    const {currentComponent} = useWorkflowNodeDetailsPanelStore();

    const {items, name} = property;

    const handleAddItemClick = () => {
        const matchingItem: ArrayPropertyType | undefined = items?.find((item) => item.type === newItemType);

        if (!currentComponent || !name) {
            return;
        }

        const newItem: ArrayPropertyType = {
            ...matchingItem,
            controlType: matchingItem
                ? matchingItem?.controlType
                : (VALUE_PROPERTY_CONTROL_TYPES[newItemType!] as ControlTypeModel),
            custom: true,
            expressionEnabled: true,
            label: '',
            name: arrayItems.length.toString(),
            type: matchingItem?.type || newItemType || 'STRING',
        };

        setArrayItems([...arrayItems, newItem]);
    };

    const handleDeleteClick = (path: string, name: string, index: number) => {
        const clickedItemParameter = getParameterByPath(path, currentComponent)[index];

        if (clickedItemParameter) {
            onDeleteClick(path, undefined, index);
        }
    };

    const availableItemTypes = items?.length
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

        if (path && path !== 'parameters') {
            params = getParameterByPath(path, currentComponent);
        }

        const currentParams = params[name].filter((param: ArrayPropertyType) => param !== null);

        if (items?.length && name && items[0].type === 'OBJECT' && Array.isArray(params?.[name])) {
            const parameterArrayItems = currentParams.map((parameterItem: ArrayPropertyType, index: number) => {
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
            });

            if (parameterArrayItems?.length) {
                setArrayItems(parameterArrayItems);
            }
        } else if (name && Array.isArray(params?.[name])) {
            const parameterArrayItems = currentParams.map((parameterItemValue: ArrayPropertyType, index: number) => {
                const parameterItemType = getParameterType(parameterItemValue);

                const customSubProperties = Object.keys(parameterItemValue).map((key) => {
                    const subPropertyParameterValue = parameterItemValue[key as keyof ArrayPropertyType];

                    const subPropertyType = typeof subPropertyParameterValue === 'boolean' ? 'BOOLEAN' : 'STRING';

                    return {
                        controlType: VALUE_PROPERTY_CONTROL_TYPES[
                            subPropertyType as keyof typeof VALUE_PROPERTY_CONTROL_TYPES
                        ] as ControlTypeModel,
                        custom: true,
                        defaultValue: subPropertyParameterValue,
                        label: key,
                        name: key,
                        type: subPropertyType,
                    };
                });

                return {
                    arrayName: name,
                    controlType: VALUE_PROPERTY_CONTROL_TYPES[
                        parameterItemType as keyof typeof VALUE_PROPERTY_CONTROL_TYPES
                    ] as ControlTypeModel,
                    custom: true,
                    defaultValue: parameterItemValue,
                    name: index.toString(),
                    properties: parameterItemType === 'OBJECT' ? customSubProperties : undefined,
                    type: parameterItemType,
                };
            });

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
                        arrayItem.map((subItem: ArrayPropertyType) => (
                            <ArrayPropertyItem
                                arrayItem={subItem}
                                arrayName={name}
                                currentComponent={currentComponent}
                                index={index}
                                key={subItem.name}
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
                            key={arrayItem.name}
                            onDeleteClick={handleDeleteClick}
                            path={path}
                            setArrayItems={setArrayItems}
                        />
                    )
                )}
            </ul>

            {availableItemTypes.length > 1 ? (
                <Popover>
                    <PopoverTrigger asChild>
                        <Button
                            className="mt-3 rounded-sm bg-gray-100 text-xs font-medium hover:bg-gray-200"
                            size="sm"
                            variant="ghost"
                        >
                            <PlusIcon className="size-4" /> Add item
                        </Button>
                    </PopoverTrigger>

                    <PopoverContent className="min-w-[400px]">
                        <header className="flex items-center justify-between">
                            <span className="font-medium">Add item</span>

                            <PopoverClose asChild>
                                <Cross2Icon aria-hidden="true" className="size-4 cursor-pointer" />
                            </PopoverClose>
                        </header>

                        <main className="my-2 space-y-2">
                            <PropertySelect
                                label="Type"
                                onValueChange={(value) =>
                                    setNewItemType(value as keyof typeof VALUE_PROPERTY_CONTROL_TYPES)
                                }
                                options={availableItemTypes}
                                value={newItemType}
                            />
                        </main>

                        <footer className="flex items-center justify-end space-x-2">
                            <PopoverClose asChild>
                                <Button onClick={handleAddItemClick} size="sm">
                                    Add
                                </Button>
                            </PopoverClose>
                        </footer>
                    </PopoverContent>
                </Popover>
            ) : (
                <Button
                    className="mt-3 rounded-sm bg-gray-100 text-xs font-medium hover:bg-gray-200"
                    onClick={handleAddItemClick}
                    size="sm"
                    variant="ghost"
                >
                    <PlusIcon className="size-4" /> Add item
                </Button>
            )}
        </>
    );
};

export default ArrayProperty;
