import Button from '@/components/Button/Button';
import LoadingDots from '@/components/LoadingDots';
import {Alert, AlertDescription, AlertTitle} from '@/components/ui/alert';
import {
    useCreateBlankAutomationMutation,
    useDeleteAutomationMutation,
    useDeprovisionReferenceMutation,
    useSetAutomationEnabledMutation,
} from '@/ee/pages/embedded/automation-hub/mutations/automationHub.mutations';
import {
    useGetAutomationsQuery,
    useGetTemplateProjectsQuery,
} from '@/ee/pages/embedded/automation-hub/queries/automationHub.queries';
import {useAutomationHubStore} from '@/ee/pages/embedded/automation-hub/stores/useAutomationHubStore';
import TemplateGridSection from '@/ee/pages/embedded/automation-hub/views/components/TemplateGridSection';
import YourAutomationsSection from '@/ee/pages/embedded/automation-hub/views/components/YourAutomationsSection';
import ActivationWizard from '@/ee/pages/embedded/automation-hub/wizard/ActivationWizard';
import {
    AutomationWorkflowProjectKindEnum,
    AutomationWorkflowProjectWorkflowTemplate,
    ConnectedUserProjectWorkflow,
} from '@/ee/shared/middleware/embedded/public';
import {PlusIcon} from 'lucide-react';
import {useMemo, useState} from 'react';
import {useNavigate} from 'react-router-dom';

interface ActiveTemplateI {
    kind: AutomationWorkflowProjectKindEnum;
    template: AutomationWorkflowProjectWorkflowTemplate;
}

interface AutomationsViewProps {
    onActivate?: (template: AutomationWorkflowProjectWorkflowTemplate, kind: AutomationWorkflowProjectKindEnum) => void;
}

/**
 * The hub's landing view: the catalog itself carries each template's usage state, so there is no
 * separate list of what the user has activated. Below the grid sits everything a published
 * template does not account for — blank workflows, copies of withdrawn templates, and dangling
 * references.
 *
 * An automation is matched to its template by uuid: a REFERENCE through `catalogWorkflowUuid`, a
 * COPY through `copiedFromWorkflowUuid`. Exactly one automation can occupy a card, so nothing is
 * ever hidden: everything else — including a second copy of the same template — falls to the
 * section below, where it is individually removable.
 *
 * Activation is handled by the caller — `onActivate` is a testing seam; the real page instead
 * opens the `ActivationWizard` via the `activeTemplate` state below.
 */
const AutomationsView = ({onActivate}: AutomationsViewProps) => {
    const [activeTemplate, setActiveTemplate] = useState<ActiveTemplateI>();

    const newWorkflowEnabled = useAutomationHubStore((state) => state.tabs.newWorkflow);

    const navigate = useNavigate();

    const {data: projects, error: projectsError, isLoading: projectsLoading} = useGetTemplateProjectsQuery();
    const {data: automations, error: automationsError, isLoading: automationsLoading} = useGetAutomationsQuery();

    const {mutate: createBlankAutomation} = useCreateBlankAutomationMutation();
    const {mutate: deleteAutomation} = useDeleteAutomationMutation();
    const {mutate: deprovisionReference} = useDeprovisionReferenceMutation();
    const {mutate: setEnabled} = useSetAutomationEnabledMutation();

    const {automationsByTemplateId, unmatchedAutomations} = useMemo(() => {
        const publishedTemplateIds = new Set(
            (projects || []).flatMap((project) => project.workflowTemplates || []).map((template) => template.id)
        );

        const matchedAutomations = new Map<string, ConnectedUserProjectWorkflow>();
        const unmatched: ConnectedUserProjectWorkflow[] = [];

        for (const automation of automations || []) {
            const templateId =
                automation.kind === 'REFERENCE' ? automation.catalogWorkflowUuid : automation.copiedFromWorkflowUuid;

            // `dangling` is excluded explicitly rather than relied upon to fail the uuid check.
            // Today the only writer of `dangling = true` is the redeploy sweep, which sets it
            // precisely when the uuid leaves the published set — but that invariant spans four
            // server classes with nothing pinning it. A dangling reference absorbed into a card
            // would lose its "Needs attention" badge and its only route to removal.
            if (!templateId || automation.dangling || !publishedTemplateIds.has(templateId)) {
                unmatched.push(automation);

                continue;
            }

            // A template can legitimately have more than one automation — an automation created
            // through the vendor's API or the sync bridge's implicit copy. The first takes the
            // card; the extras become rows below, so every one of them stays removable.
            if (matchedAutomations.has(templateId)) {
                unmatched.push(automation);
            } else {
                matchedAutomations.set(templateId, automation);
            }
        }

        return {automationsByTemplateId: matchedAutomations, unmatchedAutomations: unmatched};
    }, [automations, projects]);

    const handleCreateBlankAutomation = () => {
        createBlankAutomation(undefined, {
            onSuccess: (workflowUuid) => navigate(`/embedded/hub/builder/${workflowUuid}`),
        });
    };

    const handleUseTemplate = (
        template: AutomationWorkflowProjectWorkflowTemplate,
        kind: AutomationWorkflowProjectKindEnum
    ) => {
        if (onActivate) {
            onActivate(template, kind);

            return;
        }

        setActiveTemplate({kind, template});
    };

    if (projectsLoading || automationsLoading) {
        return (
            <div className="flex size-full items-center justify-center" data-testid="automations-view-loading">
                <LoadingDots />
            </div>
        );
    }

    return (
        <div className="flex size-full flex-col gap-8 overflow-y-auto p-6">
            <div className="flex items-center justify-between gap-4">
                <h1 className="text-lg font-semibold">Automations</h1>

                {newWorkflowEnabled && (
                    <Button
                        icon={<PlusIcon className="size-4" />}
                        label="New automation"
                        onClick={handleCreateBlankAutomation}
                    />
                )}
            </div>

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
                    onDeleteAutomation={deleteAutomation}
                    onDeprovisionReference={deprovisionReference}
                    onSetEnabled={setEnabled}
                    onUseTemplate={handleUseTemplate}
                    projects={projects || []}
                />
            )}

            <YourAutomationsSection
                automations={unmatchedAutomations}
                onDeleteAutomation={deleteAutomation}
                onDeprovisionReference={deprovisionReference}
                onSetEnabled={setEnabled}
            />

            {activeTemplate && (
                <ActivationWizard
                    kind={activeTemplate.kind}
                    onClose={() => setActiveTemplate(undefined)}
                    template={activeTemplate.template}
                />
            )}
        </div>
    );
};

export default AutomationsView;
