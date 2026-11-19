import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import WorkflowNodeOptionsSelect from '@/shared/components/WorkflowNodeOptionsSelect';
import {useGetWorkflowNodeOptionsQuery} from '@/shared/queries/platform/workflowNodeOptions.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {PropertyAllType} from '@/shared/types';
import {InfoIcon} from 'lucide-react';
import {Control, Controller, FieldValues} from 'react-hook-form';

interface TestValueDynamicSelectPropsI {
    backingWorkflowNodeName: string;
    control: Control<FieldValues>;
    fieldName: string;
    member: PropertyAllType;
    workflowId: string;
}

const TestValueDynamicSelect = ({
    backingWorkflowNodeName,
    control,
    fieldName,
    member,
    workflowId,
}: TestValueDynamicSelectPropsI) => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {data: options, isLoading} = useGetWorkflowNodeOptionsQuery(
        {
            loadDependencyValueKey: '',
            request: {
                environmentId: currentEnvironmentId,
                id: workflowId,
                propertyName: member.name!,
                workflowNodeName: backingWorkflowNodeName,
            },
        },
        Boolean(workflowId && backingWorkflowNodeName)
    );

    return (
        <Controller
            control={control}
            name={fieldName}
            render={({field}) => (
                <WorkflowNodeOptionsSelect
                    isLoading={isLoading}
                    onChange={field.onChange}
                    options={options}
                    value={field.value}
                />
            )}
        />
    );
};

interface WorkflowInputComponentTestValuePropsI {
    backingWorkflowNodeName?: string;
    control: Control<FieldValues>;
    members: PropertyAllType[];
    workflowId?: string;
}

const WorkflowInputComponentTestValue = ({
    backingWorkflowNodeName,
    control,
    members,
    workflowId,
}: WorkflowInputComponentTestValuePropsI) => {
    const isLoneProperty = members.length === 1;

    return (
        <fieldset className="space-y-3 border-0 p-0">
            {members.map((member) => {
                const hasDynamicOptions = !!member.optionsDataSource;
                const fieldName = isLoneProperty ? 'testValue' : `testValue.${member.name}`;

                return (
                    <fieldset className="w-full space-y-1 border-0 p-0" key={member.name}>
                        <Label className="text-sm font-medium text-content-neutral-primary">
                            {member.label ?? member.name}
                        </Label>

                        {hasDynamicOptions && backingWorkflowNodeName && workflowId ? (
                            <TestValueDynamicSelect
                                backingWorkflowNodeName={backingWorkflowNodeName}
                                control={control}
                                fieldName={fieldName}
                                member={member}
                                workflowId={workflowId}
                            />
                        ) : (
                            <Controller
                                control={control}
                                name={fieldName}
                                render={({field}) => (
                                    <Input
                                        onChange={field.onChange}
                                        placeholder="Enter value"
                                        value={field.value ?? ''}
                                    />
                                )}
                            />
                        )}

                        {hasDynamicOptions && !backingWorkflowNodeName && (
                            <p className="flex items-center gap-1 text-sm font-light text-content-neutral-secondary">
                                <InfoIcon className="size-4" /> Configure a connection for this component to load live
                                options.
                            </p>
                        )}
                    </fieldset>
                );
            })}
        </fieldset>
    );
};

export default WorkflowInputComponentTestValue;
