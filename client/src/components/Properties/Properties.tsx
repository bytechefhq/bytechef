import {ComponentType, CurrentComponentDefinitionType, DataPillType, PropertyType} from '@/types/types';
import {FieldValues} from 'react-hook-form/dist/types';
import {FormState, UseFormRegister} from 'react-hook-form/dist/types/form';
import {twMerge} from 'tailwind-merge';

import Property from './Property';

interface PropertiesProps {
    actionName?: string;
    currentComponentDefinition?: CurrentComponentDefinitionType;
    currentComponent?: ComponentType;
    customClassName?: string;
    dataPills?: DataPillType[];
    formState?: FormState<FieldValues>;
    onChange?: () => void;
    path?: string;
    properties: Array<PropertyType>;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    register?: UseFormRegister<any>;
}

const Properties = ({
    actionName,
    currentComponent,
    currentComponentDefinition,
    customClassName,
    dataPills,
    formState,
    onChange,
    path,
    properties,
    register,
}: PropertiesProps) => (
    <ul className={twMerge('space-y-4', customClassName)}>
        {properties.map((property, index) => (
            <Property
                actionName={actionName}
                currentComponent={currentComponent}
                currentComponentDefinition={currentComponentDefinition}
                dataPills={dataPills}
                formState={formState}
                key={`${property.name}_${index}`}
                onChange={onChange}
                path={path}
                property={property}
                register={register}
            />
        ))}
    </ul>
);

export default Properties;
