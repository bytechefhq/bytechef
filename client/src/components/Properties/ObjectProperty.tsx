import PropertyInput from '@/components/Properties/components/PropertyInput/PropertyInput';
import PropertySelect from '@/components/Properties/components/PropertySelect';
import {Button} from '@/components/ui/button';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {UpdateWorkflowRequest, WorkflowModel} from '@/middleware/automation/configuration';
import {PropertyTypeModel} from '@/middleware/platform/configuration';
import {ComponentDataType, CurrentComponentType, DataPillType, PropertyType} from '@/types/types';
import {Cross2Icon, PlusIcon} from '@radix-ui/react-icons';
import {PopoverClose} from '@radix-ui/react-popover';
import {UseMutationResult} from '@tanstack/react-query';
import {XIcon} from 'lucide-react';
import {useEffect, useState} from 'react';
import {twMerge} from 'tailwind-merge';

import Property from './Property';

interface ObjectPropertyProps {
    actionName?: string;
    currentComponent?: CurrentComponentType;
    currentComponentData?: ComponentDataType;
    dataPills?: DataPillType[];
    handleDeleteProperty?: (arg1: string, arg2: string) => void;
    property: PropertyType;
    updateWorkflowMutation?: UseMutationResult<WorkflowModel, Error, UpdateWorkflowRequest, unknown>;
}

type SubPropertyType = {
    controlType: string;
    defaultValue: string;
    name: string;
    type: string;
};

const ObjectProperty = ({
    actionName,
    currentComponent,
    currentComponentData,
    dataPills,
    handleDeleteProperty,
    property,
    updateWorkflowMutation,
}: ObjectPropertyProps) => {
    const [subProperties, setSubProperties] = useState<Array<SubPropertyType>>(
        (property.properties as Array<SubPropertyType>) || []
    );
    const [newPropertyName, setNewPropertyName] = useState('');
    const [newPropertyType, setNewPropertyType] = useState('');

    const {additionalProperties, controlType, label, multipleValues, name} = property;

    const handleAddItemClick = () => {
        setSubProperties([
            ...subProperties,
            {
                controlType: 'TEXT',
                defaultValue: '',
                name: newPropertyName,
                type: newPropertyType || additionalProperties?.[0].type || 'STRING',
            },
        ]);

        setNewPropertyName('');
    };

    const handleDeletePropertyClick = (subPropertyName: string, propertyName: string) => {
        if (!subPropertyName || !propertyName) {
            return;
        }

        if (handleDeleteProperty) {
            handleDeleteProperty(subPropertyName, propertyName);
        }

        setSubProperties((subProperties) =>
            subProperties.filter((subProperty) => subProperty.name !== subPropertyName)
        );
    };

    // on initial render, set subProperties if there are matching parameters
    useEffect(() => {
        if (!name || !currentComponentData?.parameters) {
            return;
        }

        const matchingParameters = Object.keys(currentComponentData.parameters).filter((key) => key.startsWith(name));

        if (!matchingParameters.length) {
            return;
        }

        const preexistingProperties = matchingParameters.map((matchingParameter) => {
            const value: string = currentComponentData.parameters![matchingParameter];

            return {
                ...currentComponentData.parameters![matchingParameter],
                controlType: 'TEXT',
                defaultValue: value,
                name: matchingParameter,
                type: newPropertyType || additionalProperties?.[0]?.type || ('STRING' as PropertyTypeModel),
            };
        });

        setSubProperties((subProperties) => {
            const newSubProperties = subProperties.filter(
                (subProperty) =>
                    !preexistingProperties.some((preexistingProperty) => preexistingProperty.name === subProperty.name)
            );

            return [...newSubProperties, ...preexistingProperties];
        });

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    if (!subProperties?.length && !additionalProperties?.length) {
        return <></>;
    }

    return (
        <>
            <ul
                className={twMerge('space-y-4', label && 'ml-2 border-l', subProperties?.length && 'pb-2 pt-1')}
                key={name}
            >
                {(subProperties as unknown as Array<PropertyType>)?.map((subProperty, index) => {
                    if (
                        subProperty.controlType === 'OBJECT_BUILDER' &&
                        !subProperty.additionalProperties?.length &&
                        !subProperty.properties?.length
                    ) {
                        return <></>;
                    }

                    return (
                        <div className="relative flex w-full" key={`${property.name}_${subProperty.name}_${index}`}>
                            <Property
                                actionName={actionName}
                                currentComponent={currentComponent}
                                currentComponentData={currentComponentData}
                                customClassName={twMerge('w-full last-of-type:pb-0', label && 'mb-0 pl-2')}
                                dataPills={dataPills}
                                mention={controlType === 'FILE_ENTRY' ? true : !!dataPills?.length}
                                objectName={name}
                                property={{
                                    ...subProperty,
                                    name: subProperty.name,
                                }}
                                updateWorkflowMutation={updateWorkflowMutation}
                            />

                            <Button
                                className="absolute bottom-0 right-0 ml-1"
                                onClick={() => handleDeletePropertyClick(subProperty.name!, name!)}
                                size="icon"
                                variant="ghost"
                            >
                                <Tooltip>
                                    <TooltipTrigger>
                                        <XIcon className="size-8 cursor-pointer p-2 hover:text-red-500" />
                                    </TooltipTrigger>

                                    <TooltipContent>Delete property</TooltipContent>
                                </Tooltip>
                            </Button>
                        </div>
                    );
                })}
            </ul>

            {!!additionalProperties?.length && multipleValues && (
                <Popover>
                    <PopoverTrigger asChild>
                        <Button
                            className="mt-2 rounded-sm bg-gray-100 text-xs font-medium hover:bg-gray-200"
                            size="sm"
                            variant="ghost"
                        >
                            <PlusIcon className="size-4" /> Add property
                        </Button>
                    </PopoverTrigger>

                    <PopoverContent className="min-w-[400px]">
                        <header className="flex items-center justify-between">
                            <span className="font-medium">Add property</span>

                            <PopoverClose asChild>
                                <Cross2Icon
                                    aria-hidden="true"
                                    className="size-4 cursor-pointer"
                                    onClick={() => setNewPropertyName('')}
                                />
                            </PopoverClose>
                        </header>

                        <main className="my-2 space-y-2">
                            <PropertyInput
                                className="mb-2"
                                label="Name"
                                name="additionalPropertyName"
                                onChange={(event) => setNewPropertyName(event.target.value)}
                                placeholder="Name for the additional property"
                                value={newPropertyName}
                            />

                            {(additionalProperties as Array<PropertyType>)?.length > 1 ? (
                                <PropertySelect
                                    label="Type"
                                    onValueChange={(value) => setNewPropertyType(value)}
                                    options={(additionalProperties as Array<PropertyType>).map(
                                        (additionalProperty) => ({
                                            label: additionalProperty.type!,
                                            value: additionalProperty.type!,
                                        })
                                    )}
                                    value={newPropertyType}
                                />
                            ) : (
                                <div className="flex w-full flex-col">
                                    <span className="mb-1 text-sm font-medium text-gray-700">Type</span>

                                    {additionalProperties[0] && (
                                        <span className="inline-flex w-full rounded-md bg-white py-2 text-sm">
                                            {additionalProperties[0].type}
                                        </span>
                                    )}
                                </div>
                            )}
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
            )}
        </>
    );
};

export default ObjectProperty;
