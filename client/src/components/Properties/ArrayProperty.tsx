import PropertyDropdownMenu from '@/components/Properties/components/PropertyDropdownMenu';
import {Button} from '@/components/ui/button';
import {UpdateWorkflowRequest} from '@/middleware/automation/configuration';
import {PropertyModel, WorkflowModel} from '@/middleware/platform/configuration';
import {ComponentDataType, DataPillType, PropertyType} from '@/types/types';
import {PlusIcon} from '@radix-ui/react-icons';
import {UseMutationResult} from '@tanstack/react-query';
import {useEffect, useState} from 'react';

import Property from './Property';

type ArrayPropertyProps = {
    currentComponentData?: ComponentDataType;
    dataPills?: Array<DataPillType>;
    property: PropertyType;
    updateWorkflowMutation?: UseMutationResult<WorkflowModel, Error, UpdateWorkflowRequest, unknown>;
};

const ArrayProperty = ({currentComponentData, dataPills, property, updateWorkflowMutation}: ArrayPropertyProps) => {
    const [arrayItems, setArrayItems] = useState(property.items);

    const {items, multipleValues, name} = property;

    const newPropertyTypeOptions = items?.reduce((uniqueItems: Array<{label: string; value: string}>, item) => {
        if (!uniqueItems.find((uniqueItem) => uniqueItem.value === item.type)) {
            if (item.type) {
                uniqueItems.push({
                    label: item.type,
                    value: item.type,
                });
            }
        }

        return uniqueItems;
    }, []);

    const handleAddItemClick = () => {
        if (!arrayItems) {
            return;
        }

        const newItem = {
            name: `${name}_${arrayItems.length}`,
            type: arrayItems[0].type,
        };

        setArrayItems([...arrayItems, newItem]);
    };

    useEffect(() => {
        if (!currentComponentData?.parameters || !Object.keys(currentComponentData?.parameters).length) {
            return;
        }

        const parameterArrayItems = Object.keys(currentComponentData!.parameters!).reduce(
            (parameters: Array<PropertyModel & {defaultValue: string}>, key: string) => {
                if (arrayItems?.length && key.startsWith(`${name}_`)) {
                    const strippedValue = currentComponentData.parameters?.[key]
                        .replace(/<p>/g, '')
                        .replace(/<\/p>/g, '');

                    parameters.push({
                        ...arrayItems[0],
                        defaultValue: strippedValue,
                        name: key,
                        type: arrayItems[0].type,
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

        setArrayItems(parameterArrayItems);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [arrayItems?.length, currentComponentData?.parameters, name]);

    return (
        <ul className="w-full">
            {arrayItems?.map((item) => (
                <Property
                    arrayName={name}
                    currentComponentData={currentComponentData}
                    customClassName="border-l ml-2 pl-2 pb-2 last-of-type:pb-0"
                    dataPills={dataPills}
                    key={item.name || `${name}_0`}
                    mention={!!dataPills?.length}
                    property={item}
                    updateWorkflowMutation={updateWorkflowMutation}
                />
            ))}

            {multipleValues && (
                <div className="relative ml-2 w-full self-start border-l pl-2 pt-2">
                    {newPropertyTypeOptions?.length && newPropertyTypeOptions?.length > 1 ? (
                        <PropertyDropdownMenu
                            menuItems={newPropertyTypeOptions}
                            trigger={
                                <Button
                                    className="rounded-sm bg-gray-100 text-sm font-medium hover:bg-gray-200"
                                    onClick={handleAddItemClick}
                                    size="sm"
                                    variant="ghost"
                                >
                                    <PlusIcon className="size-4" /> Add item
                                </Button>
                            }
                        />
                    ) : (
                        <Button
                            className="rounded-sm bg-gray-100 text-xs font-medium hover:bg-gray-200"
                            onClick={handleAddItemClick}
                            size="sm"
                            variant="ghost"
                        >
                            <PlusIcon className="size-4" /> Add item
                        </Button>
                    )}
                </div>
            )}
        </ul>
    );
};

export default ArrayProperty;
