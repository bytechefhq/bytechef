import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogBody,
    DialogCancelButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogMain,
} from '@/components/Dialog';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import Switch from '@/components/Switch/Switch';
import {
    AutomationWorkflowProjectConfigurationsQuery,
    AutomationWorkflowProjectsQuery,
    useAutomationWorkflowProjectConfigurationsQuery,
    useAutomationWorkflowProjectVersionsQuery,
    useCreateAutomationWorkflowProjectConfigurationMutation,
    useUpdateAutomationWorkflowProjectConfigurationMutation,
} from '@/shared/middleware/graphql';
import {useGetEnvironmentsQuery} from '@/shared/queries/platform/environments.queries';
import {useQueryClient} from '@tanstack/react-query';
import {useMemo, useState} from 'react';

type AutomationWorkflowProjectConfigurationType =
    AutomationWorkflowProjectConfigurationsQuery['automationWorkflowProjectConfigurations'][number];
type AutomationWorkflowProjectType = AutomationWorkflowProjectsQuery['automationWorkflowProjects'][number];

interface AutomationWorkflowProjectConfigurationDialogProps {
    configuration?: AutomationWorkflowProjectConfigurationType;
    onClose: () => void;
    project: AutomationWorkflowProjectType;
}

const AutomationWorkflowProjectConfigurationDialog = ({
    configuration,
    onClose,
    project,
}: AutomationWorkflowProjectConfigurationDialogProps) => {
    const [automationHubVisible, setAutomationHubVisible] = useState(configuration?.automationHubVisible ?? true);
    const [enabled, setEnabled] = useState(configuration?.enabled ?? false);
    const [environmentId, setEnvironmentId] = useState(configuration?.environmentId ?? '');
    const [projectVersion, setProjectVersion] = useState(
        configuration?.projectVersion?.toString() ?? project.lastPublishedVersion?.toString() ?? ''
    );

    const queryClient = useQueryClient();

    const {data: environments} = useGetEnvironmentsQuery();
    const {data: configurationsData} = useAutomationWorkflowProjectConfigurationsQuery({projectId: project.id});
    const {data: versionsData} = useAutomationWorkflowProjectVersionsQuery({id: project.id});

    const onSuccess = () => {
        void queryClient.invalidateQueries({queryKey: ['automationWorkflowProjectConfigurations']});
        void queryClient.invalidateQueries({queryKey: ['automationWorkflowProjects']});

        onClose();
    };

    const createConfigurationMutation = useCreateAutomationWorkflowProjectConfigurationMutation();
    const updateConfigurationMutation = useUpdateAutomationWorkflowProjectConfigurationMutation();

    const availableEnvironments = useMemo(() => {
        const takenEnvironmentIds = new Set(
            (configurationsData?.automationWorkflowProjectConfigurations ?? []).map(
                (existingConfiguration) => existingConfiguration.environmentId
            )
        );

        return (environments ?? []).filter(
            (environment) =>
                environment.id != null &&
                (!takenEnvironmentIds.has(String(environment.id)) ||
                    String(environment.id) === configuration?.environmentId)
        );
    }, [configuration?.environmentId, configurationsData, environments]);

    const publishedVersions = useMemo(
        () =>
            (versionsData?.automationWorkflowProjectVersions ?? []).filter(
                (projectVersionItem) => projectVersionItem.status === 'PUBLISHED'
            ),
        [versionsData]
    );

    const isPending = createConfigurationMutation.isPending || updateConfigurationMutation.isPending;

    const movesToOlderVersion =
        !!configuration && projectVersion !== '' && Number(projectVersion) < configuration.projectVersion;

    const handleSubmit = () => {
        if (configuration) {
            updateConfigurationMutation.mutate(
                {
                    automationHubVisible,
                    enabled,
                    id: configuration.id,
                    projectVersion: Number(projectVersion),
                },
                {onSuccess}
            );
        } else {
            createConfigurationMutation.mutate(
                {
                    automationHubVisible,
                    enabled,
                    environmentId,
                    projectId: project.id,
                    projectVersion: Number(projectVersion),
                },
                {onSuccess}
            );
        }
    };

    return (
        <Dialog
            onOpenChange={(open) => {
                if (!open) {
                    onClose();
                }
            }}
            open
        >
            <DialogContent aria-label="Configuration Dialog" onInteractOutside={(event) => event.preventDefault()}>
                <DialogMain>
                    <form
                        className="flex min-h-0 flex-1 flex-col"
                        onSubmit={(event) => {
                            event.preventDefault();

                            handleSubmit();
                        }}
                    >
                        <DialogHeader
                            description="Choose the environment and the published version of the project to run there."
                            title={`${configuration ? 'Edit' : 'Create'} Configuration`}
                        />

                        <DialogBody>
                            <fieldset className="flex flex-col gap-2 border-0 p-0">
                                <span className="text-sm font-medium">Environment</span>

                                <Select
                                    disabled={!!configuration}
                                    onValueChange={setEnvironmentId}
                                    value={environmentId}
                                >
                                    <SelectTrigger aria-label="Environment">
                                        <SelectValue placeholder="Select environment" />
                                    </SelectTrigger>

                                    <SelectContent>
                                        {availableEnvironments.map((environment) => (
                                            <SelectItem key={environment.id} value={String(environment.id)}>
                                                {environment.name}
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>
                            </fieldset>

                            <fieldset className="flex flex-col gap-2 border-0 p-0">
                                <span className="text-sm font-medium">Version</span>

                                <Select onValueChange={setProjectVersion} value={projectVersion}>
                                    <SelectTrigger aria-label="Version">
                                        <SelectValue placeholder="Select version" />
                                    </SelectTrigger>

                                    <SelectContent>
                                        {publishedVersions.map((publishedVersion) => (
                                            <SelectItem
                                                key={publishedVersion.version}
                                                value={String(publishedVersion.version)}
                                            >
                                                {`V${publishedVersion.version}`}
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>

                                {movesToOlderVersion && (
                                    <p className="text-sm text-content-warning-primary">
                                        Connected users whose automations are not in this version will be disconnected
                                        from them.
                                    </p>
                                )}
                            </fieldset>

                            <Switch checked={enabled} label="Enabled" onCheckedChange={setEnabled} />

                            <Switch
                                checked={automationHubVisible}
                                description="Turn off for flows you only activate through the API."
                                label="Show in Automation Hub"
                                onCheckedChange={setAutomationHubVisible}
                            />
                        </DialogBody>

                        <DialogFooter>
                            <DialogCancelButton />

                            <Button
                                disabled={isPending || !environmentId || !projectVersion}
                                label="Save"
                                type="submit"
                            />
                        </DialogFooter>
                    </form>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default AutomationWorkflowProjectConfigurationDialog;
