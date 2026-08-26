import EmptyList from '@/components/EmptyList';
import {Skeleton} from '@/components/ui/skeleton';
import {TemplateCard} from '@/pages/automation/templates/components/TemplateCard';
import TemplatesLayoutContainer from '@/pages/automation/templates/components/layout-container/TemplatesLayoutContainer';
import {useTemplatesStore} from '@/pages/automation/templates/stores/useTemplatesStore';
import {usePreBuiltProjectTemplatesQuery} from '@/shared/middleware/graphql';
import {LayoutTemplateIcon} from 'lucide-react';
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

    const filtered = !!category || !!query;
    const hasTemplates = !!preBuiltProjectTemplates?.length;

    return (
        <TemplatesLayoutContainer searchPlaceholder="Search projects..." title="Explore Project Templates">
            {isLoading && (
                <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
                    <Skeleton className="h-60 w-96" />

                    <Skeleton className="h-60 w-96" />

                    <Skeleton className="h-60 w-96" />
                </div>
            )}

            {!isLoading && hasTemplates && (
                <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
                    {preBuiltProjectTemplates!.map((template) => {
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
                </div>
            )}

            {!isLoading && !hasTemplates && (
                <EmptyList
                    icon={<LayoutTemplateIcon className="size-12 text-content-neutral-tertiary" />}
                    message={
                        filtered
                            ? 'Try a different search term or category.'
                            : 'There are no project templates available to import yet.'
                    }
                    title={filtered ? 'No matching project templates' : 'No project templates'}
                />
            )}
        </TemplatesLayoutContainer>
    );
};

export default ProjectTemplates;
