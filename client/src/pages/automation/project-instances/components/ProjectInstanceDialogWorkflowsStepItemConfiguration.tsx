import Properties from '@/components/Properties/Properties';
import {ProjectInstanceModel, WorkflowModel} from '@/middleware/helios/configuration';
import {PropertyType} from '@/types/projectTypes';
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
    return workflow.inputs?.length ? (
        <Properties
            formState={formState}
            path={`projectInstanceWorkflows.${workflowIndex!}.inputs`}
            properties={workflow.inputs.map((input) => {
                if (input.type === 'string') {
                    return {
                        controlType: 'TEXT',
                        type: 'STRING',
                        ...input,
                    } as PropertyType;
                } else if (input.type === 'number') {
                    return {
                        type: 'NUMBER',
                        ...input,
                    } as PropertyType;
                } else {
                    return {
                        controlType: 'SELECT',
                        type: 'BOOLEAN',
                        ...input,
                    } as PropertyType;
                }
            })}
            register={register}
        />
    ) : (
        <p className="text-sm">No defined configuration inputs.</p>
    );
};

export default ProjectInstanceDialogWorkflowsStepItemConfiguration;
