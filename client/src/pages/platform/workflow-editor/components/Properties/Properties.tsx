import {PropertyType} from '@/types/types';
import {FieldValues} from 'react-hook-form/dist/types';
import {Control, FormState} from 'react-hook-form/dist/types/form';
import {twMerge} from 'tailwind-merge';

import useWorkflowNodeDetailsPanelStore from '../../stores/useWorkflowNodeDetailsPanelStore';
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

const Properties = ({control, customClassName, formState, operationName, path, properties}: PropertiesProps) => {
    const {currentComponent} = useWorkflowNodeDetailsPanelStore();

    return (
        <ul className={twMerge('space-y-4', customClassName)}>
            {properties.map((property, index) => {
                const {displayCondition, name} = property;

                if (!name) {
                    return <></>;
                }

                if (displayCondition && !currentComponent?.displayConditions?.[displayCondition]) {
                    return <></>;
                }

                return (
                    <Property
                        control={control}
                        formState={formState}
                        key={`${name}_${index}`}
                        operationName={operationName}
                        path={path}
                        property={property}
                    />
                );
            })}
        </ul>
    );
};

export default Properties;
