import {Input} from '@/components/Input/Input';
import {SetAutomationEnabledRequestI} from '@/ee/pages/embedded/automation-hub/mutations/automationHub.mutations';
import TemplateCard from '@/ee/pages/embedded/automation-hub/views/components/TemplateCard';
import {
    AutomationWorkflowProject,
    AutomationWorkflowProjectKindEnum,
    AutomationWorkflowProjectWorkflowTemplate,
    ConnectedUserProjectWorkflow,
} from '@/ee/shared/middleware/embedded/public';
import {SearchIcon} from 'lucide-react';
import {useMemo, useState} from 'react';

interface TemplateGridSectionProps {
    activationDisabled?: boolean;
    automationsByTemplateId: Map<string, ConnectedUserProjectWorkflow>;
    onDeleteAutomation: (workflowUuid: string) => void;
    onDeprovisionReference: (workflowUuid: string) => void;
    onSetEnabled: (request: SetAutomationEnabledRequestI) => void;
    onUseTemplate: (
        template: AutomationWorkflowProjectWorkflowTemplate,
        kind: AutomationWorkflowProjectKindEnum
    ) => void;
    projects: AutomationWorkflowProject[];
}

/**
 * The catalog half of the Automations view: every published template grouped under its project,
 * each card carrying the usage state of the connected user's matching automation, if any.
 */
const TemplateGridSection = ({
    activationDisabled,
    automationsByTemplateId,
    onDeleteAutomation,
    onDeprovisionReference,
    onSetEnabled,
    onUseTemplate,
    projects,
}: TemplateGridSectionProps) => {
    const [search, setSearch] = useState('');

    const filteredProjects = useMemo(() => {
        const normalizedSearch = search.trim().toLowerCase();

        return projects
            .map((project) => ({
                ...project,
                workflowTemplates: (project.workflowTemplates || []).filter((template) =>
                    (template.label || '').toLowerCase().includes(normalizedSearch)
                ),
            }))
            .filter((project) => project.workflowTemplates.length > 0);
    }, [projects, search]);

    return (
        <div className="flex flex-col gap-6">
            <div className="relative w-full max-w-sm">
                <SearchIcon className="absolute top-2.5 left-3 size-4 text-muted-foreground" />

                <Input
                    className="pl-8"
                    onChange={(event) => setSearch(event.target.value)}
                    placeholder="Search templates"
                    value={search}
                />
            </div>

            {filteredProjects.length === 0 ? (
                <div className="flex items-center justify-center py-10 text-center text-muted-foreground">
                    No templates found.
                </div>
            ) : (
                <div className="flex flex-col gap-8">
                    {filteredProjects.map((project) => (
                        <section key={project.id}>
                            <h2 className="text-lg font-semibold">{project.name}</h2>

                            {project.description && (
                                <p className="mt-1 text-sm text-muted-foreground">{project.description}</p>
                            )}

                            <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                                {project.workflowTemplates.map((template) => (
                                    <TemplateCard
                                        activationDisabled={activationDisabled}
                                        automation={automationsByTemplateId.get(template.id!)}
                                        key={template.id}
                                        onDeleteAutomation={onDeleteAutomation}
                                        onDeprovisionReference={onDeprovisionReference}
                                        onSetEnabled={onSetEnabled}
                                        onUseTemplate={() => onUseTemplate(template, project.kind!)}
                                        template={template}
                                    />
                                ))}
                            </div>
                        </section>
                    ))}
                </div>
            )}
        </div>
    );
};

export default TemplateGridSection;
