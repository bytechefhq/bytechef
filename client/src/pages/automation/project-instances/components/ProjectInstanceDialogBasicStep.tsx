import ComboBox, {ComboBoxItemType} from '@/components/ComboBox';
import {
    FormControl,
    FormField,
    FormItem,
    FormLabel,
    FormMessage,
} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {Textarea} from '@/components/ui/textarea';
import {useGetProjectInstanceTagsQuery} from '@/queries/projectInstanceTags.queries';
import {useGetProjectsQuery} from '@/queries/projects.queries';
import CreatableSelect from 'components/CreatableSelect/CreatableSelect';
import {
    ProjectInstanceModel,
    ProjectModel,
} from 'middleware/helios/configuration';
import {
    Control,
    UseFormGetValues,
    UseFormRegister,
    UseFormReturn,
    UseFormSetValue,
    UseFormTrigger,
} from 'react-hook-form';

interface ProjectDialogBasicStepProps {
    control: Control<ProjectInstanceModel>;
    errors: UseFormReturn<ProjectInstanceModel>['formState']['errors'];
    getValues: UseFormGetValues<ProjectInstanceModel>;
    projectInstance: ProjectInstanceModel | undefined;
    register: UseFormRegister<ProjectInstanceModel>;
    setValue: UseFormSetValue<ProjectInstanceModel>;
    touchedFields: UseFormReturn<ProjectInstanceModel>['formState']['touchedFields'];
    trigger: UseFormTrigger<ProjectInstanceModel>;
}

const ProjectLabel = ({project}: {project: ProjectModel}) => (
    <div className="flex items-center">
        <span className="mr-1 ">{project.name}</span>

        <span className="text-xs text-gray-500">
            {project?.tags?.map((tag) => tag.name).join(', ')}
        </span>
    </div>
);

const ProjectInstanceDialogBasicStep = ({
    control,
    getValues,
    projectInstance,
    setValue,
    trigger,
}: ProjectDialogBasicStepProps) => {
    const {
        data: projects,
        error: projectsError,
        isLoading: projectsLoading,
    } = useGetProjectsQuery({published: true});

    const {
        data: tags,
        error: tagsError,
        isLoading: tagsLoading,
    } = useGetProjectInstanceTagsQuery();

    const tagNames = projectInstance?.tags?.map((tag) => tag.name);

    const remainingTags = tags?.filter((tag) => !tagNames?.includes(tag.name));

    return (
        <>
            {projectsError && !projectsLoading && (
                <span>An error has occurred: {projectsError.message}</span>
            )}

            {tagsError && !tagsLoading && (
                <span>An error has occurred: {tagsError.message}</span>
            )}

            {projects ? (
                <>
                    {!projectInstance?.id && (
                        <FormField
                            control={control}
                            name="projectId"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Project</FormLabel>

                                    <FormControl>
                                        <ComboBox
                                            items={projects.map(
                                                (project) =>
                                                    ({
                                                        label: (
                                                            <ProjectLabel
                                                                project={
                                                                    project
                                                                }
                                                            />
                                                        ),
                                                        value: project.id,
                                                    }) as ComboBoxItemType
                                            )}
                                            name="projectId"
                                            onBlur={field.onBlur}
                                            onChange={(item) => {
                                                if (item) {
                                                    setValue(
                                                        'projectId',
                                                        item.value
                                                    );
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

                                            trigger();
                                        }}
                                        placeholder="My CRM Project - Production"
                                        value={field.value}
                                    />
                                </FormControl>

                                <FormMessage />
                            </FormItem>
                        )}
                        rules={{required: true}}
                    />

                    <FormField
                        control={control}
                        name="description"
                        render={({field}) => (
                            <FormItem>
                                <FormLabel>Description</FormLabel>

                                <FormControl>
                                    <Textarea
                                        placeholder="Cute description of your project instance"
                                        {...field}
                                    />
                                </FormControl>

                                <FormMessage />
                            </FormItem>
                        )}
                    />

                    {remainingTags && (
                        <FormField
                            control={control}
                            name="tags"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Tags</FormLabel>

                                    <CreatableSelect
                                        field={field}
                                        isMulti
                                        onCreateOption={(
                                            inputValue: string
                                        ) => {
                                            setValue('tags', [
                                                ...getValues().tags!,
                                                {
                                                    label: inputValue,
                                                    name: inputValue,
                                                    value: inputValue,
                                                },
                                            ] as never[]);
                                        }}
                                        options={remainingTags.map((tag) => {
                                            return {
                                                label: tag.name,
                                                value: tag.name
                                                    .toLowerCase()
                                                    .replace(/\W/g, ''),
                                                ...tag,
                                            };
                                        })}
                                    />

                                    <FormMessage />
                                </FormItem>
                            )}
                        />
                    )}
                </>
            ) : (
                <span className="px-2">Loading...</span>
            )}
        </>
    );
};

export default ProjectInstanceDialogBasicStep;
