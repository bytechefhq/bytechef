import {Button} from '@/components/ui/button';
import {UpdateWorkflowRequest} from '@/middleware/automation/configuration';
import {ControlTypeModel, PropertyModel, WorkflowModel} from '@/middleware/platform/configuration';
import {PROPERTY_CONTROL_TYPES} from '@/shared/constants';
import {ComponentDataType, DataPillType, PropertyType} from '@/types/types';
import {Cross2Icon, PlusIcon} from '@radix-ui/react-icons';
import {PopoverClose} from '@radix-ui/react-popover';
import {UseMutationResult} from '@tanstack/react-query';
import {XIcon} from 'lucide-react';
import {useEffect, useState} from 'react';

import {Popover, PopoverContent, PopoverTrigger} from '../ui/popover';
import Property from './Property';
import PropertySelect from './components/PropertySelect';

interface ArrayPropertyProps {
    currentComponentData?: ComponentDataType;
    dataPills?: Array<DataPillType>;
    handleDeleteProperty?: (subPropertyName: string, propertyName: string) => void;
    property: PropertyType;
    updateWorkflowMutation?: UseMutationResult<WorkflowModel, Error, UpdateWorkflowRequest, unknown>;
}

type ArrayPropertyType = PropertyModel & {controlType?: ControlTypeModel; custom?: boolean; defaultValue?: string};

const ArrayProperty = ({
    currentComponentData,
    dataPills,
    handleDeleteProperty,
    property,
    updateWorkflowMutation,
}: ArrayPropertyProps) => {
    const [arrayItems, setArrayItems] = useState<Array<ArrayPropertyType>>([]);
    const [newItemType, setNewItemType] = useState<keyof typeof PROPERTY_CONTROL_TYPES>('STRING');

    const {items, name} = property;

    const handleAddItemClick = () => {
        const newItem: ArrayPropertyType = {
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

        setArrayItems((subProperties) => subProperties.filter((subProperty) => subProperty.name !== subPropertyName));
    };

    // set property.items?.[0].name if it's not set
    useEffect(() => {
        if (!arrayItems?.length) {
            return;
        }

        const updatedArrayItems = arrayItems.map((item) => {
            if (!item.name) {
                return {
                    ...item,
                    name: `${name}_0`,
                };
            }

            return item;
        });

        setArrayItems(updatedArrayItems);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [arrayItems?.length, name]);

    // render individual array items with data gathered from parameters
    useEffect(() => {
        if (!currentComponentData?.parameters || !Object.keys(currentComponentData?.parameters).length) {
            return;
        }

        const parameterArrayItems: Array<ArrayPropertyType> = Object.keys(currentComponentData.parameters).reduce(
            (parameters: Array<ArrayPropertyType>, key: string) => {
                if (arrayItems?.length && key.startsWith(`${name}_`)) {
                    const strippedValue: string = currentComponentData.parameters?.[key]
                        .replace(/<p>/g, '')
                        .replace(/<\/p>/g, '');

                    parameters.push({
                        ...items?.[0],
                        defaultValue: strippedValue,
                        name: key,
                        type: items?.[0].type,
                    });
                }

                return parameters;
            },
            []
        );

        parameterArrayItems.sort((first, second) => {
            const firstName = first.name?.toLowerCase();
            const secondName = second.name?.toLowerCase();

            if (!firstName || !secondName) {
                return 0;
            }

            if (firstName < secondName) {
                return -1;
            }

            if (firstName > secondName) {
                return 1;
            }

            return 0;
        });

        if (parameterArrayItems.length) {
            setArrayItems(parameterArrayItems);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    return (
        <ul className="w-full">
            {arrayItems?.map((item) => (
                <div className="ml-2 flex w-full border-l pb-2" key={item.name || `${name}_0`}>
                    <Property
                        arrayName={name}
                        currentComponentData={currentComponentData}
                        customClassName="pl-2 w-full"
                        dataPills={dataPills}
                        mention={!!dataPills?.length}
                        property={item as PropertyType & {controlType?: ControlTypeModel; defaultValue?: string}}
                        updateWorkflowMutation={updateWorkflowMutation}
                    />

                    {item.custom && (
                        <Button
                            className="ml-1 self-center"
                            onClick={() => handleDeletePropertyClick(item.name!, name!)}
                            size="icon"
                            title="Delete property"
                            variant="ghost"
                        >
                            <XIcon className="h-full w-auto cursor-pointer p-2 hover:text-red-500" />
                        </Button>
                    )}
                </div>
            ))}

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
                            options={Object.keys(PROPERTY_CONTROL_TYPES).map((type) => ({
                                label: type,
                                value: type,
                            }))}
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
        </ul>
    );
};

export default ArrayProperty;
