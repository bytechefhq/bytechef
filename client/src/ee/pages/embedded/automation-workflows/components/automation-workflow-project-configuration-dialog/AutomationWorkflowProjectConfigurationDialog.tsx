import Badge from '@/components/Badge/Badge';
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
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useGetComponentDefinitionsQuery} from '@/ee/shared/queries/embedded/componentDefinitions.queries';
import {
    AutomationWorkflowProjectConfigurationsQuery,
    AutomationWorkflowProjectsQuery,
    useAutomationWorkflowProjectConfigurationVersionImpactQuery,
    useAutomationWorkflowProjectConfigurationsQuery,
    useAutomationWorkflowProjectVersionsQuery,
    useCreateAutomationWorkflowProjectConfigurationMutation,
    useUpdateAutomationWorkflowProjectConfigurationMutation,
} from '@/shared/middleware/graphql';
import {useGetEnvironmentsQuery} from '@/shared/queries/platform/environments.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {InfoIcon} from 'lucide-react';
import {useMemo, useState} from 'react';

type AutomationWorkflowProjectConfigurationType =
    AutomationWorkflowProjectConfigurationsQuery['automationWorkflowProjectConfigurations'][number];
type AutomationWorkflowProjectType = AutomationWorkflowProjectsQuery['automationWorkflowProjects'][number];

interface AutomationWorkflowProjectConfigurationDialogProps {
    configuration?: AutomationWorkflowProjectConfigurationType;
    onClose: () => void;
    project: AutomationWorkflowProjectType;
    updateProjectVersion?: boolean;
}

const AutomationWorkflowProjectConfigurationDialog = ({
    configuration,
    onClose,
    project,
    updateProjectVersion = false,
}: AutomationWorkflowProjectConfigurationDialogProps) => {
    const [automationHubVisible, setAutomationHubVisible] = useState(configuration?.automationHubVisible ?? true);
    const [projectVersion, setProjectVersion] = useState(
        (updateProjectVersion ? project.lastPublishedVersion : configuration?.projectVersion)?.toString() ??
            project.lastPublishedVersion?.toString() ??
            ''
    );

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const queryClient = useQueryClient();

    const {data: environments} = useGetEnvironmentsQuery();
    const {data: configurationsData} = useAutomationWorkflowProjectConfigurationsQuery({projectId: project.id});
    const {data: versionsData} = useAutomationWorkflowProjectVersionsQuery({id: project.id});

    const changesVersion =
        !!configuration && projectVersion !== '' && Number(projectVersion) !== configuration.projectVersion;

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({connectionDefinitions: true}, changesVersion);
    const {data: impactData} = useAutomationWorkflowProjectConfigurationVersionImpactQuery(
        {id: configuration?.id ?? '', projectVersion: Number(projectVersion)},
        {enabled: changesVersion}
    );

    const onSuccess = () => {
        void queryClient.invalidateQueries({queryKey: ['automationWorkflowProjectConfigurations']});
        void queryClient.invalidateQueries({queryKey: ['automationWorkflowProjects']});

        onClose();
    };

    const createConfigurationMutation = useCreateAutomationWorkflowProjectConfigurationMutation();
    const updateConfigurationMutation = useUpdateAutomationWorkflowProjectConfigurationMutation();

    const environmentId = configuration?.environmentId ?? String(currentEnvironmentId);

    const environmentTaken =
        !configuration &&
        (configurationsData?.automationWorkflowProjectConfigurations ?? []).some(
            (existingConfiguration) => existingConfiguration.environmentId === environmentId
        );

    const publishedVersions = useMemo(
        () =>
            (versionsData?.automationWorkflowProjectVersions ?? []).filter(
                (projectVersionItem) => projectVersionItem.status === 'PUBLISHED'
            ),
        [versionsData]
    );

    const environmentName =
        (environments ?? []).find((environment) => String(environment.id) === environmentId)?.name || environmentId;

    const isPending = createConfigurationMutation.isPending || updateConfigurationMutation.isPending;

    const movesToOlderVersion =
        !!configuration && projectVersion !== '' && Number(projectVersion) < configuration.projectVersion;

    const impactWarnings = useMemo(() => {
        if (!changesVersion) {
            return [];
        }

        const componentTitles = new Map(
            (componentDefinitions ?? []).map((componentDefinition) => [
                componentDefinition.name,
                componentDefinition.title || componentDefinition.name,
            ])
        );

        const warnings: {key: string; text: string}[] = [];

        for (const impactWorkflow of impactData?.automationWorkflowProjectConfigurationVersionImpact.workflows ?? []) {
            for (const inputName of impactWorkflow.newRequiredInputs) {
                warnings.push({
                    key: `${impactWorkflow.workflowUuid}-input-${inputName}`,
                    text: `Version ${projectVersion} adds required input ${inputName} to ${impactWorkflow.label}: ${impactWorkflow.affectedConnectedUserCount} connected users will pause until they fill it.`,
                });
            }

            for (const componentName of impactWorkflow.newRequiredConnections) {
                const componentTitle = componentTitles.get(componentName) ?? componentName;

                warnings.push({
                    key: `${impactWorkflow.workflowUuid}-connection-${componentName}`,
                    text: `Version ${projectVersion} adds a required ${componentTitle} connection to ${impactWorkflow.label}: connected users without a ${componentTitle} integration will pause.`,
                });
            }
        }

        return warnings;
    }, [changesVersion, componentDefinitions, impactData, projectVersion]);

    const handleSubmit = () => {
        if (configuration) {
            updateConfigurationMutation.mutate(
                {
                    automationHubVisible,
                    enabled: configuration.enabled,
                    id: configuration.id,
                    projectVersion: Number(projectVersion),
                },
                {onSuccess}
            );
        } else {
            createConfigurationMutation.mutate(
                {
                    automationHubVisible,
                    enabled: false,
                    environmentId,
                    projectId: project.id,
                    projectVersion: Number(projectVersion),
                },
                {onSuccess}
            );
        }
    };

    const environmentBadgeFieldset = (
        <fieldset className="flex flex-col gap-2 border-0 p-0">
            <span className="text-sm font-medium">Environment</span>

            <div>
                <Badge label={environmentName} styleType="outline-outline" weight="semibold" />
            </div>

            {environmentTaken && (
                <p className="text-sm text-content-warning-primary">
                    This project already has a configuration in {environmentName}.
                </p>
            )}
        </fieldset>
    );

    const versionFieldset = (
        <fieldset className="flex flex-col gap-2 border-0 p-0">
            <span className="text-sm font-medium">Version</span>

            <Select onValueChange={setProjectVersion} value={projectVersion}>
                <SelectTrigger aria-label="Version">
                    <SelectValue placeholder="Select version" />
                </SelectTrigger>

                <SelectContent>
                    {publishedVersions.map((publishedVersion) => (
                        <SelectItem key={publishedVersion.version} value={String(publishedVersion.version)}>
                            {`V${publishedVersion.version}`}
                        </SelectItem>
                    ))}
                </SelectContent>
            </Select>

            {movesToOlderVersion && (
                <p className="text-sm text-content-warning-primary">
                    Versions that lack workflows connected users still use cannot be saved.
                </p>
            )}

            {impactWarnings.map((impactWarning) => (
                <p className="text-sm text-content-warning-primary" key={impactWarning.key}>
                    {impactWarning.text}
                </p>
            ))}
        </fieldset>
    );

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
                            title={
                                updateProjectVersion
                                    ? 'Upgrade Project Version'
                                    : `${configuration ? 'Edit' : 'Create'} Configuration`
                            }
                        />

                        <DialogBody>
                            {(!configuration || updateProjectVersion) && versionFieldset}

                            {environmentBadgeFieldset}

                            {!updateProjectVersion && (
                                <div className="flex items-center gap-1.5">
                                    <Switch
                                        checked={automationHubVisible}
                                        label="Show in Automation Hub"
                                        onCheckedChange={setAutomationHubVisible}
                                    />

                                    <Tooltip>
                                        <TooltipTrigger asChild>
                                            <InfoIcon
                                                aria-label="About Show in Automation Hub"
                                                className="size-4 text-content-neutral-secondary"
                                            />
                                        </TooltipTrigger>

                                        <TooltipContent>
                                            Turn off for flows you only activate through the API.
                                        </TooltipContent>
                                    </Tooltip>
                                </div>
                            )}
                        </DialogBody>

                        <DialogFooter>
                            <DialogCancelButton />

                            <Button
                                disabled={isPending || environmentTaken || !projectVersion}
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
