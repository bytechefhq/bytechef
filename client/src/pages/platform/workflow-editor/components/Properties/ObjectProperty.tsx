import {Button} from '@/components/ui/button';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {ControlTypeModel} from '@/middleware/platform/configuration';
import PropertyInput from '@/pages/platform/workflow-editor/components/Properties/components/PropertyInput/PropertyInput';
import PropertySelect from '@/pages/platform/workflow-editor/components/Properties/components/PropertySelect';
import {
    ComponentType,
    CurrentComponentDefinitionType,
    DataPillType,
    PropertyType,
    SubPropertyType,
} from '@/types/types';
import {Cross2Icon, PlusIcon} from '@radix-ui/react-icons';
import {PopoverClose} from '@radix-ui/react-popover';
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
    currentComponent?: ComponentType;
    dataPills?: DataPillType[];
    onDeleteClick?: (path: string, name: string) => void;
    path?: string;
    property: PropertyType;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    parameterValue?: any;
}

const ObjectProperty = ({
    actionName,
    arrayIndex,
    arrayName,
    currentComponent,
    currentComponentDefinition,
    dataPills,
    onDeleteClick,
    parameterValue,
    path,
    property,
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

    const handleDeleteClick = (subProperty: SubPropertyType) => {
        setSubProperties((subProperties) => subProperties.filter((property) => property.name !== subProperty.name));

        if (onDeleteClick) {
            onDeleteClick(`${path}.${name}`, subProperty.name!);
        }
    };

    // render individual object items with data gathered from parameters
    useEffect(() => {
        if (!name || !currentComponent?.parameters?.[name]) {
            return;
        }

        const objectParameters = property.properties?.length
            ? property.properties?.map((property) => property.name)
            : Object.keys(currentComponent.parameters![name]);

        if (!objectParameters.length) {
            return;
        }

        const preexistingProperties = objectParameters.map((parameter) => {
            const matchingProperty = (property.properties as Array<PropertyType>)?.find(
                (property) => property.name === parameter
            );

            if (matchingProperty) {
                return {
                    ...matchingProperty,
                    defaultValue: currentComponent.parameters![name][parameter!],
                };
            } else {
                return {
                    controlType: PROPERTY_CONTROL_TYPES[newPropertyType] as ControlTypeModel,
                    custom: true,
                    defaultValue: currentComponent.parameters![name][parameter!],
                    name: parameter,
                    type: newPropertyType,
                };
            }
        });

        if (preexistingProperties.length) {
            setSubProperties(preexistingProperties);
        }

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    if (!subProperties?.length && !additionalProperties?.length) {
        return <></>;
    }

    return (
        <>
            <ul
                className={twMerge('space-y-4', label && name !== '__item' && 'ml-2 border-l')}
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

                    const subPropertyDefaultValue = subProperty.name ? parameterValue?.[subProperty.name] : '';

                    return (
                        <div className="relative flex w-full" key={`${property.name}_${subProperty.name}_${index}`}>
                            <Property
                                actionName={actionName}
                                arrayIndex={arrayIndex}
                                arrayName={arrayName}
                                currentComponent={currentComponent}
                                currentComponentDefinition={currentComponentDefinition}
                                customClassName={twMerge('w-full last-of-type:pb-0', label && 'mb-0 pl-2')}
                                dataPills={dataPills}
                                inputTypeSwitchButtonClassName={subProperty.custom ? 'mr-6' : undefined}
                                objectName={name}
                                parameterValue={subPropertyDefaultValue}
                                path={`${path}.${name}`}
                                property={{
                                    ...subProperty,
                                    name: subProperty.name,
                                }}
                                showDeletePropertyButton={true}
                            />

                            {subProperty.custom && name && subProperty.name && currentComponent && (
                                <DeletePropertyButton
                                    className="absolute right-0"
                                    currentComponent={currentComponent}
                                    onClick={() => handleDeleteClick(subProperty)}
                                    propertyName={name}
                                    subPropertyName={subProperty.name}
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
                            className="mt-3 rounded-sm bg-gray-100 text-xs font-medium hover:bg-gray-200"
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
                                required={true}
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
