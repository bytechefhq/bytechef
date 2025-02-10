import Properties from '@/pages/platform/workflow-editor/components/properties/Properties';
import {ProjectDeployment, Workflow} from '@/shared/middleware/automation/configuration';
import {ControlType, PropertyType} from '@/shared/middleware/platform/configuration';
import {PropertyAllType} from '@/shared/types';
import {FieldValues} from 'react-hook-form/dist/types';
import {Control, FormState} from 'react-hook-form/dist/types/form';

export interface ProjectDeploymentDialogWorkflowsStepItemConfigurationProps {
    control: Control<ProjectDeployment>;
    formState: FormState<FieldValues>;
    workflow: Workflow;
    workflowIndex: number;
}

const ProjectDeploymentDialogWorkflowsStepItemInputs = ({
    control,
    formState,
    workflow,
    workflowIndex,
}: ProjectDeploymentDialogWorkflowsStepItemConfigurationProps) => {
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
            controlPath={`projectDeploymentWorkflows.${workflowIndex!}.inputs`}
            formState={formState}
            properties={properties}
        />
    ) : (
        <p className="text-sm">No defined configuration inputs.</p>
    );
};

export default ProjectDeploymentDialogWorkflowsStepItemInputs;
