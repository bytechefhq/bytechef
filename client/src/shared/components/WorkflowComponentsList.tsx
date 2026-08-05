import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import WorkflowComponentsIcon from '@/pages/automation/project/components/projects-sidebar/components/WorkflowComponentsIcon';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {useMemo} from 'react';
import {twMerge} from 'tailwind-merge';

interface WorkflowComponentsListProps {
    /* Icons collapse to a single +N pill below the 2xl viewport breakpoint. Hosts with a FIXED-width
     * container (the workflow editor's w-80 sidebar card) must pass false — their available width never
     * tracks the viewport, so the collapse would hide icons that always fit. */
    collapseOnSmallViewport?: boolean;
    filteredComponentNames: string[];
    maxIcons?: number;
    workflowComponentDefinitions: Record<string, ComponentDefinitionBasic | undefined>;
    workflowTaskDispatcherDefinitions: Record<string, ComponentDefinitionBasic | undefined>;
}

const WorkflowComponentsList = ({
    collapseOnSmallViewport = true,
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
            <div className={twMerge('flex', collapseOnSmallViewport && 'hidden 2xl:flex')}>
                {memoizedFilteredComponentNamesList.icons.map((name) => (
                    <WorkflowComponentsIcon
                        key={name}
                        name={name}
                        workflowComponentDefinitions={workflowComponentDefinitions}
                        workflowTaskDispatcherDefinitions={workflowTaskDispatcherDefinitions}
                    />
                ))}
            </div>

            {collapseOnSmallViewport && filteredComponentNames?.length > 0 && (
                <div className="2xl:hidden">
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <div className="flex size-7 items-center justify-center self-center rounded-full border border-stroke-neutral-secondary bg-background p-1">
                                <span className="self-center text-xs font-medium text-content-neutral-secondary">
                                    +{filteredComponentNames.length}
                                </span>
                            </div>
                        </TooltipTrigger>

                        <TooltipContent className="mt-1 text-pretty" side="bottom">
                            {filteredComponentNames.map((name) => (
                                <div className="flex items-center gap-1 py-0.5" key={name}>
                                    <WorkflowComponentsIcon
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
                </div>
            )}

            {filteredComponentNames?.length > maxIcons && (
                <div className={twMerge(collapseOnSmallViewport && 'hidden 2xl:block')}>
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
                                    <WorkflowComponentsIcon
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
                </div>
            )}
        </div>
    );
};

export default WorkflowComponentsList;
