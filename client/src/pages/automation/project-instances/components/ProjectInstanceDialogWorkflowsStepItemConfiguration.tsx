import {ProjectInstanceModel, WorkflowModel} from '@/middleware/automation/configuration';
import {ControlTypeModel, PropertyTypeModel} from '@/middleware/platform/configuration';
import Properties from '@/pages/platform/workflow-editor/components/Properties/Properties';
import {PropertyType} from '@/types/types';
import {UseFormRegister} from 'react-hook-form';
import {FieldValues} from 'react-hook-form/dist/types';
import {FormState} from 'react-hook-form/dist/types/form';

export interface ProjectInstanceDialogWorkflowsStepItemConfigurationProps {
    formState: FormState<FieldValues>;
    register: UseFormRegister<ProjectInstanceModel>;
    workflow: WorkflowModel;
    workflowIndex: number;
}

const ProjectInstanceDialogWorkflowsStepItemConfiguration = ({
    formState,
    register,
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
            formState={formState}
            path={`projectInstanceWorkflows.${workflowIndex!}.inputs`}
            properties={properties}
            register={register}
        />
    ) : (
        <p className="text-sm">No defined configuration inputs.</p>
    );
};

export default ProjectInstanceDialogWorkflowsStepItemConfiguration;
