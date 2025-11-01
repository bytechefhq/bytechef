import {IntegrationInstanceConfiguration} from '@/ee/shared/middleware/embedded/configuration';
import Properties from '@/pages/platform/workflow-editor/components/properties/Properties';
import {ControlType, PropertyType, Workflow} from '@/shared/middleware/platform/configuration';
import {PropertyAllType} from '@/shared/types';
import {Control, FieldValues, FormState} from 'react-hook-form';

export interface IntegrationInstanceConfigurationDialogWorkflowsStepItemConfigurationProps {
    control: Control<IntegrationInstanceConfiguration>;
    formState: FormState<FieldValues>;
    workflow: Workflow;
    workflowIndex: number;
}

const IntegrationInstanceConfigurationDialogWorkflowsStepItemInputs = ({
    control,
    formState,
    workflow,
    workflowIndex,
}: IntegrationInstanceConfigurationDialogWorkflowsStepItemConfigurationProps) => {
    const properties = workflow.inputs?.map((input) => {
        if (input.type === 'boolean') {
            return {
                ...input,
                controlType: ControlType.Select,
                type: PropertyType.Boolean,
            } as PropertyAllType;
        } else if (input.type === 'date') {
            return {
                ...input,
                controlType: ControlType.Date,
                type: PropertyType.Date,
            } as PropertyAllType;
        } else if (input.type === 'date_time') {
            return {
                ...input,
                controlType: ControlType.DateTime,
                type: PropertyType.DateTime,
            } as PropertyAllType;
        } else if (input.type === 'integer') {
            return {
                ...input,
                controlType: ControlType.Integer,
                type: PropertyType.Integer,
            } as PropertyAllType;
        } else if (input.type === 'number') {
            return {
                ...input,
                controlType: ControlType.Number,
                type: PropertyType.Number,
            } as PropertyAllType;
        } else if (input.type === 'string') {
            return {
                ...input,
                controlType: ControlType.Text,
                type: PropertyType.String,
            } as PropertyAllType;
        } else {
            return {
                ...input,
                controlType: ControlType.Time,
                type: PropertyType.Time,
            } as PropertyAllType;
        }
    });

    return properties?.length ? (
        <Properties
            control={control}
            controlPath={`integrationInstanceConfigurationWorkflows.${workflowIndex!}.inputs`}
            formState={formState}
            properties={properties}
        />
    ) : (
        <p className="text-sm">No defined configuration inputs.</p>
    );
};

export default IntegrationInstanceConfigurationDialogWorkflowsStepItemInputs;
