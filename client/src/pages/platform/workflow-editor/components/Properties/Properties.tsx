import {PropertyType} from '@/types/types';
import {FieldValues} from 'react-hook-form/dist/types';
import {FormState, UseFormRegister} from 'react-hook-form/dist/types/form';
import {twMerge} from 'tailwind-merge';

import Property from './Property';

interface PropertiesProps {
    operationName?: string;
    customClassName?: string;
    formState?: FormState<FieldValues>;
    path?: string;
    properties: Array<PropertyType>;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    register?: UseFormRegister<any>;
}

const Properties = ({customClassName, formState, operationName, path, properties, register}: PropertiesProps) => (
    <ul className={twMerge('space-y-4', customClassName)}>
        {properties.map((property, index) => (
            <Property
                formState={formState}
                key={`${property.name}_${index}`}
                operationName={operationName}
                path={path}
                property={property}
                register={register}
            />
        ))}
    </ul>
);

export default Properties;
