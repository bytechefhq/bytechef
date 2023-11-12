import {
    ComponentDataType,
    CurrentComponentType,
    DataPillType,
} from '@/types/types';
import {PlusIcon} from '@radix-ui/react-icons';
import Button from 'components/Button/Button';
import ContextualDialog from 'components/ContextualDialog/ContextualDialog';
import Input from 'components/Input/Input';
import Select from 'components/Select/Select';
import {useState} from 'react';
import {twMerge} from 'tailwind-merge';
import {PropertyType} from 'types/projectTypes';

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
    const [additionalPropertiesDialogOpen, setAdditionalPropertiesDialogOpen] =
        useState(false);

    const {additionalProperties, label, name, properties} = property;

    return (
        <div key={name}>
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
                            actionName={actionName}
                            currentComponent={currentComponent}
                            currentComponentData={currentComponentData}
                            customClassName={twMerge(
                                'last-of-type:pb-0',
                                label && 'mb-0 pb-4 pl-2'
                            )}
                            dataPills={dataPills}
                            key={`${property.name}_${subProperty.name}_${index}`}
                            mention={!!dataPills?.length}
                            property={subProperty}
                        />
                    );
                })}
            </ul>

            {!!additionalProperties?.length && (
                <div
                    className={twMerge(
                        !!properties?.length && 'mt-2',
                        'relative w-full self-start'
                    )}
                >
                    <Button
                        className="rounded-sm bg-gray-100 text-xs font-medium hover:bg-gray-200"
                        displayType="unstyled"
                        icon={<PlusIcon className="h-4 w-4" />}
                        iconPosition="left"
                        label="Add property"
                        onClick={() => setAdditionalPropertiesDialogOpen(true)}
                        size="small"
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
                                    label="Name"
                                    name="additionalPropertyName"
                                    placeholder="Name for the additional property"
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

                                        <span className="inline-flex w-full rounded-md bg-white py-2 text-sm">
                                            {additionalProperties[0]?.type}
                                        </span>
                                    </div>
                                )}
                            </ContextualDialog>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
};

export default ObjectProperty;
