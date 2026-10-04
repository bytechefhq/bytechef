import {ReactNode} from 'react';

interface McpServerToolsSectionProps {
    children: ReactNode;
    title: string;
}

const McpServerToolsSection = ({children, title}: McpServerToolsSectionProps) => (
    <section aria-label={title} className="flex flex-col gap-1.5">
        <h3 className="text-sm font-semibold text-content-neutral-secondary">{title}</h3>

        {children}
    </section>
);

interface McpServerToolsSectionsProps {
    componentList: ReactNode;
    showComponentList: boolean;
    showWorkflowList: boolean;
    workflowList: ReactNode;
}

const McpServerToolsSections = ({
    componentList,
    showComponentList,
    showWorkflowList,
    workflowList,
}: McpServerToolsSectionsProps) => (
    <div className="flex flex-col gap-4">
        {showComponentList && <McpServerToolsSection title="Components">{componentList}</McpServerToolsSection>}

        {showWorkflowList && <McpServerToolsSection title="Workflows">{workflowList}</McpServerToolsSection>}
    </div>
);

export default McpServerToolsSections;
