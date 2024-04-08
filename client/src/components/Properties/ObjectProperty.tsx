import PropertyInput from '@/components/Properties/components/PropertyInput/PropertyInput';
import PropertySelect from '@/components/Properties/components/PropertySelect';
import {Button} from '@/components/ui/button';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {UpdateWorkflowRequest, WorkflowModel} from '@/middleware/automation/configuration';
import {ControlTypeModel} from '@/middleware/platform/configuration';
import {
    ComponentDataType,
    CurrentComponentDefinitionType,
    DataPillType,
    PropertyType,
    SubPropertyType,
} from '@/types/types';
import {Cross2Icon, PlusIcon} from '@radix-ui/react-icons';
import {PopoverClose} from '@radix-ui/react-popover';
import {UseMutationResult} from '@tanstack/react-query';
import {useEffect, useState} from 'react';
import {twMerge} from 'tailwind-merge';

import Property from './Property';
import DeletePropertyButton from './components/DeletePropertyButton';

const PROPERTY_CONTROL_TYPES = {
    ARRAY: 'ARRAY_BUILDER',
    BOOLEAN: 'BOOLEAN',
    DATE: 'DATE',
    DATE_TIME: 'DATE_TIME',
    INTEGER: 'INTEGER',
    NULL: 'NULL',
    NUMBER: 'NUMBER',
    OBJECT: 'OBJECT_BUILDER',
    STRING: 'TEXT',
    TIME: 'TIME',
};

interface ObjectPropertyProps {
    actionName?: string;
    arrayIndex?: number;
    arrayName?: string;
    currentComponentDefinition?: CurrentComponentDefinitionType;
    currentComponentData?: ComponentDataType;
    dataPills?: DataPillType[];
    path?: string;
    property: PropertyType;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    taskParameterValue?: any;
    updateWorkflowMutation?: UseMutationResult<WorkflowModel, Error, UpdateWorkflowRequest, unknown>;
}

const ObjectProperty = ({
    actionName,
    arrayIndex,
    arrayName,
    currentComponentData,
    currentComponentDefinition,
    dataPills,
    path,
    property,
    taskParameterValue,
    updateWorkflowMutation,
}: ObjectPropertyProps) => {
    const [subProperties, setSubProperties] = useState<Array<PropertyType>>(
        (property.properties as Array<PropertyType>) || []
    );
    const [newPropertyName, setNewPropertyName] = useState('');
    const [newPropertyType, setNewPropertyType] = useState<keyof typeof PROPERTY_CONTROL_TYPES>('STRING');

    const {additionalProperties, label, name} = property;

    const handleAddItemClick = () => {
        const newItem: SubPropertyType = {
            additionalProperties,
            controlType: PROPERTY_CONTROL_TYPES[newPropertyType] as ControlTypeModel,
            custom: true,
            expressionEnabled: true,
            name: newPropertyName,
            type: (newPropertyType ||
                additionalProperties?.[0].type ||
                'STRING') as keyof typeof PROPERTY_CONTROL_TYPES,
        };

        setSubProperties([...subProperties, newItem]);

        setNewPropertyName('');
    };

    // on initial render, set subProperties if there are matching parameters
    useEffect(() => {
        if (!name || !currentComponentData?.parameters?.[name]) {
            return;
        }

        const objectParameters = Object.keys(currentComponentData.parameters![name]);

        if (!objectParameters.length) {
            return;
        }

        const preexistingProperties = objectParameters.map((parameter) => {
            const value: string = currentComponentData.parameters![parameter];

            const matchingSubProperty = subProperties.find((subProperty) => subProperty.name === parameter);

            return {
                ...matchingSubProperty,
                defaultValue: value,
            };
        });

        setSubProperties((subProperties) =>
            subProperties.map((subProperty) => {
                if (preexistingProperties.find((property) => property.name === subProperty.name)) {
                    return {...subProperty, defaultValue: undefined};
                } else {
                    return subProperty;
                }
            })
        );

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    if (!subProperties?.length && !additionalProperties?.length) {
        return <></>;
    }

    return (
        <>
            <ul
                className={twMerge('space-y-4', label && 'ml-2 border-l', subProperties?.length && 'pb-2 pt-1')}
                key={`${name}_${newPropertyName}`}
            >
                {(subProperties as unknown as Array<SubPropertyType>)?.map((subProperty, index) => {
                    if (
                        subProperty.controlType === 'OBJECT_BUILDER' &&
                        !subProperty.additionalProperties?.length &&
                        !subProperty.properties?.length
                    ) {
                        return <></>;
                    }

                    const subPropertyDefaultValue = subProperty.name ? taskParameterValue?.[subProperty.name] : '';

                    return (
                        <div className="relative flex w-full" key={`${property.name}_${subProperty.name}_${index}`}>
                            <Property
                                actionName={actionName}
                                arrayIndex={arrayIndex}
                                arrayName={arrayName}
                                currentComponentData={currentComponentData}
                                currentComponentDefinition={currentComponentDefinition}
                                customClassName={twMerge('w-full last-of-type:pb-0', label && 'mb-0 pl-2')}
                                dataPills={dataPills}
                                inputTypeSwitchButtonClassName={subProperty.custom ? 'mr-6' : undefined}
                                objectName={name}
                                path={`${path}.${name}`}
                                property={{
                                    ...subProperty,
                                    name: subProperty.name,
                                }}
                                showDeletePropertyButton={true}
                                taskParameterValue={subPropertyDefaultValue}
                                updateWorkflowMutation={updateWorkflowMutation}
                            />

                            {subProperty.custom &&
                                name &&
                                subProperty.name &&
                                currentComponentData &&
                                updateWorkflowMutation && (
                                    <DeletePropertyButton
                                        currentComponentData={currentComponentData}
                                        handleDeletePropertyClick={() =>
                                            setSubProperties((subProperties) =>
                                                subProperties.filter((property) => property.name !== subProperty.name)
                                            )
                                        }
                                        propertyName={name}
                                        subPropertyName={subProperty.name}
                                        updateWorkflowMutation={updateWorkflowMutation}
                                    />
                                )}
                        </div>
                    );
                })}
            </ul>

            {!!additionalProperties?.length && (
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
                                    onValueChange={(value) =>
                                        setNewPropertyType(value as keyof typeof PROPERTY_CONTROL_TYPES)
                                    }
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
