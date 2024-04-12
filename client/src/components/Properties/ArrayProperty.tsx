import {Button} from '@/components/ui/button';
import {UpdateWorkflowRequest} from '@/middleware/automation/configuration';
import {ControlTypeModel, ObjectPropertyModel, WorkflowModel} from '@/middleware/platform/configuration';
import {PROPERTY_CONTROL_TYPES} from '@/shared/constants';
import {
    ArrayPropertyType,
    ComponentType,
    CurrentComponentDefinitionType,
    DataPillType,
    PropertyType,
} from '@/types/types';
import {Cross2Icon, PlusIcon} from '@radix-ui/react-icons';
import {PopoverClose} from '@radix-ui/react-popover';
import {UseMutationResult} from '@tanstack/react-query';
import {useEffect, useState} from 'react';

import {Popover, PopoverContent, PopoverTrigger} from '../ui/popover';
import ArrayPropertyItem from './components/ArrayPropertyItem';
import PropertySelect from './components/PropertySelect';

interface ArrayPropertyProps {
    currentComponentDefinition?: CurrentComponentDefinitionType;
    currentComponent?: ComponentType;
    dataPills?: Array<DataPillType>;
    path?: string;
    property: PropertyType;
    updateWorkflowMutation?: UseMutationResult<WorkflowModel, Error, UpdateWorkflowRequest, unknown>;
}

const ArrayProperty = ({
    currentComponent,
    currentComponentDefinition,
    dataPills,
    path,
    property,
    updateWorkflowMutation,
}: ArrayPropertyProps) => {
    const [arrayItems, setArrayItems] = useState<Array<ArrayPropertyType | Array<ArrayPropertyType>>>([]);
    const [newItemType, setNewItemType] = useState<keyof typeof PROPERTY_CONTROL_TYPES>('STRING');

    const {items, name} = property;

    const handleAddItemClick = () => {
        const matchingItem = items?.find((item) => item.type === newItemType);

        const newItem: ArrayPropertyType = {
            ...matchingItem,
            controlType: PROPERTY_CONTROL_TYPES[newItemType] as ControlTypeModel,
            custom: true,
            name: `${name}_${arrayItems.length}`,
            type: newItemType,
        };

        setArrayItems([...arrayItems, newItem]);
    };

    // render individual array items with data gathered from parameters
    useEffect(() => {
        if (
            !currentComponent?.parameters ||
            !name ||
            !currentComponent.parameters[name] ||
            !Object.keys(currentComponent?.parameters).length
        ) {
            return;
        }

        if (items?.length && name && items[0].type === 'OBJECT') {
            const parameterArrayItems = currentComponent.parameters[name].map(
                (parameterItem: ArrayPropertyType, index: number) => {
                    const subProperties = (items[0] as ObjectPropertyModel).properties?.map((property) => {
                        const matchingSubproperty = Object.keys(parameterItem).includes(
                            property.name as keyof ArrayPropertyType
                        )
                            ? {
                                  ...property,
                                  defaultValue: parameterItem[property.name as keyof ArrayPropertyType],
                              }
                            : property;

                        return matchingSubproperty;
                    });

                    return {
                        ...items[0],
                        custom: true,
                        name: `${name}_${index}`,
                        properties: subProperties,
                    };
                }
            );

            if (parameterArrayItems?.length) {
                setArrayItems(parameterArrayItems);
            }
        } else if (name) {
            const parameterArrayItems = currentComponent.parameters[name].map(
                (parameterItem: ArrayPropertyType, index: number) => ({
                    controlType: PROPERTY_CONTROL_TYPES[newItemType] as ControlTypeModel,
                    custom: true,
                    defaultValue: parameterItem,
                    name: `${name}_${index}`,
                    type: newItemType,
                })
            );

            if (parameterArrayItems?.length) {
                setArrayItems(parameterArrayItems);
            }
        }

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

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
        : Object.keys(PROPERTY_CONTROL_TYPES).map((type) => ({
              label: type,
              value: type,
          }));

    useEffect(() => {
        if (availableItemTypes.length === 1) {
            setNewItemType(availableItemTypes[0].value as keyof typeof PROPERTY_CONTROL_TYPES);
        }
    }, [availableItemTypes]);

    return (
        <ul className="w-full">
            {arrayItems?.map((arrayItem, index) =>
                Array.isArray(arrayItem) ? (
                    arrayItem.map((subItem: ArrayPropertyType) => (
                        <ArrayPropertyItem
                            arrayItem={subItem}
                            arrayName={name}
                            currentComponent={currentComponent}
                            currentComponentDefinition={currentComponentDefinition}
                            dataPills={dataPills}
                            index={index}
                            key={subItem.name}
                            path={path}
                            setArrayItems={setArrayItems}
                            updateWorkflowMutation={updateWorkflowMutation}
                        />
                    ))
                ) : (
                    <ArrayPropertyItem
                        arrayItem={arrayItem}
                        arrayName={name}
                        currentComponent={currentComponent}
                        currentComponentDefinition={currentComponentDefinition}
                        dataPills={dataPills}
                        index={index}
                        key={arrayItem.name}
                        path={path}
                        setArrayItems={setArrayItems}
                        updateWorkflowMutation={updateWorkflowMutation}
                    />
                )
            )}

            {availableItemTypes.length > 1 ? (
                <Popover>
                    <PopoverTrigger asChild>
                        <Button
                            className="mt-2 rounded-sm bg-gray-100 text-xs font-medium hover:bg-gray-200"
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
                                onValueChange={(value: keyof typeof PROPERTY_CONTROL_TYPES) => setNewItemType(value)}
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
                    className="mt-2 rounded-sm bg-gray-100 text-xs font-medium hover:bg-gray-200"
                    onClick={handleAddItemClick}
                    size="sm"
                    variant="ghost"
                >
                    <PlusIcon className="size-4" /> Add item
                </Button>
            )}
        </ul>
    );
};

export default ArrayProperty;
