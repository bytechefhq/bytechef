import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import ComboBox, {ComboBoxItemType} from '@/components/ComboBox/ComboBox';
import {
    Dialog,
    DialogCancelButton,
    DialogContent,
    DialogFooter,
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
import ConfigurationDialogTabs, {ConfigurationDialogTabType} from '@/ee/pages/embedded/shared/ConfigurationDialogTabs';
import {useGetComponentDefinitionsQuery} from '@/ee/shared/queries/embedded/componentDefinitions.queries';
import {
    AutomationProjectConfigurationsQuery,
    AutomationProjectsQuery,
    useAutomationProjectConfigurationVersionImpactQuery,
    useAutomationProjectConfigurationsQuery,
    useAutomationProjectVersionWorkflowsQuery,
    useAutomationProjectVersionsQuery,
    useCreateAutomationProjectConfigurationMutation,
    useEnableAutomationProjectConfigurationWorkflowMutation,
    useUpdateAutomationProjectConfigurationMutation,
    useUpdateAutomationProjectConfigurationWorkflowMutation,
} from '@/shared/middleware/graphql';
import {useGetEnvironmentsQuery} from '@/shared/queries/platform/environments.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {InfoIcon, Settings2Icon} from 'lucide-react';
import {ReactNode, useMemo, useState} from 'react';

import AutomationProjectConfigurationDialogWorkflowsStep from './AutomationProjectConfigurationDialogWorkflowsStep';
import {
    ConfigurationDialogWorkflowI,
    WorkflowDraftI,
    getWorkflowChanges,
} from './automationProjectConfigurationDialog-utils';

type AutomationProjectConfigurationType =
    AutomationProjectConfigurationsQuery['automationProjectConfigurations'][number];
type AutomationProjectType = AutomationProjectsQuery['automationProjects'][number];

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

interface AutomationProjectConfigurationDialogProps {
    configuration?: AutomationProjectConfigurationType;
    onClose: () => void;
    onSuccess?: (configurationId: string) => void;
    project?: AutomationProjectType;
    projects?: AutomationProjectType[];
    showTabs?: boolean;
    updateProjectVersion?: boolean;
}

const AutomationProjectConfigurationDialog = ({
    configuration: configurationProp,
    onClose,
    onSuccess,
    project: fixedProject,
    projects = [],
    showTabs = false,
    updateProjectVersion: updateProjectVersionProp = false,
}: AutomationProjectConfigurationDialogProps) => {
    const [automationHubVisible, setAutomationHubVisible] = useState(configurationProp?.automationHubVisible ?? true);
    const [basicStepTab, setBasicStepTab] = useState<ConfigurationDialogTabType | undefined>();
    const [chosenProjectVersion, setChosenProjectVersion] = useState(
        (updateProjectVersionProp
            ? fixedProject?.lastPublishedVersion
            : configurationProp?.projectVersion
        )?.toString() ?? ''
    );
    const [createdConfigurationId, setCreatedConfigurationId] = useState<string | undefined>();
    const [enteredName, setEnteredName] = useState<string | undefined>(configurationProp?.name);
    const [isSaving, setIsSaving] = useState(false);
    const [permissionExpression, setPermissionExpression] = useState(configurationProp?.permissionExpression ?? '');
    const [selectedExistingConfigurationId, setSelectedExistingConfigurationId] = useState<string | undefined>();
    const [selectedProjectId, setSelectedProjectId] = useState<string | undefined>(
        fixedProject?.id ?? configurationProp?.projectId
    );
    const [workflowChangesFailed, setWorkflowChangesFailed] = useState(false);
    const [workflowDrafts, setWorkflowDrafts] = useState<Record<string, WorkflowDraftI>>({});

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const queryClient = useQueryClient();

    const {data: environments} = useGetEnvironmentsQuery();

    const project = fixedProject ?? projects.find((projectItem) => projectItem.id === selectedProjectId);

    const projectVersion = chosenProjectVersion || project?.lastPublishedVersion?.toString() || '';

    const {data: configurationsData} = useAutomationProjectConfigurationsQuery(
        {projectId: project?.id ?? ''},
        {enabled: !!project}
    );

    const dialogEnvironmentId = configurationProp?.environmentId ?? String(currentEnvironmentId);

    const environmentConfigurations = useMemo(
        () =>
            (configurationsData?.automationProjectConfigurations ?? []).filter(
                (existingConfiguration) => existingConfiguration.environmentId === dialogEnvironmentId
            ),
        [configurationsData, dialogEnvironmentId]
    );

    const isChangeVersionTab = showTabs && basicStepTab === 'change-version';

    const selectedExistingConfiguration = isChangeVersionTab
        ? environmentConfigurations.find(
              (existingConfiguration) => existingConfiguration.id === selectedExistingConfigurationId
          ) || (environmentConfigurations.length === 1 ? environmentConfigurations[0] : undefined)
        : undefined;

    const configuration = configurationProp ?? selectedExistingConfiguration;

    const updateProjectVersion = updateProjectVersionProp || isChangeVersionTab;

    if (showTabs && !basicStepTab && configurationsData) {
        setBasicStepTab(environmentConfigurations.length > 0 ? 'change-version' : 'new-configuration');
    }

    const {data: versionsData} = useAutomationProjectVersionsQuery({id: project?.id ?? ''}, {enabled: !!project});
    const {data: versionWorkflowsData, isLoading: versionWorkflowsLoading} = useAutomationProjectVersionWorkflowsQuery(
        {id: project?.id ?? '', projectVersion: Number(projectVersion)},
        {enabled: (updateProjectVersionProp || !configuration) && !!project && projectVersion !== ''}
    );

    const changesVersion =
        !!configuration && projectVersion !== '' && Number(projectVersion) !== configuration.projectVersion;

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({connectionDefinitions: true}, changesVersion);
    const {data: impactData} = useAutomationProjectConfigurationVersionImpactQuery(
        {id: configuration?.id ?? '', projectVersion: Number(projectVersion)},
        {enabled: changesVersion}
    );

    const createConfigurationMutation = useCreateAutomationProjectConfigurationMutation();
    const enableWorkflowMutation = useEnableAutomationProjectConfigurationWorkflowMutation();
    const updateConfigurationMutation = useUpdateAutomationProjectConfigurationMutation();
    const updateWorkflowMutation = useUpdateAutomationProjectConfigurationWorkflowMutation();

    const environmentId = configuration?.environmentId ?? dialogEnvironmentId;

    const publishedVersions = useMemo(
        () =>
            (versionsData?.automationProjectVersions ?? []).filter(
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
            (configurationsData?.automationProjectConfigurations ?? []).some(
                (existingConfiguration) =>
                    existingConfiguration.environmentId === environmentId &&
                    existingConfiguration.id !== configuration?.id &&
                    existingConfiguration.id !== createdConfigurationId &&
                    existingConfiguration.name.trim().toLowerCase() === trimmedCandidateName
            )
        );
    };

    const getAvailableName = (baseName: string) => {
        let candidateName = baseName;

        for (let suffix = 2; isNameTaken(candidateName); suffix++) {
            candidateName = `${baseName} ${suffix}`;
        }

        return candidateName;
    };

    const prefilledName = !configuration && project ? getAvailableName(project.name) : '';

    const name = enteredName ?? prefilledName;

    const trimmedName = name.trim();

    const nameTaken = !updateProjectVersion && isNameTaken(name);

    const basicFieldsLocked = !!createdConfigurationId;

    const canSave =
        !isSaving &&
        !nameTaken &&
        !!project &&
        !!projectVersion &&
        (updateProjectVersion ? !!configuration : !!trimmedName) &&
        (!showTabs || !!basicStepTab);

    const movesToOlderVersion =
        !!configuration && projectVersion !== '' && Number(projectVersion) < configuration.projectVersion;

    const dialogWorkflows = useMemo<ConfigurationDialogWorkflowI[]>(() => {
        if (configuration && updateProjectVersionProp) {
            return (versionWorkflowsData?.automationProjectVersionWorkflows ?? []).map((versionWorkflow) => ({
                enabled:
                    configuration.workflows.find(
                        (configurationWorkflow) => configurationWorkflow.workflowUuid === versionWorkflow.workflowUuid
                    )?.enabled ?? true,
                label: versionWorkflow.label || versionWorkflow.workflowUuid,
                workflowUuid: versionWorkflow.workflowUuid,
            }));
        }

        if (configuration) {
            return configuration.workflows.map((configurationWorkflow) => ({
                enabled: configurationWorkflow.enabled,
                label: configurationWorkflow.label || configurationWorkflow.workflowUuid,
                workflowUuid: configurationWorkflow.workflowUuid,
            }));
        }

        return (versionWorkflowsData?.automationProjectVersionWorkflows ?? []).map((versionWorkflow) => ({
            enabled: true,
            label: versionWorkflow.label || versionWorkflow.workflowUuid,
            workflowUuid: versionWorkflow.workflowUuid,
        }));
    }, [configuration, updateProjectVersionProp, versionWorkflowsData]);

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

        for (const impactWorkflow of impactData?.automationProjectConfigurationVersionImpact.workflows ?? []) {
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
        () =>
            isChangeVersionTab
                ? [{canProceed: canSave, id: 'basic', label: 'Basic'}]
                : [
                      {canProceed: canSave, id: 'basic', label: 'Basic'},
                      {canProceed: canSave, id: 'workflows', label: 'Workflows'},
                  ],
        [canSave, isChangeVersionTab]
    );

    const configurationItems = useMemo(
        () =>
            environmentConfigurations.map((existingConfiguration) => ({
                label: `${existingConfiguration.name} - V${existingConfiguration.projectVersion}`,
                value: existingConfiguration.id,
            })),
        [environmentConfigurations]
    );

    const handleBasicStepTabChange = (tab: ConfigurationDialogTabType) => {
        setBasicStepTab(tab);
        setSelectedExistingConfigurationId(undefined);
        setWorkflowDrafts({});
    };

    const handleExistingConfigurationSelect = (configurationId: string) => {
        setSelectedExistingConfigurationId(configurationId);
        setWorkflowDrafts({});
    };

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
                queryKey: ['automationProjectConfigurationWorkflowProvidableValues'],
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
                automationHubVisible: updateProjectVersion ? configuration.automationHubVisible : automationHubVisible,
                enabled: configuration.enabled,
                id: configuration.id,
                name: updateProjectVersion ? configuration.name : trimmedName,
                permissionExpression: updateProjectVersion
                    ? configuration.permissionExpression
                    : permissionExpression.trim() || null,
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

        setCreatedConfigurationId(createResult.createAutomationProjectConfiguration);

        return createResult.createAutomationProjectConfiguration;
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

            void queryClient.invalidateQueries({queryKey: ['automationProjectConfigurations']});
            void queryClient.invalidateQueries({queryKey: ['automationProjects']});

            setWorkflowChangesFailed(!workflowChangesSaved);

            if (workflowChangesSaved) {
                onSuccess?.(configurationId);

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
            <div className="flex items-center gap-1.5">
                <Label htmlFor="configurationPermissionExpression">Assignment rule</Label>

                <Tooltip>
                    <TooltipTrigger asChild>
                        <InfoIcon
                            aria-label="About Assignment rule"
                            className="size-4 text-content-neutral-secondary"
                        />
                    </TooltipTrigger>

                    <TooltipContent className="max-w-80">
                        Which new users self-serve places on this configuration. Checked once, when a user is first
                        assigned. Users already on it stay until you move them. Empty means everyone.
                    </TooltipContent>
                </Tooltip>
            </div>

            <Textarea
                disabled={basicFieldsLocked}
                id="configurationPermissionExpression"
                onChange={(event) => setPermissionExpression(event.target.value)}
                placeholder="e.g. metadata['beta'] == 'true'"
                rows={3}
                value={permissionExpression}
            />
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
                value={projectVersion}
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

    const newConfigurationContent = (
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

    const basicStepContent = showTabs ? (
        basicStepTab && (
            <ConfigurationDialogTabs
                activeTab={basicStepTab}
                changeVersionContent={
                    <>
                        {versionFieldset}

                        {environmentBadgeFieldset}
                    </>
                }
                configurationItems={configurationItems}
                emptyTitle="No configurations for this project yet"
                newConfigurationContent={newConfigurationContent}
                onConfigurationSelect={handleExistingConfigurationSelect}
                onTabChange={handleBasicStepTabChange}
                selectedConfigurationValue={selectedExistingConfiguration?.id}
            />
        )
    ) : updateProjectVersionProp ? (
        <>
            {environmentBadgeFieldset}

            {versionFieldset}
        </>
    ) : (
        newConfigurationContent
    );

    const workflowsStepContent = (
        <>
            {workflowChangesFailedMessage}

            <AutomationProjectConfigurationDialogWorkflowsStep
                configurationId={configuration?.id}
                description={
                    updateProjectVersionProp
                        ? `Choose which workflows of V${projectVersion} this configuration runs. Connections and inputs set here are used by every connected user and cannot be changed by them.`
                        : undefined
                }
                isLoading={(updateProjectVersionProp || !configuration) && versionWorkflowsLoading}
                onWorkflowDraftChange={handleWorkflowDraftChange}
                projectVersion={updateProjectVersionProp && projectVersion ? Number(projectVersion) : undefined}
                workflowDrafts={workflowDrafts}
                workflows={dialogWorkflows}
            />
        </>
    );

    const wizardTitle = updateProjectVersion
        ? 'Change Project Version'
        : `${configuration ? 'Edit' : 'New'} Configuration`;

    const wizardDescription = updateProjectVersion
        ? 'Move this configuration to another project version.'
        : configuration
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
                hasSidebar
                onInteractOutside={(event) => event.preventDefault()}
            >
                <DialogWizard
                    description={wizardDescription}
                    footer={
                        <ConfigurationDialogFooter
                            canSave={canSave}
                            isSaving={isSaving}
                            onSave={() => void handleSubmit()}
                        />
                    }
                    icon={<Settings2Icon />}
                    onComplete={handleSubmit}
                    steps={steps}
                    title={wizardTitle}
                >
                    <ConfigurationDialogStepContent
                        basicStepContent={basicStepContent}
                        workflowsStepContent={workflowsStepContent}
                    />
                </DialogWizard>
            </DialogContent>
        </Dialog>
    );
};

export default AutomationProjectConfigurationDialog;
