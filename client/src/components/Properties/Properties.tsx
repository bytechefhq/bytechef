import {UpdateWorkflowRequest, WorkflowModel} from '@/middleware/automation/configuration';
import {ComponentType, CurrentComponentDefinitionType, DataPillType, PropertyType} from '@/types/types';
import {UseMutationResult} from '@tanstack/react-query';
import {ChangeEvent} from 'react';
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
    onChange?: (event: ChangeEvent<HTMLInputElement>) => void;
    path?: string;
    properties: Array<PropertyType>;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    register?: UseFormRegister<any>;
    updateWorkflowMutation?: UseMutationResult<WorkflowModel, Error, UpdateWorkflowRequest, unknown>;
}

const Properties = ({
    actionName,
    currentComponent,
    currentComponentDefinition,
    customClassName,
    dataPills,
    formState,
    path,
    properties,
    register,
    updateWorkflowMutation,
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
                path={path}
                property={property}
                register={register}
                updateWorkflowMutation={updateWorkflowMutation}
            />
        ))}
    </ul>
);

export default Properties;
