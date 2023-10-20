import {TagModel} from '@/middleware/helios/configuration';
import {ComponentDataType, DataPillType} from '@/types/types';
import {ChangeEvent} from 'react';
import {FieldValues} from 'react-hook-form/dist/types';
import {FormState, UseFormRegister} from 'react-hook-form/dist/types/form';
import {twMerge} from 'tailwind-merge';
import {PropertyType} from 'types/projectTypes';

import Property from './Property';

export interface PropertyFormProps {
    authorizationName: string;
    componentName: {
        value: string;
        label: string;
    };
    name: string;
    parameters: {[key: string]: object};
    tags: Array<TagModel | {label: string; value: string}>;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    [key: string]: any;
}

interface PropertiesProps {
    actionName?: string;
    currentComponentData?: ComponentDataType;
    customClassName?: string;
    dataPills?: DataPillType[];
    formState?: FormState<FieldValues>;
    mention?: boolean;
    onChange?: (event: ChangeEvent<HTMLInputElement>) => void;
    properties: Array<PropertyType>;
    register?: UseFormRegister<PropertyFormProps>;
}

const Properties = ({
    actionName,
    currentComponentData,
    customClassName,
    dataPills,
    formState,
    mention,
    onChange,
    properties,
    register,
}: PropertiesProps) => (
    <ul className={twMerge('h-full', customClassName)}>
        {properties.map((property, index) => {
            const defaultValue =
                currentComponentData?.properties?.[
                    property.name as keyof (typeof currentComponentData)['properties']
                ];

            return (
                <Property
                    actionName={actionName}
                    dataPills={dataPills}
                    defaultValue={defaultValue || ''}
                    formState={formState}
                    key={`${property.name}_${index}`}
                    mention={mention}
                    onChange={onChange}
                    property={property}
                    register={register}
                />
            );
        })}
    </ul>
);

export default Properties;
