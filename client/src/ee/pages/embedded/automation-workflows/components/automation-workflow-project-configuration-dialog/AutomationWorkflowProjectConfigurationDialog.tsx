import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import ComboBox, {ComboBoxItemType} from '@/components/ComboBox/ComboBox';
import {
    Dialog,
    DialogBody,
    DialogCancelButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogMain,
    DialogNextButton,
    DialogPreviousButton,
    type DialogStepI,
    DialogWizard,
    useDialogSteps,
} from '@/components/Dialog';
import RequiredMark from '@/components/RequiredMark';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import Switch from '@/components/Switch/Switch';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useGetComponentDefinitionsQuery} from '@/ee/shared/queries/embedded/componentDefinitions.queries';
import {
    AutomationWorkflowProjectConfigurationsQuery,
    AutomationWorkflowProjectsQuery,
    useAutomationWorkflowProjectConfigurationVersionImpactQuery,
    useAutomationWorkflowProjectConfigurationsQuery,
    useAutomationWorkflowProjectVersionWorkflowChangesQuery,
    useAutomationWorkflowProjectVersionsQuery,
    useCreateAutomationWorkflowProjectConfigurationMutation,
    useEnableAutomationWorkflowProjectConfigurationWorkflowMutation,
    useUpdateAutomationWorkflowProjectConfigurationMutation,
    useUpdateAutomationWorkflowProjectConfigurationWorkflowMutation,
} from '@/shared/middleware/graphql';
import {WorkflowChangeType} from '@/shared/middleware/graphql-types';
import {useGetEnvironmentsQuery} from '@/shared/queries/platform/environments.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {InfoIcon, SlidersHorizontalIcon} from 'lucide-react';
import {ReactNode, useMemo, useState} from 'react';

import AutomationWorkflowProjectConfigurationDialogWorkflowsStep from './AutomationWorkflowProjectConfigurationDialogWorkflowsStep';
import {
    ConfigurationDialogWorkflowI,
    WorkflowDraftI,
    getWorkflowChanges,
} from './automationWorkflowProjectConfigurationDialog-utils';

type AutomationWorkflowProjectConfigurationType =
    AutomationWorkflowProjectConfigurationsQuery['automationWorkflowProjectConfigurations'][number];
type AutomationWorkflowProjectType = AutomationWorkflowProjectsQuery['automationWorkflowProjects'][number];

interface ConfigurationDialogStepContentProps {
    basicStepContent: ReactNode;
    workflowsStepContent: ReactNode;
}

const ConfigurationDialogStepContent = ({
    basicStepContent,
    workflowsStepContent,
}: ConfigurationDialogStepContentProps) => {
    const {currentStep} = useDialogSteps();

    return <>{currentStep.id === 'workflows' ? workflowsStepContent : basicStepContent}</>;
};

interface ConfigurationDialogFooterProps {
    canSave: boolean;
    isSaving: boolean;
    onSave: () => void;
}

const ConfigurationDialogFooter = ({canSave, isSaving, onSave}: ConfigurationDialogFooterProps) => {
    const {isLastStep} = useDialogSteps();

    return (
        <DialogFooter startContent={<DialogCancelButton />}>
            <DialogPreviousButton className="lg:hidden" />

            {!isLastStep && (
                <Button disabled={!canSave || isSaving} label="Save" onClick={onSave} type="button" variant="outline" />
            )}

            <DialogNextButton isPending={isSaving} label="Next" lastStepLabel="Save" />
        </DialogFooter>
    );
};

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
    const [chosenProjectVersion, setChosenProjectVersion] = useState(
        (updateProjectVersion ? fixedProject?.lastPublishedVersion : configuration?.projectVersion)?.toString() ?? ''
    );
    const [createdConfigurationId, setCreatedConfigurationId] = useState<string | undefined>();
    const [enteredName, setEnteredName] = useState<string | undefined>(configuration?.name);
    const [isSaving, setIsSaving] = useState(false);
    const [permissionExpression, setPermissionExpression] = useState(configuration?.permissionExpression ?? '');
    const [selectedProjectId, setSelectedProjectId] = useState<string | undefined>(
        fixedProject?.id ?? configuration?.projectId
    );
    const [workflowChangesFailed, setWorkflowChangesFailed] = useState(false);
    const [workflowDrafts, setWorkflowDrafts] = useState<Record<string, WorkflowDraftI>>({});

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const queryClient = useQueryClient();

    const {data: environments} = useGetEnvironmentsQuery();

    const project = fixedProject ?? projects.find((projectItem) => projectItem.id === selectedProjectId);

    const projectVersion = chosenProjectVersion || project?.lastPublishedVersion?.toString() || '';

    const isWizard = !updateProjectVersion;

    const {data: configurationsData} = useAutomationWorkflowProjectConfigurationsQuery(
        {projectId: project?.id ?? ''},
        {enabled: !!project}
    );
    const {data: versionsData} = useAutomationWorkflowProjectVersionsQuery(
        {id: project?.id ?? ''},
        {enabled: !!project}
    );
    const {data: versionWorkflowChangesData, isLoading: versionWorkflowsLoading} =
        useAutomationWorkflowProjectVersionWorkflowChangesQuery(
            {id: project?.id ?? '', projectVersion: Number(projectVersion)},
            {enabled: isWizard && !configuration && !!project && projectVersion !== ''}
        );

    const changesVersion =
        !!configuration && projectVersion !== '' && Number(projectVersion) !== configuration.projectVersion;

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({connectionDefinitions: true}, changesVersion);
    const {data: impactData} = useAutomationWorkflowProjectConfigurationVersionImpactQuery(
        {id: configuration?.id ?? '', projectVersion: Number(projectVersion)},
        {enabled: changesVersion}
    );

    const createConfigurationMutation = useCreateAutomationWorkflowProjectConfigurationMutation();
    const enableWorkflowMutation = useEnableAutomationWorkflowProjectConfigurationWorkflowMutation();
    const updateConfigurationMutation = useUpdateAutomationWorkflowProjectConfigurationMutation();
    const updateWorkflowMutation = useUpdateAutomationWorkflowProjectConfigurationWorkflowMutation();

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

    const isNameTaken = (candidateName: string) => {
        const trimmedCandidateName = candidateName.trim().toLowerCase();

        return (
            trimmedCandidateName !== '' &&
            (configurationsData?.automationWorkflowProjectConfigurations ?? []).some(
                (existingConfiguration) =>
                    existingConfiguration.environmentId === environmentId &&
                    existingConfiguration.id !== configuration?.id &&
                    existingConfiguration.id !== createdConfigurationId &&
                    existingConfiguration.name.trim().toLowerCase() === trimmedCandidateName
            )
        );
    };

    const prefilledName = !configuration && project && !isNameTaken(project.name) ? project.name : '';

    const name = enteredName ?? prefilledName;

    const trimmedName = name.trim();

    const nameTaken = isNameTaken(name);

    const basicFieldsLocked = !!createdConfigurationId;

    const canSave = !isSaving && !nameTaken && !!project && !!projectVersion && (updateProjectVersion || !!trimmedName);

    const movesToOlderVersion =
        !!configuration && projectVersion !== '' && Number(projectVersion) < configuration.projectVersion;

    const dialogWorkflows = useMemo<ConfigurationDialogWorkflowI[]>(() => {
        if (configuration) {
            return configuration.workflows.map((configurationWorkflow) => ({
                enabled: configurationWorkflow.enabled,
                label: configurationWorkflow.label || configurationWorkflow.workflowUuid,
                workflowUuid: configurationWorkflow.workflowUuid,
            }));
        }

        return (versionWorkflowChangesData?.automationWorkflowProjectVersionWorkflowChanges.workflows ?? [])
            .filter((versionWorkflow) => versionWorkflow.changeType !== WorkflowChangeType.Removed)
            .map((versionWorkflow) => ({
                enabled: true,
                label: versionWorkflow.label || versionWorkflow.workflowUuid,
                workflowUuid: versionWorkflow.workflowUuid,
            }));
    }, [configuration, versionWorkflowChangesData]);

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

    const projectItems = useMemo<ComboBoxItemType[]>(
        () =>
            (fixedProject ? [fixedProject] : projects).map((projectItem) => ({
                label: projectItem.name,
                name: projectItem.name,
                value: projectItem.id,
            })),
        [fixedProject, projects]
    );

    const steps = useMemo<DialogStepI[]>(
        () => [
            {canProceed: canSave, id: 'basic', label: 'Basic'},
            {canProceed: canSave, id: 'workflows', label: 'Workflows'},
        ],
        [canSave]
    );

    const handleSelectProject = (item?: ComboBoxItemType) => {
        if (!item) {
            return;
        }

        if (!enteredName?.trim() || enteredName === project?.name) {
            setEnteredName(undefined);
        }

        setSelectedProjectId(String(item.value));
        setChosenProjectVersion('');
        setWorkflowDrafts({});
    };

    const handleSelectProjectVersion = (version: string) => {
        setChosenProjectVersion(version);
        setWorkflowDrafts({});
    };

    const handleWorkflowDraftChange = (workflowUuid: string, workflowDraft: WorkflowDraftI) =>
        setWorkflowDrafts((previousWorkflowDrafts) => ({...previousWorkflowDrafts, [workflowUuid]: workflowDraft}));

    const saveWorkflowChanges = async (configurationId: string) => {
        const {enableChanges, providedValuesChanges} = getWorkflowChanges(dialogWorkflows, workflowDrafts);

        const results = await Promise.allSettled([
            ...enableChanges.map((enableChange) =>
                enableWorkflowMutation.mutateAsync({
                    enable: enableChange.enable,
                    id: configurationId,
                    workflowUuid: enableChange.workflowUuid,
                })
            ),
            ...providedValuesChanges.map((providedValuesChange) =>
                updateWorkflowMutation.mutateAsync({
                    id: configurationId,
                    ...providedValuesChange.providedValuesPayload,
                    workflowUuid: providedValuesChange.workflowUuid,
                })
            ),
        ]);

        if (providedValuesChanges.length > 0) {
            void queryClient.invalidateQueries({
                queryKey: ['automationWorkflowProjectConfigurationWorkflowProvidableValues'],
            });
        }

        return results.every((result) => result.status === 'fulfilled');
    };

    const saveConfiguration = async (): Promise<string | undefined> => {
        if (!project) {
            return undefined;
        }

        if (configuration) {
            await updateConfigurationMutation.mutateAsync({
                automationHubVisible,
                enabled: configuration.enabled,
                id: configuration.id,
                name: updateProjectVersion ? configuration.name : trimmedName,
                permissionExpression: permissionExpression.trim() || null,
                projectVersion: Number(projectVersion),
            });

            return configuration.id;
        }

        if (createdConfigurationId) {
            return createdConfigurationId;
        }

        const createResult = await createConfigurationMutation.mutateAsync({
            automationHubVisible,
            enabled: false,
            environmentId,
            name: trimmedName,
            permissionExpression: permissionExpression.trim() || null,
            projectId: project.id,
            projectVersion: Number(projectVersion),
        });

        setCreatedConfigurationId(createResult.createAutomationWorkflowProjectConfiguration);

        return createResult.createAutomationWorkflowProjectConfiguration;
    };

    const handleSubmit = async () => {
        if (!canSave) {
            return;
        }

        setIsSaving(true);

        try {
            const configurationId = await saveConfiguration();

            if (!configurationId) {
                return;
            }

            const workflowChangesSaved = await saveWorkflowChanges(configurationId);

            void queryClient.invalidateQueries({queryKey: ['automationWorkflowProjectConfigurations']});
            void queryClient.invalidateQueries({queryKey: ['automationWorkflowProjects']});

            setWorkflowChangesFailed(!workflowChangesSaved);

            if (workflowChangesSaved) {
                onClose();
            }
        } catch {
            setWorkflowChangesFailed(false);
        } finally {
            setIsSaving(false);
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

            <ComboBox
                aria-label="Project"
                disabled={!!fixedProject || !!configuration || basicFieldsLocked}
                emptyMessage="No published projects yet. Publish a project in the Development environment first."
                items={projectItems}
                name="projectId"
                onChange={handleSelectProject}
                value={project?.id}
            />
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
                disabled={basicFieldsLocked}
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
                disabled={basicFieldsLocked}
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

    const latestPublishedVersionPlaceholder = project?.lastPublishedVersion
        ? `Latest published version (V${project.lastPublishedVersion})`
        : 'Latest published version';

    const versionFieldset = (
        <fieldset className="flex flex-col gap-2 border-0 p-0">
            <span className="text-sm font-medium">Version</span>

            <Select
                disabled={!project || basicFieldsLocked}
                onValueChange={handleSelectProjectVersion}
                value={chosenProjectVersion}
            >
                <SelectTrigger aria-label="Version">
                    <SelectValue placeholder={project ? latestPublishedVersionPlaceholder : 'Select a project first'} />
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

    const automationHubVisibleSwitch = (
        <div className="flex items-center gap-1.5">
            <Switch
                checked={automationHubVisible}
                disabled={basicFieldsLocked}
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

                <TooltipContent>Turn off for flows you only activate through the API.</TooltipContent>
            </Tooltip>
        </div>
    );

    const workflowChangesFailedMessage = workflowChangesFailed && (
        <p className="text-sm text-content-destructive-primary" role="alert">
            {configuration
                ? 'The configuration was saved, but some workflow settings could not be saved. Save to retry them.'
                : 'The configuration was created, but some workflow settings could not be saved. Save to retry them, or Cancel to keep the configuration with the remaining workflow defaults.'}
        </p>
    );

    const basicStepContent = (
        <>
            {workflowChangesFailedMessage}

            {projectFieldset}

            {environmentBadgeFieldset}

            {nameFieldset}

            {!configuration && versionFieldset}

            {permissionExpressionFieldset}

            {automationHubVisibleSwitch}
        </>
    );

    const workflowsStepContent = (
        <>
            {workflowChangesFailedMessage}

            <AutomationWorkflowProjectConfigurationDialogWorkflowsStep
                configurationId={configuration?.id}
                isLoading={!configuration && versionWorkflowsLoading}
                onWorkflowDraftChange={handleWorkflowDraftChange}
                workflowDrafts={workflowDrafts}
                workflows={dialogWorkflows}
            />
        </>
    );

    const wizardTitle = `${configuration ? 'Edit' : 'New'} Project Configuration`;

    const wizardDescription = configuration
        ? 'Change how this configuration runs the project for connected users.'
        : 'Run a published project version for connected users.';

    return (
        <Dialog
            onOpenChange={(open) => {
                if (!open) {
                    onClose();
                }
            }}
            open
        >
            <DialogContent
                aria-label="Configuration Dialog"
                hasSidebar={isWizard}
                onInteractOutside={(event) => event.preventDefault()}
            >
                {isWizard ? (
                    <DialogWizard
                        description={wizardDescription}
                        footer={
                            <ConfigurationDialogFooter
                                canSave={canSave}
                                isSaving={isSaving}
                                onSave={() => void handleSubmit()}
                            />
                        }
                        icon={<SlidersHorizontalIcon />}
                        onComplete={handleSubmit}
                        steps={steps}
                        title={wizardTitle}
                    >
                        <ConfigurationDialogStepContent
                            basicStepContent={basicStepContent}
                            workflowsStepContent={workflowsStepContent}
                        />
                    </DialogWizard>
                ) : (
                    <DialogMain>
                        <form
                            className="flex min-h-0 flex-1 flex-col"
                            onSubmit={(event) => {
                                event.preventDefault();

                                void handleSubmit();
                            }}
                        >
                            <DialogHeader title="Upgrade Project Version" />

                            <DialogBody>
                                {versionFieldset}

                                {environmentBadgeFieldset}
                            </DialogBody>

                            <DialogFooter>
                                <DialogCancelButton />

                                <Button disabled={!canSave} label="Save" type="submit" />
                            </DialogFooter>
                        </form>
                    </DialogMain>
                )}
            </DialogContent>
        </Dialog>
    );
};

export default AutomationWorkflowProjectConfigurationDialog;
