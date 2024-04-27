import ComboBox, {ComboBoxItemType} from '@/components/ComboBox';
import {FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Textarea} from '@/components/ui/textarea';
import {useWorkflowsEnabledStore} from '@/pages/automation/project-instances/stores/useWorkflowsEnabledStore';
import {useGetProjectInstanceTagsQuery} from '@/queries/automation/projectInstanceTags.queries';
import {useGetProjectVersionsQuery} from '@/queries/automation/projectVersions.queries';
import {useGetProjectsQuery} from '@/queries/automation/projects.queries';
import CreatableSelect from 'components/CreatableSelect/CreatableSelect';
import {ProjectInstanceModel, ProjectModel, ProjectStatusModel} from 'middleware/automation/configuration';
import {Dispatch, FocusEventHandler, SetStateAction} from 'react';
import {Control, UseFormGetValues, UseFormRegister, UseFormReturn, UseFormSetValue} from 'react-hook-form';
import {ControllerRenderProps} from 'react-hook-form/dist/types/controller';
import {useShallow} from 'zustand/react/shallow';

interface ProjectDialogBasicStepProps {
    control: Control<ProjectInstanceModel>;
    errors: UseFormReturn<ProjectInstanceModel>['formState']['errors'];
    getValues: UseFormGetValues<ProjectInstanceModel>;
    projectId: number | undefined;
    projectInstance: ProjectInstanceModel | undefined;
    register: UseFormRegister<ProjectInstanceModel>;
    setProjectId: Dispatch<SetStateAction<number | undefined>>;
    setValue: UseFormSetValue<ProjectInstanceModel>;
    touchedFields: UseFormReturn<ProjectInstanceModel>['formState']['touchedFields'];
}

const ProjectLabel = ({project}: {project: ProjectModel}) => (
    <div className="flex items-center">
        <span className="mr-1 ">{project.name}</span>

        <span className="text-xs text-gray-500">{project?.tags?.map((tag) => tag.name).join(', ')}</span>
    </div>
);

const ProjectsComboBox = ({
    onBlur,
    onChange,
    value,
}: {
    onBlur: FocusEventHandler;
    onChange: (item?: ComboBoxItemType) => void;
    value?: number;
}) => {
    const {data: projects} = useGetProjectsQuery({status: ProjectStatusModel.Published});

    return projects ? (
        <ComboBox
            items={projects.map(
                (project) =>
                    ({
                        label: <ProjectLabel project={project} />,
                        value: project.id,
                    }) as ComboBoxItemType
            )}
            name="projectId"
            onBlur={onBlur}
            onChange={onChange}
            value={value}
        />
    ) : (
        <>Loading...</>
    );
};

const ProjectVersionsSelect = ({
    disabled,
    onChange,
    projectId,
    projectVersion,
}: {
    disabled: boolean;
    onChange: (value: number) => void;
    projectId?: number;
    projectVersion?: number;
}) => {
    const {data: projectVersions} = useGetProjectVersionsQuery(projectId!, !!projectId);

    return (
        <Select
            defaultValue={projectVersion?.toString()}
            disabled={disabled}
            onValueChange={(value) => {
                onChange(+value);
            }}
        >
            <SelectTrigger className="w-full">
                <SelectValue placeholder="Select version" />
            </SelectTrigger>

            <SelectContent>
                {projectVersions &&
                    projectVersions.map(
                        (projectVersion) =>
                            projectVersion.status == ProjectStatusModel.Published && (
                                <SelectItem key={projectVersion.version} value={projectVersion.version!.toString()}>
                                    V{projectVersion.version}
                                </SelectItem>
                            )
                    )}
            </SelectContent>
        </Select>
    );
};

const TagsSelect = ({
    field,
    onCreateOption,
    projectInstance,
}: {
    field: ControllerRenderProps<ProjectInstanceModel, 'tags'>;
    onCreateOption: (inputValue: string) => void;
    projectInstance?: ProjectInstanceModel;
}) => {
    const {data: tags} = useGetProjectInstanceTagsQuery();

    const tagNames = projectInstance?.tags?.map((tag) => tag.name);

    const remainingTags = tags?.filter((tag) => !tagNames?.includes(tag.name));

    return remainingTags ? (
        <CreatableSelect
            field={field}
            isMulti
            onCreateOption={onCreateOption}
            options={remainingTags.map((tag) => {
                return {
                    label: tag.name,
                    value: tag.name.toLowerCase().replace(/\W/g, ''),
                    ...tag,
                };
            })}
        />
    ) : (
        <>Loading....</>
    );
};

const ProjectInstanceDialogBasicStep = ({
    control,
    getValues,
    projectId,
    projectInstance,
    setProjectId,
    setValue,
}: ProjectDialogBasicStepProps) => {
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
                                <ProjectsComboBox
                                    onBlur={field.onBlur}
                                    onChange={(item) => {
                                        if (item) {
                                            resetWorkflowsEnabledStore();
                                            setValue('projectId', item.value);
                                            setValue('projectVersion', undefined);
                                            setProjectId(item.value);
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

            {!projectInstance?.id && (
                <FormField
                    control={control}
                    name="projectVersion"
                    render={({field}) => (
                        <FormItem>
                            <FormLabel>Version</FormLabel>

                            <FormControl>
                                <ProjectVersionsSelect
                                    disabled={!!projectInstance?.projectVersion}
                                    onChange={(value) => field.onChange(value)}
                                    projectId={projectId}
                                    projectVersion={field.value}
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
                                        <SelectItem value="DEVELOPMENT">Development</SelectItem>

                                        <SelectItem value="STAGING">Staging</SelectItem>

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

            <FormField
                control={control}
                name="tags"
                render={({field}) => (
                    <FormItem>
                        <FormLabel>Tags</FormLabel>

                        <TagsSelect
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
        </div>
    );
};

export default ProjectInstanceDialogBasicStep;
