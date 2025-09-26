import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import WorkflowComponentsIcons from '@/pages/automation/project/components/projects-sidebar/components/WorkflowComponentsIcons';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {useMemo} from 'react';

interface WorkflowComponentsListProps {
    filteredComponentNames: string[];
    maxIcons?: number;
    workflowComponentDefinitions: Record<string, ComponentDefinitionBasic | undefined>;
    workflowTaskDispatcherDefinitions: Record<string, ComponentDefinitionBasic | undefined>;
}

const WorkflowComponentsList = ({
    filteredComponentNames,
    maxIcons = 7,
    workflowComponentDefinitions,
    workflowTaskDispatcherDefinitions,
}: WorkflowComponentsListProps) => {
    const memoizedFilteredComponentNamesList = useMemo(
        () => ({
            icons: filteredComponentNames.slice(0, maxIcons),
            remainingComponents: filteredComponentNames.slice(maxIcons),
        }),
        [filteredComponentNames, maxIcons]
    );

    return (
        <div className="flex">
            {memoizedFilteredComponentNamesList.icons.map((name) => (
                <WorkflowComponentsIcons
                    key={name}
                    name={name}
                    workflowComponentDefinitions={workflowComponentDefinitions}
                    workflowTaskDispatcherDefinitions={workflowTaskDispatcherDefinitions}
                />
            ))}

            {filteredComponentNames?.length > maxIcons && (
                <Tooltip>
                    <TooltipTrigger asChild>
                        <div className="flex size-7 items-center justify-center self-center rounded-full border border-stroke-neutral-secondary bg-background p-1">
                            <span className="self-center text-xs font-medium text-content-neutral-secondary">
                                +{filteredComponentNames.length - maxIcons}
                            </span>
                        </div>
                    </TooltipTrigger>

                    <TooltipContent className="mt-1 text-pretty" side="bottom">
                        {memoizedFilteredComponentNamesList.remainingComponents.map((name) => (
                            <div className="flex items-center gap-1 py-0.5" key={name}>
                                <WorkflowComponentsIcons
                                    key={name}
                                    name={name}
                                    workflowComponentDefinitions={workflowComponentDefinitions}
                                    workflowTaskDispatcherDefinitions={workflowTaskDispatcherDefinitions}
                                />

                                <span className="block">{name}</span>
                            </div>
                        ))}
                    </TooltipContent>
                </Tooltip>
            )}
        </div>
    );
};

export default WorkflowComponentsList;
