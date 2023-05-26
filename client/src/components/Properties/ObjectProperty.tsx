import {PlusIcon} from '@heroicons/react/24/outline';
import Button from 'components/Button/Button';
import ContextualDialog from 'components/ContextualDialog/ContextualDialog';
import Input from 'components/Input/Input';
import Select from 'components/Select/Select';
import {useState} from 'react';
import {twMerge} from 'tailwind-merge';
import {PropertyType} from 'types/projectTypes';

import {Property} from './Properties';

const ObjectProperty = ({property}: {property: PropertyType}) => {
    const [additionalPropertiesDialogOpen, setAdditionalPropertiesDialogOpen] =
        useState(false);

    const {additionalProperties, label, properties} = property;

    return (
        <>
            <ul className={twMerge(label && 'ml-2 border-l')}>
                {(properties as PropertyType[])?.map((subProperty, index) => {
                    if (
                        subProperty.type === 'OBJECT' &&
                        !subProperty.additionalProperties?.length &&
                        !subProperty.properties?.length
                    ) {
                        return <></>;
                    }

                    return (
                        <Property
                            customClassName={twMerge(
                                'last-of-type:pb-0',
                                label && 'mb-0 pb-4 pl-2'
                            )}
                            key={`${subProperty.name}_${index}`}
                            property={subProperty}
                        />
                    );
                })}
            </ul>

            {!!additionalProperties?.length && (
                <div className="relative w-full self-start">
                    <Button
                        displayType="unstyled"
                        label="Add property"
                        size="small"
                        className="rounded-sm bg-gray-100 text-xs font-medium hover:bg-gray-200"
                        onClick={() => setAdditionalPropertiesDialogOpen(true)}
                        iconPosition="left"
                        icon={<PlusIcon className="h-4 w-4" />}
                    />

                    {additionalPropertiesDialogOpen && (
                        <div className="absolute z-50 w-3/4 rounded-md bg-gray-100 shadow-md">
                            <ContextualDialog
                                handleCancelClick={() =>
                                    setAdditionalPropertiesDialogOpen(false)
                                }
                                handleSaveClick={() => console.log('save')}
                                saveButtonLabel="Add"
                                title="Add property"
                            >
                                <Input
                                    placeholder="Name for the additional property"
                                    name="additionalPropertyName"
                                    label="Name"
                                />

                                {(additionalProperties as PropertyType[])
                                    ?.length > 1 ? (
                                    <Select
                                        label="Type"
                                        options={(
                                            additionalProperties as PropertyType[]
                                        ).map((type) => ({
                                            label: type.type!,
                                            value: type.type!,
                                        }))}
                                        triggerClassName="w-full"
                                    />
                                ) : (
                                    <div className="flex w-full flex-col">
                                        <span className="mb-1 text-sm font-medium text-gray-700">
                                            Type
                                        </span>

                                        <span className="inline-flex w-full rounded-md bg-white p-2 text-sm">
                                            {additionalProperties[0]?.type}
                                        </span>
                                    </div>
                                )}
                            </ContextualDialog>
                        </div>
                    )}
                </div>
            )}
        </>
    );
};

export default ObjectProperty;
