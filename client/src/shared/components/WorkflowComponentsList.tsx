import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import WorkflowComponentsIcon, {
    WorkflowComponentIconDefinitionType,
} from '@/pages/automation/project/components/projects-sidebar/components/WorkflowComponentsIcon';
import useElementWidth from '@/shared/hooks/useElementWidth';
import {useMemo, useRef} from 'react';
import {twMerge} from 'tailwind-merge';

const ICON_SLOT_WIDTH_PX = 38;
const ICON_TRAILING_MARGIN_PX = 8;
const OVERFLOW_PILL_WIDTH_PX = 28;

interface WorkflowComponentsListProps {
    filteredComponentNames: string[];
    fitToWidth?: boolean;
    maxIcons?: number;
    workflowComponentDefinitions: Record<string, WorkflowComponentIconDefinitionType | undefined>;
    workflowTaskDispatcherDefinitions: Record<string, WorkflowComponentIconDefinitionType | undefined>;
}

const WorkflowComponentsList = ({
    filteredComponentNames,
    fitToWidth = false,
    maxIcons = 7,
    workflowComponentDefinitions,
    workflowTaskDispatcherDefinitions,
}: WorkflowComponentsListProps) => {
    const containerRef = useRef<HTMLDivElement>(null);

    const containerWidth = useElementWidth(containerRef);

    const collapseOnSmallViewport = !fitToWidth;

    const visibleIconCount = useMemo(() => {
        if (!fitToWidth || containerWidth === 0) {
            return maxIcons;
        }

        const iconsFittingWithoutPill = Math.floor((containerWidth + ICON_TRAILING_MARGIN_PX) / ICON_SLOT_WIDTH_PX);

        if (filteredComponentNames.length <= Math.min(iconsFittingWithoutPill, maxIcons)) {
            return maxIcons;
        }

        const iconsFittingBesidePill = Math.floor((containerWidth - OVERFLOW_PILL_WIDTH_PX) / ICON_SLOT_WIDTH_PX);

        return Math.max(0, Math.min(iconsFittingBesidePill, maxIcons));
    }, [containerWidth, filteredComponentNames.length, fitToWidth, maxIcons]);

    const memoizedFilteredComponentNamesList = useMemo(
        () => ({
            icons: filteredComponentNames.slice(0, visibleIconCount),
            remainingComponents: filteredComponentNames.slice(visibleIconCount),
        }),
        [filteredComponentNames, visibleIconCount]
    );

    return (
        <div className={twMerge('flex', fitToWidth && 'min-w-0 flex-1 overflow-hidden')} ref={containerRef}>
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

            {filteredComponentNames?.length > visibleIconCount && (
                <div className={twMerge(collapseOnSmallViewport && 'hidden 2xl:block')}>
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <div className="flex size-7 items-center justify-center self-center rounded-full border border-stroke-neutral-secondary bg-background p-1">
                                <span className="self-center text-xs font-medium text-content-neutral-secondary">
                                    +{filteredComponentNames.length - visibleIconCount}
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
