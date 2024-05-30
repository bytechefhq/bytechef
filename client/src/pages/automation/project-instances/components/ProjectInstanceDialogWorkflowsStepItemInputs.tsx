import Properties from '@/pages/platform/workflow-editor/components/Properties/Properties';
import {ProjectInstanceModel, WorkflowModel} from '@/shared/middleware/automation/configuration';
import {ControlTypeModel, PropertyTypeModel} from '@/shared/middleware/platform/configuration';
import {PropertyType} from '@/shared/types';
import {FieldValues} from 'react-hook-form/dist/types';
import {Control, FormState} from 'react-hook-form/dist/types/form';

export interface ProjectInstanceDialogWorkflowsStepItemConfigurationProps {
    control: Control<ProjectInstanceModel>;
    formState: FormState<FieldValues>;
    workflow: WorkflowModel;
    workflowIndex: number;
}

const ProjectInstanceDialogWorkflowsStepItemInputs = ({
    control,
    formState,
    workflow,
    workflowIndex,
}: ProjectInstanceDialogWorkflowsStepItemConfigurationProps) => {
    const properties = workflow.inputs?.map((input) => {
        if (input.type === 'boolean') {
            return {
                ...input,
                controlType: ControlTypeModel.Select,
                type: PropertyTypeModel.Boolean,
            } as PropertyType;
        } else if (input.type === 'date') {
            return {
                ...input,
                controlType: ControlTypeModel.Date,
                type: PropertyTypeModel.Date,
            } as PropertyType;
        } else if (input.type === 'date_time') {
            return {
                ...input,
                controlType: ControlTypeModel.DateTime,
                type: PropertyTypeModel.DateTime,
            } as PropertyType;
        } else if (input.type === 'integer') {
            return {
                ...input,
                controlType: ControlTypeModel.Integer,
                type: PropertyTypeModel.Integer,
            } as PropertyType;
        } else if (input.type === 'number') {
            return {
                ...input,
                controlType: ControlTypeModel.Number,
                type: PropertyTypeModel.Number,
            } as PropertyType;
        } else if (input.type === 'string') {
            return {
                ...input,
                controlType: ControlTypeModel.Text,
                type: PropertyTypeModel.String,
            } as PropertyType;
        } else {
            return {
                ...input,
                controlType: ControlTypeModel.Time,
                type: PropertyTypeModel.Time,
            } as PropertyType;
        }
    });

    return properties?.length ? (
        <Properties
            control={control}
            formState={formState}
            path={`projectInstanceWorkflows.${workflowIndex!}.inputs`}
            properties={properties}
        />
    ) : (
        <p className="text-sm">No defined configuration inputs.</p>
    );
};

export default ProjectInstanceDialogWorkflowsStepItemInputs;
