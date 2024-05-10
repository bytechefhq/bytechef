import {PropertyType} from '@/types/types';
import {FieldValues} from 'react-hook-form/dist/types';
import {Control, FormState} from 'react-hook-form/dist/types/form';
import {twMerge} from 'tailwind-merge';

import Property from './Property';

interface PropertiesProps {
    /* eslint-disable @typescript-eslint/no-explicit-any */
    control?: Control<any, any>;
    customClassName?: string;
    operationName?: string;
    formState?: FormState<FieldValues>;
    path?: string;
    properties: Array<PropertyType>;
}

const Properties = ({control, customClassName, formState, operationName, path, properties}: PropertiesProps) => (
    <ul className={twMerge('space-y-4', customClassName)}>
        {properties.map((property, index) => (
            <Property
                control={control}
                formState={formState}
                key={`${property.name}_${index}`}
                operationName={operationName}
                path={path}
                property={property}
            />
        ))}
    </ul>
);

export default Properties;
