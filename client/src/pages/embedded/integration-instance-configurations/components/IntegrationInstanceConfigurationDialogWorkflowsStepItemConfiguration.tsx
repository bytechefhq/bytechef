import {IntegrationInstanceConfigurationModel} from '@/middleware/embedded/configuration';
import {WorkflowModel} from '@/middleware/platform/configuration';
import Properties from '@/pages/platform/workflow-editor/components/Properties/Properties';
import {PropertyType} from '@/types/types';
import {UseFormRegister} from 'react-hook-form';
import {FieldValues} from 'react-hook-form/dist/types';
import {FormState} from 'react-hook-form/dist/types/form';

export interface IntegrationInstanceConfigurationDialogWorkflowsStepItemConfigurationProps {
    formState: FormState<FieldValues>;
    register: UseFormRegister<IntegrationInstanceConfigurationModel>;
    workflow: WorkflowModel;
    workflowIndex: number;
}

const IntegrationInstanceConfigurationDialogWorkflowsStepItemConfiguration = ({
    formState,
    register,
    workflow,
    workflowIndex,
}: IntegrationInstanceConfigurationDialogWorkflowsStepItemConfigurationProps) => {
    return workflow.inputs?.length ? (
        <Properties
            formState={formState}
            path={`integrationInstanceConfigurationWorkflows.${workflowIndex!}.inputs`}
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

export default IntegrationInstanceConfigurationDialogWorkflowsStepItemConfiguration;
