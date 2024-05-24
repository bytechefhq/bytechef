import {FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Textarea} from '@/components/ui/textarea';
import ProjectInstanceDialogBasicStepProjectVersionsSelect from '@/pages/automation/project-instances/components/ProjectInstanceDialogBasicStepProjectVersionsSelect';
import ProjectInstanceDialogBasicStepProjectsComboBox from '@/pages/automation/project-instances/components/ProjectInstanceDialogBasicStepProjectsComboBox';
import ProjectInstanceDialogBasicStepTagsSelect from '@/pages/automation/project-instances/components/ProjectInstanceDialogBasicStepTagsSelect';
import {useWorkflowsEnabledStore} from '@/pages/automation/project-instances/stores/useWorkflowsEnabledStore';
import {ProjectInstanceModel} from 'middleware/automation/configuration';
import {useState} from 'react';
import {Control, UseFormGetValues, UseFormSetValue} from 'react-hook-form';
import {useShallow} from 'zustand/react/shallow';

interface ProjectDialogBasicStepProps {
    control: Control<ProjectInstanceModel>;
    getValues: UseFormGetValues<ProjectInstanceModel>;
    projectInstance: ProjectInstanceModel | undefined;
    setValue: UseFormSetValue<ProjectInstanceModel>;
    updateProjectVersion: boolean;
}

const ProjectInstanceDialogBasicStep = ({
    control,
    getValues,
    projectInstance,
    setValue,
    updateProjectVersion,
}: ProjectDialogBasicStepProps) => {
    const [curProjectId, setCurProjectId] = useState(getValues('projectId'));
    const [curProjectVersion, setCurProjectVersion] = useState<number | undefined>(getValues('projectVersion'));

    const [resetWorkflowsEnabledStore] = useWorkflowsEnabledStore(useShallow(({reset}) => [reset]));

    return (
        <div className="grid gap-4">
            {!projectInstance?.id && (
                <FormField
                    control={control}
                    name="projectId"
                    render={({field}) => (
                        <FormItem>
                            <FormLabel>Project</FormLabel>

                            <FormControl>
                                <ProjectInstanceDialogBasicStepProjectsComboBox
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

            {(!projectInstance?.id || updateProjectVersion) && (
                <FormField
                    control={control}
                    name="projectVersion"
                    render={({field}) => (
                        <FormItem>
                            <FormLabel>Version</FormLabel>

                            <FormControl>
                                <ProjectInstanceDialogBasicStepProjectVersionsSelect
                                    onChange={(value) => {
                                        field.onChange(value);
                                        setValue('projectInstanceWorkflows', []);
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

            {!projectInstance?.id && (
                <FormField
                    control={control}
                    name="environment"
                    render={({field}) => (
                        <FormItem>
                            <FormLabel>Environment</FormLabel>

                            <FormControl>
                                <Select defaultValue={field.value} onValueChange={(value) => field.onChange(value)}>
                                    <SelectTrigger className="w-full">
                                        <SelectValue placeholder="Select environment" />
                                    </SelectTrigger>

                                    <SelectContent>
                                        <SelectItem value="TEST">Test</SelectItem>

                                        <SelectItem value="PRODUCTION">Production</SelectItem>
                                    </SelectContent>
                                </Select>
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
                                <Textarea placeholder="Cute description of your project instance" {...field} />
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

                            <ProjectInstanceDialogBasicStepTagsSelect
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
                                projectInstance={projectInstance}
                            />

                            <FormMessage />
                        </FormItem>
                    )}
                />
            )}
        </div>
    );
};

export default ProjectInstanceDialogBasicStep;
