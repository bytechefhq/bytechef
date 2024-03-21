import {Button} from '@/components/ui/button';
import {UpdateWorkflowRequest} from '@/middleware/automation/configuration';
import {ControlTypeModel, ObjectPropertyModel, PropertyModel, WorkflowModel} from '@/middleware/platform/configuration';
import {PROPERTY_CONTROL_TYPES} from '@/shared/constants';
import {ComponentDataType, CurrentComponentType, DataPillType, PropertyType} from '@/types/types';
import {Cross2Icon, PlusIcon} from '@radix-ui/react-icons';
import {PopoverClose} from '@radix-ui/react-popover';
import {UseMutationResult} from '@tanstack/react-query';
import {useEffect, useState} from 'react';

import {Popover, PopoverContent, PopoverTrigger} from '../ui/popover';
import Property from './Property';
import DeletePropertyButton from './components/DeletePropertyButton';
import PropertySelect from './components/PropertySelect';

interface ArrayPropertyProps {
    currentComponent?: CurrentComponentType;
    currentComponentData?: ComponentDataType;
    dataPills?: Array<DataPillType>;
    handleDeleteProperty?: (subPropertyName: string, propertyName: string) => void;
    property: PropertyType;
    updateWorkflowMutation?: UseMutationResult<WorkflowModel, Error, UpdateWorkflowRequest, unknown>;
}

type ArrayPropertyType = PropertyModel & {controlType?: ControlTypeModel; custom?: boolean; defaultValue?: string};

const ArrayProperty = ({
    currentComponent,
    currentComponentData,
    dataPills,
    handleDeleteProperty,
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

    const handleDeletePropertyClick = (subPropertyName: string, propertyName: string) => {
        if (!subPropertyName || !propertyName) {
            return;
        }

        if (handleDeleteProperty) {
            handleDeleteProperty(subPropertyName, propertyName);
        }

        setArrayItems((subProperties) =>
            subProperties.filter((subProperty) => (subProperty as ArrayPropertyType).name !== subPropertyName)
        );
    };

    // render individual array items with data gathered from parameters
    useEffect(() => {
        if (!currentComponentData?.parameters || !Object.keys(currentComponentData?.parameters).length) {
            return;
        }

        if (items?.length && name && items[0].type === 'OBJECT') {
            const parameterArrayItems = currentComponentData.parameters[name]?.map(
                (parameterItem: ArrayPropertyType, index: number) => {
                    const subProperties = Object.keys(parameterItem).map((key) => {
                        const matchingSubproperty = (items[0] as ObjectPropertyModel).properties?.find(
                            (property) => property.name === key
                        );

                        return {
                            ...matchingSubproperty,
                            defaultValue: parameterItem[key as keyof ArrayPropertyType],
                        };
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
            const parameterArrayItems = currentComponentData.parameters[name]?.map(
                (parameterItem: ArrayPropertyType) => ({
                    controlType: PROPERTY_CONTROL_TYPES[newItemType] as ControlTypeModel,
                    custom: true,
                    defaultValue: Object.values(parameterItem)[0],
                    name: Object.keys(parameterItem)[0],
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
                (arrayItem as Array<ArrayPropertyType>).length ? (
                    (arrayItem as Array<ArrayPropertyType>).map((subItem: ArrayPropertyType) => (
                        <div className="ml-2 flex w-full border-l pb-2" key={subItem.name || `${name}_0`}>
                            <Property
                                arrayIndex={index}
                                arrayName={name}
                                currentComponent={currentComponent}
                                currentComponentData={currentComponentData}
                                customClassName="pl-2 w-full"
                                dataPills={dataPills}
                                mention={!!dataPills?.length}
                                property={
                                    subItem as PropertyType & {controlType?: ControlTypeModel; defaultValue?: string}
                                }
                                updateWorkflowMutation={updateWorkflowMutation}
                            />

                            {subItem.custom && name && subItem.name && (
                                <DeletePropertyButton
                                    handleDeletePropertyClick={handleDeletePropertyClick}
                                    propertyName={name}
                                    subPropertyName={subItem.name}
                                />
                            )}
                        </div>
                    ))
                ) : (
                    <div
                        className="ml-2 flex w-full border-l pb-2"
                        key={(arrayItem as ArrayPropertyType).name || `${name}_0`}
                    >
                        <Property
                            arrayIndex={index}
                            arrayName={name}
                            currentComponent={currentComponent}
                            currentComponentData={currentComponentData}
                            customClassName="pl-2 w-full"
                            dataPills={dataPills}
                            mention={!!dataPills?.length}
                            property={
                                arrayItem as PropertyType & {controlType?: ControlTypeModel; defaultValue?: string}
                            }
                            updateWorkflowMutation={updateWorkflowMutation}
                        />

                        {(arrayItem as ArrayPropertyType).custom && name && (arrayItem as ArrayPropertyType).name && (
                            <DeletePropertyButton
                                handleDeletePropertyClick={handleDeletePropertyClick}
                                propertyName={name}
                                subPropertyName={(arrayItem as ArrayPropertyType).name!}
                            />
                        )}
                    </div>
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
