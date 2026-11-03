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
import RequiredMark from '@/components/RequiredMark';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import Switch from '@/components/Switch/Switch';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useGetComponentDefinitionsQuery} from '@/ee/shared/queries/embedded/componentDefinitions.queries';
import {ENVIRONMENT_CONFIGS} from '@/shared/constants/environmentConfigs';
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
    project?: AutomationWorkflowProjectType;
    projects?: AutomationWorkflowProjectType[];
    updateProjectVersion?: boolean;
}

const AutomationWorkflowProjectConfigurationDialog = ({
    configuration,
    onClose,
    project: fixedProject,
    projects = [],
    updateProjectVersion = false,
}: AutomationWorkflowProjectConfigurationDialogProps) => {
    const [automationHubVisible, setAutomationHubVisible] = useState(configuration?.automationHubVisible ?? true);
    const [enteredName, setEnteredName] = useState<string | undefined>(configuration?.name);
    const [permissionExpression, setPermissionExpression] = useState(configuration?.permissionExpression ?? '');
    const [chosenProjectVersion, setChosenProjectVersion] = useState(
        (updateProjectVersion ? fixedProject?.lastPublishedVersion : configuration?.projectVersion)?.toString() ?? ''
    );
    const [selectedProjectId, setSelectedProjectId] = useState<string | undefined>(fixedProject?.id);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const queryClient = useQueryClient();

    const {data: environments} = useGetEnvironmentsQuery();
    const project = fixedProject ?? projects.find((projectItem) => projectItem.id === selectedProjectId);

    const projectVersion = chosenProjectVersion || project?.lastPublishedVersion?.toString() || '';

    const {data: configurationsData} = useAutomationWorkflowProjectConfigurationsQuery(
        {projectId: project?.id ?? ''},
        {enabled: !!project}
    );
    const {data: versionsData} = useAutomationWorkflowProjectVersionsQuery(
        {id: project?.id ?? ''},
        {enabled: !!project}
    );

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

    const publishedVersions = useMemo(
        () =>
            (versionsData?.automationWorkflowProjectVersions ?? []).filter(
                (projectVersionItem) => projectVersionItem.status === 'PUBLISHED'
            ),
        [versionsData]
    );

    const environmentName =
        (environments ?? []).find((environment) => String(environment.id) === environmentId)?.name || environmentId;

    const environmentHasConfiguration = (configurationsData?.automationWorkflowProjectConfigurations ?? []).some(
        (existingConfiguration) => existingConfiguration.environmentId === environmentId
    );

    const name =
        enteredName ?? (environmentHasConfiguration ? '' : ENVIRONMENT_CONFIGS[currentEnvironmentId]?.label || '');

    const trimmedName = name.trim();

    const nameTaken =
        trimmedName !== '' &&
        (configurationsData?.automationWorkflowProjectConfigurations ?? []).some(
            (existingConfiguration) =>
                existingConfiguration.environmentId === environmentId &&
                existingConfiguration.id !== configuration?.id &&
                existingConfiguration.name.trim().toLowerCase() === trimmedName.toLowerCase()
        );

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

    const handleSelectProject = (projectId: string) => {
        setSelectedProjectId(projectId);
        setChosenProjectVersion('');
    };

    const handleSubmit = () => {
        if (!project) {
            return;
        }

        if (configuration) {
            updateConfigurationMutation.mutate(
                {
                    automationHubVisible,
                    enabled: configuration.enabled,
                    id: configuration.id,
                    name: trimmedName,
                    permissionExpression: permissionExpression.trim() || null,
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
                    name: trimmedName,
                    permissionExpression: permissionExpression.trim() || null,
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
        </fieldset>
    );

    const projectFieldset = (
        <fieldset className="flex flex-col gap-2 border-0 p-0">
            <span className="text-sm font-medium">
                Project
                <RequiredMark />
            </span>

            <Select onValueChange={handleSelectProject} value={selectedProjectId}>
                <SelectTrigger aria-label="Project">
                    <SelectValue placeholder="Select project" />
                </SelectTrigger>

                <SelectContent>
                    {projects.map((projectItem) => (
                        <SelectItem key={projectItem.id} value={projectItem.id}>
                            {projectItem.name}
                        </SelectItem>
                    ))}
                </SelectContent>
            </Select>
        </fieldset>
    );

    const nameFieldset = (
        <fieldset className="flex flex-col gap-2 border-0 p-0">
            <Label className="gap-0" htmlFor="configurationName">
                Name
                <RequiredMark />
            </Label>

            <Input
                aria-invalid={nameTaken}
                id="configurationName"
                onChange={(event) => setEnteredName(event.target.value)}
                value={name}
            />

            {nameTaken && (
                <p className="text-sm text-content-destructive-primary">
                    {`A configuration named ${trimmedName} already exists in ${environmentName}.`}
                </p>
            )}
        </fieldset>
    );

    const permissionExpressionFieldset = (
        <fieldset className="flex flex-col gap-2 border-0 p-0">
            <Label htmlFor="configurationPermissionExpression">Assignment rule</Label>

            <Textarea
                id="configurationPermissionExpression"
                onChange={(event) => setPermissionExpression(event.target.value)}
                placeholder="e.g. metadata['beta'] == 'true'"
                rows={3}
                value={permissionExpression}
            />

            <p className="text-sm text-content-neutral-secondary">
                Which new users self-serve places on this configuration. Checked once, when a user is first assigned.
                Users already on it stay until you move them. Empty means everyone.
            </p>
        </fieldset>
    );

    const versionFieldset = (
        <fieldset className="flex flex-col gap-2 border-0 p-0">
            <span className="text-sm font-medium">Version</span>

            <Select disabled={!project} onValueChange={setChosenProjectVersion} value={projectVersion}>
                <SelectTrigger aria-label="Version">
                    <SelectValue placeholder={project ? 'Select version' : 'Select a project first'} />
                </SelectTrigger>

                <SelectContent>
                    {publishedVersions.length > 0 ? (
                        publishedVersions.map((publishedVersion) => (
                            <SelectItem key={publishedVersion.version} value={String(publishedVersion.version)}>
                                {`V${publishedVersion.version}`}
                            </SelectItem>
                        ))
                    ) : (
                        <p className="px-2 py-1.5 text-sm text-content-neutral-secondary">
                            {versionsData
                                ? 'This project has no published versions. Publish it in the Development environment first.'
                                : 'Loading versions...'}
                        </p>
                    )}
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
                                    : `${configuration ? 'Edit' : 'New'} Configuration`
                            }
                        />

                        <DialogBody>
                            {!fixedProject && projectFieldset}

                            {!updateProjectVersion && nameFieldset}

                            {(!configuration || updateProjectVersion) && versionFieldset}

                            {environmentBadgeFieldset}

                            {!updateProjectVersion && permissionExpressionFieldset}

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
                                disabled={
                                    isPending ||
                                    nameTaken ||
                                    !project ||
                                    !projectVersion ||
                                    (!updateProjectVersion && !trimmedName)
                                }
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
