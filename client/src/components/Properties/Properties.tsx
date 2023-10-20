import {ComponentDataType, DataPillType} from '@/types/types';
import {ChangeEvent} from 'react';
import {FieldValues} from 'react-hook-form/dist/types';
import {FormState, UseFormRegister} from 'react-hook-form/dist/types/form';
import {twMerge} from 'tailwind-merge';
import {PropertyType} from 'types/projectTypes';

import Property from './Property';

interface PropertiesProps {
    actionName?: string;
    currentComponentData?: ComponentDataType;
    customClassName?: string;
    dataPills?: DataPillType[];
    formState?: FormState<FieldValues>;
    mention?: boolean;
    onChange?: (event: ChangeEvent<HTMLInputElement>) => void;
    path?: string;
    properties: Array<PropertyType>;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    register?: UseFormRegister<any>;
}

const Properties = ({
    actionName,
    currentComponentData,
    customClassName,
    dataPills,
    formState,
    mention,
    onChange,
    path,
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
                    path={path}
                    property={property}
                    register={register}
                />
            );
        })}
    </ul>
);

export default Properties;
