import LazyLoadSVG from '@/components/LazyLoadSVG/LazyLoadSVG';
import {Skeleton} from '@/components/ui/skeleton';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';

interface WorkflowComponentsIconsProps {
    workflowComponentDefinitions: Record<string, ComponentDefinitionBasic | undefined>;
    name: string;
    workflowTaskDispatcherDefinitions: Record<string, ComponentDefinitionBasic | undefined>;
}

const WorkflowComponentsIcons = ({
    name,
    workflowComponentDefinitions,
    workflowTaskDispatcherDefinitions,
}: WorkflowComponentsIconsProps) => {
    const componentDefinition = workflowComponentDefinitions[name];
    const taskDispatcherDefinition = workflowTaskDispatcherDefinitions[name];

    return (
        <div
            aria-label="Workflow component icon"
            className="mr-2 flex items-center justify-center rounded-full border bg-background p-1"
            key={name}
        >
            <Tooltip>
                <TooltipTrigger asChild>
                    {componentDefinition?.icon || taskDispatcherDefinition?.icon ? (
                        <LazyLoadSVG
                            className="size-5 flex-none"
                            src={componentDefinition?.icon || taskDispatcherDefinition?.icon || ''}
                        />
                    ) : (
                        <Skeleton className="size-4 rounded-full" />
                    )}
                </TooltipTrigger>

                <TooltipContent side="top">{componentDefinition?.title || name}</TooltipContent>
            </Tooltip>
        </div>
    );
};

export default WorkflowComponentsIcons;
