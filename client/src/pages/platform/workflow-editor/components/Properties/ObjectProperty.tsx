import {Button} from '@/components/ui/button';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {ControlTypeModel} from '@/middleware/platform/configuration';
import PropertyInput from '@/pages/platform/workflow-editor/components/Properties/components/PropertyInput/PropertyInput';
import PropertySelect from '@/pages/platform/workflow-editor/components/Properties/components/PropertySelect';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {PROPERTY_CONTROL_TYPES} from '@/shared/constants';
import {PropertyType, SubPropertyType} from '@/types/types';
import {Cross2Icon, PlusIcon} from '@radix-ui/react-icons';
import {PopoverClose} from '@radix-ui/react-popover';
import {useEffect, useState} from 'react';
import {twMerge} from 'tailwind-merge';

import getParameterByPath from '../../utils/getParameterByPath';
import Property from './Property';
import DeletePropertyButton from './components/DeletePropertyButton';

interface ObjectPropertyProps {
    operationName?: string;
    arrayIndex?: number;
    arrayName?: string;
    onDeleteClick?: (path: string, name: string) => void;
    path?: string;
    property: PropertyType;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    parameterValue?: any;
}

const ObjectProperty = ({
    arrayIndex,
    arrayName,
    onDeleteClick,
    operationName,
    parameterValue,
    path,
    property,
}: ObjectPropertyProps) => {
    const [subProperties, setSubProperties] = useState<Array<PropertyType>>(
        (property.properties as Array<PropertyType>) || []
    );
    const [newPropertyName, setNewPropertyName] = useState('');
    const [newPropertyType, setNewPropertyType] = useState<keyof typeof PROPERTY_CONTROL_TYPES>(
        (property.additionalProperties?.[0]?.type as keyof typeof PROPERTY_CONTROL_TYPES) || 'STRING'
    );

    const {currentComponent} = useWorkflowNodeDetailsPanelStore();

    const {additionalProperties, label, name, properties} = property;

    const handleAddItemClick = () => {
        const newItem: SubPropertyType = {
            additionalProperties,
            controlType: PROPERTY_CONTROL_TYPES[newPropertyType] as ControlTypeModel,
            custom: true,
            expressionEnabled: true,
            label: newPropertyName,
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
        if (!name || !path || !currentComponent?.parameters) {
            return;
        }

        let value = getParameterByPath(path, currentComponent);

        if (value && arrayName && arrayIndex) {
            value = value[arrayIndex];
        } else if (value && name !== '__item') {
            value = value[name];
        }

        if (!value) {
            return;
        }

        const objectParameters = properties?.length ? properties?.map((property) => property.name) : Object.keys(value);

        const preexistingProperties = objectParameters.map((parameter) => {
            const matchingProperty = (properties as Array<PropertyType>)?.find(
                (property) => property.name === parameter
            );

            if (matchingProperty) {
                return {
                    ...matchingProperty,
                    defaultValue: value[parameter!],
                };
            } else {
                return {
                    controlType: PROPERTY_CONTROL_TYPES[newPropertyType] as ControlTypeModel,
                    custom: true,
                    defaultValue: value[parameter!],
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

    let availablePropertyTypes = additionalProperties?.length
        ? additionalProperties?.reduce((types: Array<{label: string; value: string}>, property) => {
              if (property.type) {
                  types.push({
                      label: property.type,
                      value: property.type,
                  });
              }

              return types;
          }, [])
        : Object.keys(PROPERTY_CONTROL_TYPES).map((type) => ({
              label: type,
              value: type,
          }));

    if (properties?.length) {
        const hasCustomProperty = (properties as Array<PropertyType>).find((property) => property.custom);

        if (!hasCustomProperty) {
            availablePropertyTypes = [];
        }
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
                                arrayIndex={arrayIndex}
                                arrayName={arrayName}
                                customClassName={twMerge(
                                    'w-full last-of-type:pb-0',
                                    label && 'mb-0',
                                    name === '__item' ? 'pb-0' : !arrayName && 'pl-2'
                                )}
                                inputTypeSwitchButtonClassName={subProperty.custom ? 'mr-6' : undefined}
                                objectName={name}
                                operationName={operationName}
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

            {!!availablePropertyTypes?.length && (
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
                                required
                                value={newPropertyName}
                            />

                            {availablePropertyTypes?.length > 1 ? (
                                <PropertySelect
                                    label="Type"
                                    onValueChange={(value) =>
                                        setNewPropertyType(value as keyof typeof PROPERTY_CONTROL_TYPES)
                                    }
                                    options={availablePropertyTypes.map((property) => ({
                                        label: property.value!,
                                        value: property.value!,
                                    }))}
                                    value={newPropertyType}
                                />
                            ) : (
                                <div className="flex w-full flex-col">
                                    <span className="mb-1 text-sm font-medium text-gray-700">Type</span>

                                    {availablePropertyTypes[0] && (
                                        <span className="inline-flex w-full rounded-md bg-white py-2 text-sm">
                                            {availablePropertyTypes[0].value}
                                        </span>
                                    )}
                                </div>
                            )}
                        </main>

                        <footer className="flex items-center justify-end space-x-2">
                            <PopoverClose asChild>
                                <Button disabled={!newPropertyName} onClick={handleAddItemClick} size="sm">
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
