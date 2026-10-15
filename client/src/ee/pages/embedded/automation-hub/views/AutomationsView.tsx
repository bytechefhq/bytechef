import LoadingDots from '@/components/LoadingDots';
import {Alert, AlertDescription, AlertTitle} from '@/components/ui/alert';
import {
    useCreateBlankAutomationMutation,
    useDeleteAutomationMutation,
    useDeprovisionReferenceMutation,
    useSetAutomationEnabledMutation,
} from '@/ee/pages/embedded/automation-hub/mutations/automationHub.mutations';
import {
    AutomationHubKeys,
    useGetAutomationsQuery,
    useGetTemplateProjectsQuery,
} from '@/ee/pages/embedded/automation-hub/queries/automationHub.queries';
import {useAutomationHubStore} from '@/ee/pages/embedded/automation-hub/stores/useAutomationHubStore';
import CatalogToolbar, {
    CATALOG_LAYOUT_STORAGE_KEY,
} from '@/ee/pages/embedded/automation-hub/views/components/CatalogToolbar';
import TemplateGridSection, {
    CatalogFilterType,
    CatalogLayoutType,
} from '@/ee/pages/embedded/automation-hub/views/components/TemplateGridSection';
import {getEmbedCredentials} from '@/ee/pages/embedded/shared/embedCredentials';
import {
    AutomationWorkflowProjectWorkflowTemplate,
    ConnectedUserProjectWorkflow,
} from '@/ee/shared/middleware/embedded/public';
import {ActivationDialog} from '@bytechef/embedded';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback, useMemo, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {useShallow} from 'zustand/react/shallow';

const readStoredLayout = (): string | undefined => {
    try {
        return localStorage.getItem(CATALOG_LAYOUT_STORAGE_KEY) ?? undefined;
    } catch {
        return undefined;
    }
};

interface AutomationsViewProps {
    onActivate?: (template: AutomationWorkflowProjectWorkflowTemplate) => void;
}

const AutomationsView = ({onActivate}: AutomationsViewProps) => {
    const [activeTemplate, setActiveTemplate] = useState<AutomationWorkflowProjectWorkflowTemplate>();
    const [storedLayout, setStoredLayout] = useState<CatalogLayoutType | undefined>(
        () => readStoredLayout() as CatalogLayoutType | undefined
    );

    const [filter, setFilter] = useState<CatalogFilterType>('all');
    const [search, setSearch] = useState('');

    const {
        connectionDialogAllowed,
        defaultLayout,
        editWorkflowAllowed,
        layoutSwitcherAllowed,
        newWorkflowEnabled,
        themeMode,
    } = useAutomationHubStore(
        useShallow((state) => ({
            connectionDialogAllowed: state.connectionDialogAllowed,
            defaultLayout: state.defaultLayout,
            editWorkflowAllowed: state.editWorkflowAllowed,
            layoutSwitcherAllowed: state.layoutSwitcherAllowed,
            newWorkflowEnabled: state.tabs.newWorkflow,
            themeMode: state.theme.mode,
        }))
    );

    const navigate = useNavigate();
    const queryClient = useQueryClient();

    const {data: projects, error: projectsError, isLoading: projectsLoading} = useGetTemplateProjectsQuery();
    const {data: automations, error: automationsError, isLoading: automationsLoading} = useGetAutomationsQuery();

    const {mutate: createBlankAutomation} = useCreateBlankAutomationMutation();
    const {mutateAsync: deleteAutomation} = useDeleteAutomationMutation();
    const {mutateAsync: deprovisionReference} = useDeprovisionReferenceMutation();
    const {mutateAsync: setEnabled} = useSetAutomationEnabledMutation();

    const handleActivated = useCallback(() => {
        void queryClient.invalidateQueries({queryKey: AutomationHubKeys.automations});
        void queryClient.invalidateQueries({queryKey: AutomationHubKeys.connections});
    }, [queryClient]);

    const handleEditWorkflow = useCallback(
        (workflowUuid: string) => void navigate(`/embedded/hub/builder/${workflowUuid}`),
        [navigate]
    );

    const {automationsByTemplateId, unmatchedAutomations} = useMemo(() => {
        const publishedTemplateIds = new Set(
            (projects || []).flatMap((project) => project.workflowTemplates || []).map((template) => template.id)
        );

        const matchedAutomations = new Map<string, ConnectedUserProjectWorkflow>();
        const unmatched: ConnectedUserProjectWorkflow[] = [];

        for (const automation of automations || []) {
            const templateId =
                automation.kind === 'REFERENCE' ? automation.automationWorkflowUuid : automation.copiedFromWorkflowUuid;

            if (!templateId || automation.dangling || !publishedTemplateIds.has(templateId)) {
                unmatched.push(automation);

                continue;
            }

            if (matchedAutomations.has(templateId)) {
                unmatched.push(automation);
            } else {
                matchedAutomations.set(templateId, automation);
            }
        }

        return {automationsByTemplateId: matchedAutomations, unmatchedAutomations: unmatched};
    }, [automations, projects]);

    const layout = layoutSwitcherAllowed ? (storedLayout ?? defaultLayout) : defaultLayout;

    const handleLayoutChange = (nextLayout: CatalogLayoutType) => {
        setStoredLayout(nextLayout);

        try {
            localStorage.setItem(CATALOG_LAYOUT_STORAGE_KEY, nextLayout);
        } catch {
            return;
        }
    };

    const handleCreateBlankAutomation = () => {
        createBlankAutomation(undefined, {
            onSuccess: (workflowUuid) => navigate(`/embedded/hub/builder/${workflowUuid}`),
        });
    };

    const handleUseTemplate = (template: AutomationWorkflowProjectWorkflowTemplate) => {
        if (onActivate) {
            onActivate(template);

            return;
        }

        setActiveTemplate(template);
    };

    if (projectsLoading || automationsLoading) {
        return (
            <div className="flex size-full items-center justify-center" data-testid="automations-view-loading">
                <LoadingDots />
            </div>
        );
    }

    return (
        <div className="flex size-full flex-col gap-6 overflow-y-auto">
            <CatalogToolbar
                filter={filter}
                layout={layout}
                layoutSwitcherAllowed={layoutSwitcherAllowed}
                newWorkflowEnabled={newWorkflowEnabled}
                onCreateBlankAutomation={handleCreateBlankAutomation}
                onFilterChange={setFilter}
                onLayoutChange={handleLayoutChange}
                onSearchChange={setSearch}
                search={search}
            />

            {automationsError && (
                <Alert variant="destructive">
                    <AlertTitle>Unable to load automations</AlertTitle>

                    <AlertDescription>{automationsError.message}</AlertDescription>
                </Alert>
            )}

            {projectsError ? (
                <Alert variant="destructive">
                    <AlertTitle>Unable to load templates</AlertTitle>

                    <AlertDescription>{projectsError.message}</AlertDescription>
                </Alert>
            ) : (
                <TemplateGridSection
                    activationDisabled={!!automationsError}
                    automationsByTemplateId={automationsByTemplateId}
                    filter={filter}
                    layout={layout}
                    newWorkflowEnabled={newWorkflowEnabled}
                    onDeleteAutomation={deleteAutomation}
                    onDeprovisionReference={deprovisionReference}
                    onSetEnabled={setEnabled}
                    onUseTemplate={handleUseTemplate}
                    projects={projects || []}
                    search={search}
                    unmatchedAutomations={unmatchedAutomations}
                />
            )}

            {activeTemplate?.id && (
                <ActivationDialog
                    baseUrl={import.meta.env.VITE_API_BASE_PATH || ''}
                    connectionCreationAllowed={connectionDialogAllowed}
                    environment={getEmbedCredentials().environment || 'PRODUCTION'}
                    jwtToken={getEmbedCredentials().jwtToken || ''}
                    mode={themeMode}
                    onActivated={handleActivated}
                    onClose={() => setActiveTemplate(undefined)}
                    onEditWorkflow={editWorkflowAllowed ? handleEditWorkflow : undefined}
                    workflowTemplateId={activeTemplate.id}
                />
            )}
        </div>
    );
};

export default AutomationsView;
