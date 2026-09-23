import {SetAutomationEnabledRequestI} from '@/ee/pages/embedded/automation-hub/mutations/automationHub.mutations';
import AutomationCard from '@/ee/pages/embedded/automation-hub/views/components/AutomationCard';
import TemplateCard from '@/ee/pages/embedded/automation-hub/views/components/TemplateCard';
import {
    AutomationWorkflowProject,
    AutomationWorkflowProjectWorkflowTemplate,
    ConnectedUserProjectWorkflow,
} from '@/ee/shared/middleware/embedded/public';
import {useMemo} from 'react';
import {twMerge} from 'tailwind-merge';

export type CatalogLayoutType = 'grid' | 'list';

export type CatalogFilterType = 'active' | 'all' | 'enabled';

interface TemplateGridSectionProps {
    activationDisabled?: boolean;
    automationsByTemplateId: Map<string, ConnectedUserProjectWorkflow>;
    filter: CatalogFilterType;
    layout: CatalogLayoutType;
    onDeleteAutomation: (workflowUuid: string) => Promise<unknown>;
    onDeprovisionReference: (workflowUuid: string) => Promise<unknown>;
    onSetEnabled: (request: SetAutomationEnabledRequestI) => Promise<unknown>;
    onUseTemplate: (template: AutomationWorkflowProjectWorkflowTemplate) => void;
    newWorkflowEnabled: boolean;
    projects: AutomationWorkflowProject[];
    search: string;
    unmatchedAutomations: ConnectedUserProjectWorkflow[];
}

const TemplateGridSection = ({
    activationDisabled,
    automationsByTemplateId,
    filter,
    layout,
    newWorkflowEnabled,
    onDeleteAutomation,
    onDeprovisionReference,
    onSetEnabled,
    onUseTemplate,
    projects,
    search,
    unmatchedAutomations,
}: TemplateGridSectionProps) => {
    const normalizedSearch = useMemo(() => search.trim().toLowerCase(), [search]);

    const templates = useMemo(
        () =>
            projects.flatMap((project) =>
                (project.workflowTemplates || [])
                    .filter((template) => (template.label || '').toLowerCase().includes(normalizedSearch))
                    .filter((template) => {
                        const automation = automationsByTemplateId.get(template.id!);

                        if (filter === 'enabled') {
                            return !!automation?.enabled;
                        }

                        return filter === 'all' || !!automation;
                    })
            ),
        [automationsByTemplateId, filter, normalizedSearch, projects]
    );

    const automations = useMemo(
        () =>
            newWorkflowEnabled
                ? unmatchedAutomations
                      .filter((automation) => (automation.label || '').toLowerCase().includes(normalizedSearch))
                      .filter((automation) => filter !== 'enabled' || !!automation.enabled)
                : [],
        [filter, newWorkflowEnabled, normalizedSearch, unmatchedAutomations]
    );

    if (templates.length === 0 && automations.length === 0) {
        return (
            <div className="flex items-center justify-center py-10 text-center text-muted-foreground">
                No automations found.
            </div>
        );
    }

    return (
        <div
            className={twMerge('grid gap-4', layout === 'grid' && 'sm:grid-cols-2 lg:grid-cols-3')}
            data-layout={layout}
            data-testid="automations-catalog"
        >
            {templates.map((template) => (
                <TemplateCard
                    activationDisabled={activationDisabled}
                    automation={automationsByTemplateId.get(template.id!)}
                    key={template.id}
                    onDeleteAutomation={onDeleteAutomation}
                    onDeprovisionReference={onDeprovisionReference}
                    onSetEnabled={onSetEnabled}
                    onUseTemplate={() => onUseTemplate(template)}
                    template={template}
                />
            ))}

            {automations.length > 0 && <h2 className="col-span-full mt-4 text-lg font-semibold">My Automations</h2>}

            {automations.map((automation) => (
                <AutomationCard
                    automation={automation}
                    key={automation.workflowUuid}
                    onDeleteAutomation={onDeleteAutomation}
                    onDeprovisionReference={onDeprovisionReference}
                    onSetEnabled={onSetEnabled}
                />
            ))}
        </div>
    );
};

export default TemplateGridSection;
