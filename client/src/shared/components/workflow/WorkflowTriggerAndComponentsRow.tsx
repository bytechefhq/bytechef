import Badge from '@/components/Badge/Badge';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {WorkflowComponentIconDefinitionType} from '@/pages/automation/project/components/projects-sidebar/components/WorkflowComponentsIcon';
import WorkflowComponentsList from '@/shared/components/WorkflowComponentsList';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {ComponentIcon} from 'lucide-react';
import {useMemo} from 'react';
import InlineSVG from 'react-inlinesvg';
import {twMerge} from 'tailwind-merge';

interface WorkflowTriggerSourceI {
    triggers?: Array<{description?: string; label?: string; type?: string}>;
    workflowTriggerComponentNames?: Array<string>;
}

interface TriggerDataI {
    componentName: string;
    description: string;
    iconSrc: string;
    label: string;
}

interface WorkflowTriggerAndComponentsRowProps {
    className?: string;
    filteredComponentNames?: string[];
    fitToWidth?: boolean;
    maxIcons?: number;
    workflow: WorkflowTriggerSourceI;
    workflowComponentDefinitions: Record<string, WorkflowComponentIconDefinitionType | undefined>;
    workflowTaskDispatcherDefinitions: Record<string, WorkflowComponentIconDefinitionType | undefined>;
}

const WorkflowTriggerAndComponentsRow = ({
    className,
    filteredComponentNames,
    fitToWidth,
    maxIcons,
    workflow,
    workflowComponentDefinitions,
    workflowTaskDispatcherDefinitions,
}: WorkflowTriggerAndComponentsRowProps) => {
    const triggerComponentName = workflow.workflowTriggerComponentNames?.[0];
    const triggerType = workflow.triggers?.[0]?.type;

    const triggerVersionNumber = triggerType ? +triggerType.split('/')[1].replace('v', '') : 1;

    const {data: triggerComponentDefinition} = useGetComponentDefinitionQuery(
        {
            componentName: triggerComponentName || '',
            componentVersion: triggerVersionNumber,
        },
        !!triggerComponentName
    );

    const triggerData = useMemo<TriggerDataI | null>(() => {
        if (!triggerComponentName && !workflow.triggers?.[0]) {
            return null;
        }

        const triggerFromWorkflow = workflow.triggers?.[0];
        const triggerDefinition = workflowComponentDefinitions[triggerComponentName || ''];

        const matchedTrigger = triggerComponentDefinition?.triggers?.find(
            (trigger) => trigger.name === triggerFromWorkflow?.type?.split('/')[2]
        );

        if (!matchedTrigger && !triggerFromWorkflow) {
            return null;
        }

        return {
            componentName: triggerDefinition?.title || triggerComponentName || 'Unknown Trigger',
            description: matchedTrigger?.description || triggerFromWorkflow?.description || '',
            iconSrc: triggerDefinition?.icon || '',
            label: matchedTrigger?.title || triggerFromWorkflow?.label || '',
        };
    }, [workflow, workflowComponentDefinitions, triggerComponentName, triggerComponentDefinition]);

    const taskOnlyComponentNames = useMemo(() => {
        if (!filteredComponentNames) {
            return [];
        }

        const triggerCount = workflow.workflowTriggerComponentNames?.length ?? 0;

        return filteredComponentNames.slice(triggerCount);
    }, [filteredComponentNames, workflow.workflowTriggerComponentNames]);

    const triggerLabel = triggerData?.label || triggerData?.componentName || '';

    const triggerBadge = (
        <div className={fitToWidth ? 'min-w-0' : 'shrink-0'}>
            {fitToWidth ? (
                <Badge className="max-w-full" styleType="outline-outline" weight="semibold">
                    <span className="truncate">{triggerLabel}</span>
                </Badge>
            ) : (
                <Badge label={triggerLabel} styleType="outline-outline" weight="semibold" />
            )}
        </div>
    );

    return (
        <div className={twMerge('flex items-center gap-1', fitToWidth && 'min-w-0', className)}>
            {triggerData && (
                <div className={twMerge('flex items-center gap-1', fitToWidth ? 'min-w-0' : 'shrink-0')}>
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <div className="flex shrink-0 items-center justify-center rounded-full border border-stroke-neutral-primary bg-surface-neutral-primary p-1">
                                {triggerData.iconSrc ? (
                                    <InlineSVG
                                        className="size-5"
                                        loader={<ComponentIcon className="size-5 flex-none" />}
                                        src={triggerData.iconSrc}
                                        title={null}
                                    />
                                ) : (
                                    <ComponentIcon className="size-3 flex-none text-content-neutral-primary" />
                                )}
                            </div>
                        </TooltipTrigger>

                        <TooltipContent>{triggerData.componentName}</TooltipContent>
                    </Tooltip>

                    {triggerData.description ? (
                        <Tooltip>
                            <TooltipTrigger asChild>{triggerBadge}</TooltipTrigger>

                            <TooltipContent className="max-w-xs text-sm" side="right">
                                {triggerData.description}
                            </TooltipContent>
                        </Tooltip>
                    ) : (
                        triggerBadge
                    )}
                </div>
            )}

            <WorkflowComponentsList
                filteredComponentNames={taskOnlyComponentNames}
                fitToWidth={fitToWidth}
                maxIcons={maxIcons}
                workflowComponentDefinitions={workflowComponentDefinitions}
                workflowTaskDispatcherDefinitions={workflowTaskDispatcherDefinitions}
            />
        </div>
    );
};

export default WorkflowTriggerAndComponentsRow;
