import {Button} from '@/components/ui/button';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import PropertyInput from '@/pages/platform/workflow-editor/components/Properties/components/PropertyInput/PropertyInput';
import PropertySelect from '@/pages/platform/workflow-editor/components/Properties/components/PropertySelect';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {VALUE_PROPERTY_CONTROL_TYPES} from '@/shared/constants';
import {ControlTypeModel} from '@/shared/middleware/platform/configuration';
import {PropertyType, SubPropertyType} from '@/shared/types';
import {Cross2Icon, PlusIcon} from '@radix-ui/react-icons';
import {PopoverClose} from '@radix-ui/react-popover';
import {useEffect, useState} from 'react';
import {twMerge} from 'tailwind-merge';

import getParameterType from '../../utils/getParameterType';
import getParameterValueByPath from '../../utils/getParameterValueByPath';
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
    const [newPropertyType, setNewPropertyType] = useState<keyof typeof VALUE_PROPERTY_CONTROL_TYPES>(
        (property.additionalProperties?.[0]?.type as keyof typeof VALUE_PROPERTY_CONTROL_TYPES) || 'STRING'
    );

    const {currentComponent} = useWorkflowNodeDetailsPanelStore();

    const {additionalProperties, label, name, properties} = property;

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
        : Object.keys(VALUE_PROPERTY_CONTROL_TYPES).map((type) => ({
              label: type,
              value: type,
          }));

    if (properties?.length) {
        const hasCustomProperty = (properties as Array<PropertyType>).find((property) => property.custom);

        if (!hasCustomProperty) {
            availablePropertyTypes = [];
        }
    }

    if (!path) {
        path = name;
    }

    if (name === '__item' && path) {
        path = path.replace('.__item', '');
    }

    const handleAddItemClick = () => {
        const newItem: SubPropertyType = {
            additionalProperties,
            controlType: VALUE_PROPERTY_CONTROL_TYPES[newPropertyType] as ControlTypeModel,
            custom: true,
            expressionEnabled: true,
            label: newPropertyName,
            name: newPropertyName,
            type: (newPropertyType ||
                additionalProperties?.[0].type ||
                'STRING') as keyof typeof VALUE_PROPERTY_CONTROL_TYPES,
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

        let parameterObject = getParameterValueByPath(path, currentComponent.parameters);

        if (parameterObject && arrayName && arrayIndex) {
            parameterObject = parameterObject[arrayIndex];
        }

        if (!parameterObject) {
            return;
        }

        const objectParameters = properties?.length
            ? properties?.map((property) => property.name)
            : Object.keys(parameterObject);

        const preexistingProperties = objectParameters.map((parameter) => {
            const matchingProperty = (properties as Array<PropertyType>)?.find(
                (property) => property.name === parameter
            );

            if (matchingProperty) {
                return {
                    ...matchingProperty,
                    defaultValue: parameterObject[parameter!],
                };
            } else {
                const parameterItemType = getParameterType(parameterObject[parameter!]);

                return {
                    controlType: VALUE_PROPERTY_CONTROL_TYPES[
                        parameterItemType as keyof typeof VALUE_PROPERTY_CONTROL_TYPES
                    ] as ControlTypeModel,
                    custom: true,
                    defaultValue: parameterObject[parameter!],
                    expressionEnabled: true,
                    name: parameter,
                    type: parameterItemType,
                };
            }
        });

        if (preexistingProperties.length) {
            setSubProperties(preexistingProperties);
        }

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    return (
        <>
            <ul
                className={twMerge('space-y-4', label && name !== '__item' && 'ml-2 border-l')}
                key={`${name}_${newPropertyName}`}
            >
                {(subProperties as unknown as Array<SubPropertyType>)?.map((subProperty, index) => {
                    const subPropertyDefaultValue = subProperty.name ? parameterValue?.[subProperty.name] : '';

                    return (
                        <div
                            className={twMerge(
                                'relative flex w-full',
                                subProperty.controlType === 'OBJECT_BUILDER' && 'pl-2'
                            )}
                            key={`${property.name}_${subProperty.name}_${index}`}
                        >
                            <Property
                                arrayIndex={arrayIndex}
                                arrayName={arrayName}
                                customClassName={twMerge(
                                    'w-full last-of-type:pb-0',
                                    label && 'mb-0',
                                    name === '__item' ? 'pb-0' : !arrayName && 'pl-2'
                                )}
                                inputTypeSwitchButtonClassName={subProperty.custom ? 'mr-6' : ''}
                                objectName={arrayName ? '' : name}
                                operationName={operationName}
                                parameterValue={subPropertyDefaultValue}
                                path={`${path}.${subProperty.name}`}
                                property={{
                                    ...subProperty,
                                    name: subProperty.name,
                                }}
                                showDeletePropertyButton
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
                                        setNewPropertyType(value as keyof typeof VALUE_PROPERTY_CONTROL_TYPES)
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
