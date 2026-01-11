import {Skeleton} from '@/components/ui/skeleton';
import {TemplateCard} from '@/pages/automation/templates/components/TemplateCard';
import TemplatesLayoutContainer from '@/pages/automation/templates/components/layout-container/TemplatesLayoutContainer';
import {useTemplatesStore} from '@/pages/automation/templates/stores/useTemplatesStore';
import {usePreBuiltWorkflowTemplatesQuery} from '@/shared/middleware/graphql';
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

    return (
        <TemplatesLayoutContainer searchPlaceholder="Search workflows..." title="Explore Workflow Templates">
            <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
                {isLoading ? (
                    <>
                        <Skeleton className="h-60 w-96" />

                        <Skeleton className="h-60 w-96" />

                        <Skeleton className="h-60 w-96" />
                    </>
                ) : preBuiltWorkflowTemplates && preBuiltWorkflowTemplates.length > 0 ? (
                    preBuiltWorkflowTemplates.map((template) => {
                        const icons = template.components.map((component) => component!.icon);

                        return (
                            <TemplateCard
                                authorName={template.authorName}
                                categories={template.categories}
                                description={template.workflow.description}
                                icons={icons as string[]}
                                key={template.id}
                                templateId={template.id!}
                                title={template.workflow.label}
                            />
                        );
                    })
                ) : (
                    <div className="text-muted-foreground">No workflow templates found.</div>
                )}
            </div>
        </TemplatesLayoutContainer>
    );
};

export default WorkflowTemplates;
