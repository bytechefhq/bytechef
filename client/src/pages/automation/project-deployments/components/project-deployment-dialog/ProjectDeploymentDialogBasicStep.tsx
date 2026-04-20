import {Note} from '@/components/Note';
import {FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Skeleton} from '@/components/ui/skeleton';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {Textarea} from '@/components/ui/textarea';
import ProjectDeploymentDialogBasicStepProjectVersionsSelect from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogBasicStepProjectVersionsSelect';
import ProjectDeploymentDialogBasicStepProjectsComboBox from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogBasicStepProjectsComboBox';
import ProjectDeploymentDialogBasicStepTagsSelect from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogBasicStepTagsSelect';
import {useWorkflowsEnabledStore} from '@/pages/automation/project-deployments/stores/useWorkflowsEnabledStore';
import EnvironmentBadge from '@/shared/components/EnvironmentBadge';
import {ProjectDeployment} from '@/shared/middleware/automation/configuration';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {InfoIcon, LayersIcon, PlusIcon, RefreshCwIcon} from 'lucide-react';
import {useState} from 'react';
import {Control, UseFormGetValues, UseFormSetValue} from 'react-hook-form';
import {useShallow} from 'zustand/react/shallow';

interface ProjectDialogBasicStepProps {
    changeProjectVersion: boolean;
    control: Control<ProjectDeployment>;
    getValues: UseFormGetValues<ProjectDeployment>;
    projectDeployment: ProjectDeployment | undefined;
    projectDeployments?: ProjectDeployment[];
    projectDeploymentsLoading?: boolean;
    setValue: UseFormSetValue<ProjectDeployment>;
}

const ProjectDeploymentDialogBasicStep = ({
    changeProjectVersion,
    control,
    getValues,
    projectDeployment,
    projectDeployments,
    projectDeploymentsLoading,
    setValue,
}: ProjectDialogBasicStepProps) => {
    const [selectedDeploymentId, setSelectedDeploymentId] = useState<string | undefined>();
    const [currentProjectId, setCurrentProjectId] = useState(getValues('projectId'));
    const [currentProjectVersion, setCurrentProjectVersion] = useState<number | undefined>(getValues('projectVersion'));

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const [resetWorkflowsEnabledStore] = useWorkflowsEnabledStore(useShallow(({reset}) => [reset]));

    const handleDeploymentSelectChange = (value: string) => {
        setSelectedDeploymentId(value);

        const selectedDeployment = projectDeployments?.find((deployment) => deployment.id?.toString() === value);

        if (selectedDeployment?.projectVersion != null) {
            setValue('projectVersion', selectedDeployment.projectVersion);
            setCurrentProjectVersion(selectedDeployment.projectVersion);
        }
    };

    const handleProjectVersionChange = (value: number, onChange?: (value: number) => void) => {
        if (onChange) {
            onChange(value);
        }

        setValue('projectVersion', value);
        setValue('projectDeploymentWorkflows', []);

        setCurrentProjectVersion(value);
    };

    const handleProjectSelectionChange = (item?: {value: number; name?: string}) => {
        if (!item) {
            return;
        }

        resetWorkflowsEnabledStore();

        setValue('projectId', item.value);
        setValue('projectVersion', undefined);

        if (!getValues('name')) {
            setValue('name', item.name!.toString());
        }

        setCurrentProjectId(item.value);
        setCurrentProjectVersion(undefined);
    };

    const hasDeployments = (projectDeployments?.length ?? 0) > 0;

    return (
        <Tabs defaultValue="new-deployment">
            <TabsList>
                <TabsTrigger className="gap-2 px-3 py-1" value="new-deployment">
                    <PlusIcon className="size-4" />

                    <span>New</span>
                </TabsTrigger>

                <TabsTrigger className="gap-2 px-3 py-1" value="change-version">
                    <RefreshCwIcon className="size-4" />

                    <span>Change Version</span>
                </TabsTrigger>
            </TabsList>

            <TabsContent className="m-0 flex flex-col gap-4" value="new-deployment">
                <Note
                    className="mt-4"
                    content="Existing deployments already exists. This will start an additional deployment instance."
                    icon={<InfoIcon />}
                />

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
                                        onChange={handleProjectSelectionChange}
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

                {!changeProjectVersion && (
                    <FormField
                        control={control}
                        name="name"
                        render={({field}) => (
                            <FormItem>
                                <FormLabel>Name</FormLabel>

                                <FormControl>
                                    <Input onChange={field.onChange} placeholder="My CRM Project" value={field.value} />
                                </FormControl>

                                <FormMessage />
                            </FormItem>
                        )}
                        rules={{required: true}}
                    />
                )}

                {currentProjectId && (!projectDeployment?.id || changeProjectVersion) && (
                    <FormField
                        control={control}
                        name="projectVersion"
                        render={({field}) => (
                            <FormItem>
                                <FormLabel>Version</FormLabel>

                                <FormControl>
                                    <ProjectDeploymentDialogBasicStepProjectVersionsSelect
                                        onChange={(value) => handleProjectVersionChange(value, field.onChange)}
                                        projectId={currentProjectId}
                                        projectVersion={currentProjectVersion}
                                    />
                                </FormControl>

                                <FormMessage />
                            </FormItem>
                        )}
                        rules={{required: true}}
                        shouldUnregister={false}
                    />
                )}

                {!changeProjectVersion && (
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

                {!changeProjectVersion && (
                    <FormField
                        control={control}
                        name="tags"
                        render={({field}) => (
                            <FormItem>
                                <FormLabel>Tags</FormLabel>

                                <ProjectDeploymentDialogBasicStepTagsSelect
                                    field={field}
                                    onCreateOption={(inputValue: string) =>
                                        setValue('tags', [
                                            ...(getValues().tags ?? []),
                                            {
                                                label: inputValue,
                                                name: inputValue,
                                                value: inputValue,
                                            },
                                        ] as never[])
                                    }
                                    projectDeployment={projectDeployment}
                                />

                                <FormMessage />
                            </FormItem>
                        )}
                    />
                )}
            </TabsContent>

            <TabsContent className="m-0 flex flex-col gap-4" value="change-version">
                {hasDeployments && currentProjectId && (!projectDeployment?.id || changeProjectVersion) && (
                    <>
                        <FormItem className="pt-4">
                            <FormLabel>Deployment</FormLabel>

                            <FormControl>
                                <div>
                                    {projectDeploymentsLoading && <Skeleton className="h-9 w-full" />}

                                    {!projectDeploymentsLoading &&
                                        projectDeployments &&
                                        projectDeployments.length > 0 && (
                                            <Select
                                                onValueChange={(value) => handleDeploymentSelectChange(value)}
                                                value={selectedDeploymentId}
                                            >
                                                <SelectTrigger className="w-full">
                                                    <SelectValue placeholder="Select deployment" />
                                                </SelectTrigger>

                                                <SelectContent className="w-full">
                                                    {projectDeployments.map((deployment) => (
                                                        <SelectItem
                                                            key={
                                                                deployment.id?.toString() ??
                                                                `${deployment.name ?? 'deployment'}-${deployment.projectVersion}`
                                                            }
                                                            value={deployment.id?.toString() ?? ''}
                                                        >
                                                            {`${deployment.name ?? `Deployment ${deployment.id}`} - V${deployment.projectVersion}`}
                                                        </SelectItem>
                                                    ))}
                                                </SelectContent>
                                            </Select>
                                        )}
                                </div>
                            </FormControl>
                        </FormItem>

                        <FormField
                            control={control}
                            name="projectVersion"
                            render={() => (
                                <FormItem>
                                    <FormLabel>Version</FormLabel>

                                    <FormControl>
                                        <ProjectDeploymentDialogBasicStepProjectVersionsSelect
                                            onChange={handleProjectVersionChange}
                                            projectId={currentProjectId}
                                            projectVersion={currentProjectVersion}
                                        />
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                            rules={{required: true}}
                            shouldUnregister={false}
                        />
                    </>
                )}

                {!hasDeployments && (
                    <div className="flex w-full flex-col items-center justify-center gap-4 pt-4">
                        <div className="flex size-10 items-center justify-center rounded-lg bg-surface-neutral-secondary">
                            <LayersIcon />
                        </div>

                        <h2 className="text-xl">No deployments for this project yet</h2>
                    </div>
                )}
            </TabsContent>
        </Tabs>
    );
};

export default ProjectDeploymentDialogBasicStep;
