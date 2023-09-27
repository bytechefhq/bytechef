import {TagModel} from '@/middleware/helios/configuration';
import {DataPillType} from '@/types/types';
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
    customClassName?: string;
    dataPills?: DataPillType[];
    formState?: FormState<FieldValues>;
    mention?: boolean;
    properties: Array<PropertyType>;
    register?: UseFormRegister<PropertyFormProps>;
}

const Properties = ({
    actionName,
    customClassName,
    dataPills,
    formState,
    mention,
    properties,
    register,
}: PropertiesProps) => (
    <ul className={twMerge('h-full', customClassName)}>
        {properties.map((property, index) => (
            <Property
                actionName={actionName}
                dataPills={dataPills}
                formState={formState}
                key={`${property.name}_${index}`}
                mention={mention}
                property={property}
                register={register}
            />
        ))}
    </ul>
);

export default Properties;
