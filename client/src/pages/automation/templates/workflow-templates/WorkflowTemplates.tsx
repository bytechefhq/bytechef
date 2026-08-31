import EmptyList from '@/components/EmptyList';
import {Skeleton} from '@/components/ui/skeleton';
import {TemplateCard} from '@/pages/automation/templates/components/TemplateCard';
import TemplatesLayoutContainer from '@/pages/automation/templates/components/layout-container/TemplatesLayoutContainer';
import {useTemplatesStore} from '@/pages/automation/templates/stores/useTemplatesStore';
import {usePreBuiltWorkflowTemplatesQuery} from '@/shared/middleware/graphql';
import {LayoutTemplateIcon} from 'lucide-react';
import {useShallow} from 'zustand/react/shallow';

const WorkflowTemplates = () => {
    const {category, query} = useTemplatesStore(
        useShallow((state) => ({
            category: state.category,
            query: state.query,
        }))
    );

    const {data: {preBuiltWorkflowTemplates} = {}, isLoading} = usePreBuiltWorkflowTemplatesQuery({
        category,
        query,
    });

    const filtered = !!category || !!query;
    const workflowTemplates = preBuiltWorkflowTemplates ?? [];
    const hasTemplates = workflowTemplates.length > 0;

    return (
        <TemplatesLayoutContainer searchPlaceholder="Search workflows..." title="Explore Workflow Templates">
            {isLoading && (
                <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
                    <Skeleton className="h-60 w-96" />

                    <Skeleton className="h-60 w-96" />

                    <Skeleton className="h-60 w-96" />
                </div>
            )}

            {!isLoading && hasTemplates && (
                <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
                    {workflowTemplates.map((template) => {
                        const icons = template.components
                            .map((component) => component.icon)
                            .filter((icon): icon is string => icon != null);

                        return (
                            <TemplateCard
                                authorName={template.authorName}
                                categories={template.categories}
                                description={template.workflow.description}
                                icons={icons}
                                key={template.id}
                                templateId={template.id!}
                                title={template.workflow.label}
                            />
                        );
                    })}
                </div>
            )}

            {!isLoading && !hasTemplates && (
                <EmptyList
                    icon={<LayoutTemplateIcon className="size-12 text-content-neutral-tertiary" />}
                    message={
                        filtered
                            ? 'Try a different search term or category.'
                            : 'There are no workflow templates available to import yet.'
                    }
                    title={filtered ? 'No matching workflow templates' : 'No workflow templates'}
                />
            )}
        </TemplatesLayoutContainer>
    );
};

export default WorkflowTemplates;
