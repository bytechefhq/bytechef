import {
    ComponentDataType,
    CurrentComponentType,
    DataPillType,
} from '@/types/types';
import {ChangeEvent} from 'react';
import {FieldValues} from 'react-hook-form/dist/types';
import {FormState, UseFormRegister} from 'react-hook-form/dist/types/form';
import {twMerge} from 'tailwind-merge';
import {PropertyType} from 'types/projectTypes';

import Property from './Property';

interface PropertiesProps {
    actionName?: string;
    currentComponent?: CurrentComponentType;
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
    currentComponent,
    currentComponentData,
    customClassName,
    dataPills,
    formState,
    mention,
    path,
    properties,
    register,
}: PropertiesProps) => (
    <ul className={twMerge('space-y-4', customClassName)}>
        {properties.map((property, index) => (
            <Property
                actionName={actionName}
                currentComponent={currentComponent}
                currentComponentData={currentComponentData}
                dataPills={dataPills}
                formState={formState}
                key={`${property.name}_${index}`}
                mention={mention}
                path={path}
                property={property}
                register={register}
            />
        ))}
    </ul>
);

export default Properties;
