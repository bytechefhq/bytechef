import PropertyInput from '@/components/Properties/components/PropertyInput/PropertyInput';
import PropertySelect from '@/components/Properties/components/PropertySelect';
import {Button} from '@/components/ui/button';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {ComponentDataType, CurrentComponentType, DataPillType, PropertyType} from '@/types/types';
import {Cross2Icon, PlusIcon} from '@radix-ui/react-icons';
import {PopoverClose} from '@radix-ui/react-popover';
import {twMerge} from 'tailwind-merge';

import Property from './Property';

interface ObjectPropertyProps {
    actionName?: string;
    currentComponent?: CurrentComponentType;
    currentComponentData?: ComponentDataType;
    dataPills?: DataPillType[];
    property: PropertyType;
}

const ObjectProperty = ({
    actionName,
    currentComponent,
    currentComponentData,
    dataPills,
    property,
}: ObjectPropertyProps) => {
    const {additionalProperties, controlType, label, multipleValues, name, properties} = property;

    if (!properties?.length && !additionalProperties?.length) {
        return <></>;
    }

    return (
        <div key={name}>
            <ul className={twMerge('space-y-4', label && 'ml-2 border-l')}>
                {(properties as Array<PropertyType>)?.map((subProperty, index) => {
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
                        />
                    );
                })}
            </ul>

            {!!additionalProperties?.length && multipleValues && (
                <div className={twMerge(!!properties?.length && 'mt-2')}>
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
                            <header className="flex items-center justify-between py-2">
                                <span className="font-medium">Add property</span>

                                <PopoverClose asChild>
                                    <Cross2Icon aria-hidden="true" className="size-4 cursor-pointer" />
                                </PopoverClose>
                            </header>

                            <main className="space-y-2">
                                <PropertyInput
                                    label="Name"
                                    name="additionalPropertyName"
                                    placeholder="Name for the additional property"
                                />

                                {(additionalProperties as Array<PropertyType>)?.length > 1 ? (
                                    <PropertySelect
                                        label="Type"
                                        options={(additionalProperties as Array<PropertyType>).map(
                                            (additionalProperty) => ({
                                                label: additionalProperty.type!,
                                                value: additionalProperty.type!,
                                            })
                                        )}
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

                            <footer className="flex items-center justify-end space-x-2 py-2">
                                <Button onClick={() => console.log('save')} size="sm">
                                    Add
                                </Button>
                            </footer>
                        </PopoverContent>
                    </Popover>
                </div>
            )}
        </div>
    );
};

export default ObjectProperty;
