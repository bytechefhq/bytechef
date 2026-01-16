import {Skeleton} from '@/components/ui/skeleton';
import {TemplateCard} from '@/pages/automation/templates/components/TemplateCard';
import TemplatesLayoutContainer from '@/pages/automation/templates/components/layout-container/TemplatesLayoutContainer';
import {useTemplatesStore} from '@/pages/automation/templates/stores/useTemplatesStore';
import {usePreBuiltProjectTemplatesQuery} from '@/shared/middleware/graphql';
import {useShallow} from 'zustand/react/shallow';

const ProjectTemplates = () => {
    const {category, query} = useTemplatesStore(
        useShallow((state) => ({
            category: state.category,
            query: state.query,
        }))
    );

    const {data: {preBuiltProjectTemplates} = {}, isLoading} = usePreBuiltProjectTemplatesQuery({
        category,
        query,
    });

    return (
        <TemplatesLayoutContainer searchPlaceholder="Search projects..." title="Explore Project Templates">
            <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
                {isLoading && (
                    <>
                        <Skeleton className="h-60 w-96" />
                        <Skeleton className="h-60 w-96" />
                        <Skeleton className="h-60 w-96" />
                    </>
                )}

                {!isLoading &&
                    preBuiltProjectTemplates &&
                    preBuiltProjectTemplates.length > 0 &&
                    preBuiltProjectTemplates.map((template) => {
                        const icons = template!.components.flatMap((templateComponent) =>
                            templateComponent!.value.map((component) => component!.icon)
                        );

                        return (
                            <TemplateCard
                                authorName={template!.authorName}
                                categories={template!.categories}
                                description={template!.project!.description}
                                icons={icons as string[]}
                                key={template.id}
                                templateId={template!.id!}
                                title={template!.project!.name}
                            />
                        );
                    })}

                {!isLoading && !preBuiltProjectTemplates && (
                    <span className="text-muted-foreground">No project templates found.</span>
                )}
            </div>
        </TemplatesLayoutContainer>
    );
};

export default ProjectTemplates;
