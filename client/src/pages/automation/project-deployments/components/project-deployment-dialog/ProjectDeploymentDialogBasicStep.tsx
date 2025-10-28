import {FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {Textarea} from '@/components/ui/textarea';
import ProjectDeploymentDialogBasicStepProjectVersionsSelect from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogBasicStepProjectVersionsSelect';
import ProjectDeploymentDialogBasicStepProjectsComboBox from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogBasicStepProjectsComboBox';
import ProjectDeploymentDialogBasicStepTagsSelect from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogBasicStepTagsSelect';
import {useWorkflowsEnabledStore} from '@/pages/automation/project-deployments/stores/useWorkflowsEnabledStore';
import EnvironmentBadge from '@/shared/components/EnvironmentBadge';
import {ProjectDeployment} from '@/shared/middleware/automation/configuration';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useState} from 'react';
import {Control, UseFormGetValues, UseFormSetValue} from 'react-hook-form';
import {useShallow} from 'zustand/react/shallow';

interface ProjectDialogBasicStepProps {
    control: Control<ProjectDeployment>;
    getValues: UseFormGetValues<ProjectDeployment>;
    projectDeployment: ProjectDeployment | undefined;
    setValue: UseFormSetValue<ProjectDeployment>;
    updateProjectVersion: boolean;
}

const ProjectDeploymentDialogBasicStep = ({
    control,
    getValues,
    projectDeployment,
    setValue,
    updateProjectVersion,
}: ProjectDialogBasicStepProps) => {
    const [curProjectId, setCurProjectId] = useState(getValues('projectId'));
    const [curProjectVersion, setCurProjectVersion] = useState<number | undefined>(getValues('projectVersion'));

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const [resetWorkflowsEnabledStore] = useWorkflowsEnabledStore(useShallow(({reset}) => [reset]));

    return (
        <div className="grid gap-4">
            {!projectDeployment?.id && (
                <FormField
                    control={control}
                    name="projectId"
                    render={({field}) => (
                        <FormItem>
                            <FormLabel>Project</FormLabel>

                            <FormControl>
                                <ProjectDeploymentDialogBasicStepProjectsComboBox
                                    apiCollections={false}
                                    onBlur={field.onBlur}
                                    onChange={(item) => {
                                        if (item) {
                                            resetWorkflowsEnabledStore();
                                            setValue('projectId', item.value);
                                            setValue('projectVersion', undefined);

                                            if (!getValues('name')) {
                                                setValue('name', item.name!.toString());
                                            }

                                            setCurProjectId(item.value);
                                            setCurProjectVersion(undefined);
                                        }
                                    }}
                                    value={field.value}
                                />
                            </FormControl>

                            <FormMessage />
                        </FormItem>
                    )}
                    rules={{required: true}}
                    shouldUnregister={false}
                />
            )}

            <FormField
                control={control}
                name="environmentId"
                render={() => (
                    <FormItem className="space-x-2">
                        <FormLabel>Environment</FormLabel>

                        <FormControl>
                            <EnvironmentBadge environmentId={currentEnvironmentId} />
                        </FormControl>

                        <FormMessage />
                    </FormItem>
                )}
            />

            {!updateProjectVersion && (
                <FormField
                    control={control}
                    name="name"
                    render={({field}) => (
                        <FormItem>
                            <FormLabel>Name</FormLabel>

                            <FormControl>
                                <Input
                                    onChange={(value) => {
                                        field.onChange(value);
                                    }}
                                    placeholder="My CRM Project"
                                    value={field.value}
                                />
                            </FormControl>

                            <FormMessage />
                        </FormItem>
                    )}
                    rules={{required: true}}
                />
            )}

            {curProjectId && (!projectDeployment?.id || updateProjectVersion) && (
                <FormField
                    control={control}
                    name="projectVersion"
                    render={({field}) => (
                        <FormItem>
                            <FormLabel>Version</FormLabel>

                            <FormControl>
                                <ProjectDeploymentDialogBasicStepProjectVersionsSelect
                                    onChange={(value) => {
                                        field.onChange(value);
                                        setValue('projectDeploymentWorkflows', []);
                                        setCurProjectVersion(value);
                                    }}
                                    projectId={curProjectId}
                                    projectVersion={curProjectVersion}
                                />
                            </FormControl>

                            <FormMessage />
                        </FormItem>
                    )}
                    rules={{required: true}}
                    shouldUnregister={false}
                />
            )}

            {!updateProjectVersion && (
                <FormField
                    control={control}
                    name="description"
                    render={({field}) => (
                        <FormItem>
                            <FormLabel>Description</FormLabel>

                            <FormControl>
                                <Textarea placeholder="Cute description of your project deployment" {...field} />
                            </FormControl>

                            <FormMessage />
                        </FormItem>
                    )}
                />
            )}

            {!updateProjectVersion && (
                <FormField
                    control={control}
                    name="tags"
                    render={({field}) => (
                        <FormItem>
                            <FormLabel>Tags</FormLabel>

                            <ProjectDeploymentDialogBasicStepTagsSelect
                                field={field}
                                onCreateOption={(inputValue: string) => {
                                    setValue('tags', [
                                        ...(getValues().tags ?? []),
                                        {
                                            label: inputValue,
                                            name: inputValue,
                                            value: inputValue,
                                        },
                                    ] as never[]);
                                }}
                                projectDeployment={projectDeployment}
                            />

                            <FormMessage />
                        </FormItem>
                    )}
                />
            )}
        </div>
    );
};

export default ProjectDeploymentDialogBasicStep;
