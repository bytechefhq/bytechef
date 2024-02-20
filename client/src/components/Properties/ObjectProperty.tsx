import PropertyInput from '@/components/Properties/components/PropertyInput/PropertyInput';
import PropertySelect from '@/components/Properties/components/PropertySelect';
import {Button} from '@/components/ui/button';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {UpdateWorkflowRequest, WorkflowModel} from '@/middleware/automation/configuration';
import {ComponentDataType, CurrentComponentType, DataPillType, PropertyType} from '@/types/types';
import {Cross2Icon, PlusIcon} from '@radix-ui/react-icons';
import {PopoverClose} from '@radix-ui/react-popover';
import {UseMutationResult} from '@tanstack/react-query';
import {useEffect, useState} from 'react';
import {twMerge} from 'tailwind-merge';

import Property from './Property';

interface ObjectPropertyProps {
    actionName?: string;
    currentComponent?: CurrentComponentType;
    currentComponentData?: ComponentDataType;
    dataPills?: DataPillType[];
    property: PropertyType;
    updateWorkflowMutation?: UseMutationResult<WorkflowModel, Error, UpdateWorkflowRequest, unknown>;
}

const ObjectProperty = ({
    actionName,
    currentComponent,
    currentComponentData,
    dataPills,
    property,
    updateWorkflowMutation,
}: ObjectPropertyProps) => {
    const [subProperties, setSubProperties] = useState(property.properties);
    const [newPropertyName, setNewPropertyName] = useState('');
    const [newPropertyType, setNewPropertyType] = useState('');

    const {additionalProperties, controlType, label, multipleValues, name} = property;

    const handleAddItemClick = () => {
        setSubProperties([
            ...subProperties!,
            {
                controlType: 'TEXT',
                name: newPropertyName,
                type: newPropertyType || additionalProperties?.[0].type,
            },
        ]);

        setNewPropertyName('');
    };

    useEffect(() => {
        if (!currentComponentData?.parameters) {
            return;
        }

        const parameterKeys = Object.keys(currentComponentData?.parameters);

        if (!currentComponentData?.parameters || !parameterKeys.length) {
            return;
        }

        const parameterSubProperties = parameterKeys
            .filter((parameterKey) => parameterKey.includes(`${name}.`))
            .map((parameterKey) => {
                const parameterName = parameterKey.split(`${name}.`)[1];

                return {
                    controlType: 'TEXT',
                    name: parameterName,
                    type: newPropertyType || additionalProperties?.[0].type,
                };
            });

        if (parameterSubProperties?.length) {
            setSubProperties(parameterSubProperties);
        }
    }, [additionalProperties, currentComponentData?.parameters, name, newPropertyType]);

    if (!subProperties?.length && !additionalProperties?.length) {
        return <></>;
    }

    return (
        <>
            <ul className={twMerge('space-y-4', label && 'ml-2 border-l', subProperties?.length && 'pb-2')} key={name}>
                {(subProperties as Array<PropertyType>)?.map((subProperty, index) => {
                    if (
                        subProperty.type === 'OBJECT' &&
                        !subProperty.additionalProperties?.length &&
                        !subProperty.properties?.length
                    ) {
                        return <></>;
                    }

                    return (
                        <Property
                            actionName={actionName}
                            currentComponent={currentComponent}
                            currentComponentData={currentComponentData}
                            customClassName={twMerge('last-of-type:pb-0', label && 'mb-0 pl-2')}
                            dataPills={dataPills}
                            key={`${property.name}_${subProperty.name}_${index}`}
                            mention={controlType === 'FILE_ENTRY' ? true : !!dataPills?.length}
                            property={{
                                ...subProperty,
                                name: `${property.name}.${subProperty.name}`,
                            }}
                            updateWorkflowMutation={updateWorkflowMutation}
                        />
                    );
                })}
            </ul>

            {!!additionalProperties?.length && multipleValues && (
                <Popover>
                    <PopoverTrigger asChild>
                        <Button
                            className="rounded-sm bg-gray-100 text-xs font-medium hover:bg-gray-200"
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

                                    <span className="inline-flex w-full rounded-md bg-white py-2 text-sm">
                                        {additionalProperties[0]?.type}
                                    </span>
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
