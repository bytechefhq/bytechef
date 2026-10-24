import Badge from '@/components/Badge/Badge';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import DataSyncsLeftSidebarDropdownMenu from '@/pages/automation/data-syncs/components/DataSyncsLeftSidebarDropdownMenu';
import {findElement} from '@/pages/automation/data-syncs/utils/dataSyncElements';
import {describeTrigger} from '@/pages/automation/data-syncs/utils/dataSyncTrigger';
import getDataSyncPath from '@/pages/automation/data-syncs/utils/getDataSyncPath';
import {DataSyncElementKind, DataSyncTriggerType} from '@/shared/middleware/graphql';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {ArrowLeftRightIcon, ArrowRightIcon, CalendarClockIcon, ComponentIcon, HandIcon} from 'lucide-react';
import {useMemo} from 'react';
import InlineSVG from 'react-inlinesvg';
import {Link} from 'react-router-dom';

interface ProjectDataSyncListItemDataSyncI {
    description?: string | null;
    elements?: ({componentName: string; kind: DataSyncElementKind} | null)[] | null;
    id: string;
    lastPublishedVersion: number;
    projectId: string;
    title: string;
    triggerParameters?: unknown;
    triggerType: DataSyncTriggerType;
}

interface ProjectDataSyncListItemProps {
    dataSync: ProjectDataSyncListItemDataSyncI;
}

const ProjectDataSyncListItem = ({dataSync}: ProjectDataSyncListItemProps) => {
    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({
        actionDefinitions: true,
        clusterElementDefinitions: true,
    });

    const definitionsByName = useMemo(
        () => new Map((componentDefinitions ?? []).map((definition) => [definition.name, definition])),
        [componentDefinitions]
    );

    const source = findElement(dataSync, DataSyncElementKind.Source);
    const destination = findElement(dataSync, DataSyncElementKind.Destination);

    const sourceDefinition = source ? definitionsByName.get(source.componentName) : undefined;
    const destinationDefinition = destination ? definitionsByName.get(destination.componentName) : undefined;

    const triggerSummary = describeTrigger(
        dataSync.triggerType,
        (dataSync.triggerParameters ?? undefined) as Record<string, unknown> | undefined
    );

    const isDraft = dataSync.lastPublishedVersion === 0;
    const displayVersion = isDraft ? 1 : dataSync.lastPublishedVersion;

    return (
        <li className="group flex items-center justify-between rounded-md px-3 py-1 hover:bg-surface-neutral-primary-hover">
            <Link
                aria-label={`Link to data sync ${dataSync.title}`}
                className="flex min-w-0 flex-1 items-center gap-2"
                data-testid={`${dataSync.id}-link`}
                to={getDataSyncPath(dataSync)}
            >
                <div className="flex w-80 min-w-0 shrink-0 items-center gap-2 pr-1 text-sm font-semibold">
                    <ArrowLeftRightIcon className="size-4 shrink-0 text-content-neutral-secondary" />

                    <Tooltip>
                        <TooltipTrigger className="line-clamp-1 min-w-0 flex-1 truncate text-start">
                            {dataSync.title}
                        </TooltipTrigger>

                        <TooltipContent align="start" className="max-w-md break-all">
                            {dataSync.title}
                        </TooltipContent>
                    </Tooltip>
                </div>

                <span className="hidden min-w-0 items-center gap-2 truncate text-sm font-normal text-content-neutral-secondary sm:flex">
                    <span className="flex min-w-0 items-center gap-1.5">
                        {sourceDefinition?.icon && (
                            <InlineSVG
                                className="size-4 shrink-0"
                                loader={<ComponentIcon className="size-4 shrink-0" />}
                                src={sourceDefinition.icon}
                                title={null}
                            />
                        )}

                        <span className="truncate">
                            {sourceDefinition?.title || source?.componentName || 'No source'}
                        </span>
                    </span>

                    <ArrowRightIcon className="size-4 shrink-0" />

                    <span className="flex min-w-0 items-center gap-1.5">
                        {destinationDefinition?.icon && (
                            <InlineSVG
                                className="size-4 shrink-0"
                                loader={<ComponentIcon className="size-4 shrink-0" />}
                                src={destinationDefinition.icon}
                                title={null}
                            />
                        )}

                        <span className="truncate">
                            {destinationDefinition?.title || destination?.componentName || 'No destination'}
                        </span>
                    </span>
                </span>
            </Link>

            <div className="flex items-center justify-end gap-x-6">
                <span className="flex items-center gap-1 text-sm text-content-neutral-secondary">
                    {dataSync.triggerType === DataSyncTriggerType.Schedule ? (
                        <CalendarClockIcon className="size-4 shrink-0" />
                    ) : (
                        <HandIcon className="size-4 shrink-0" />
                    )}

                    <span className="truncate">{triggerSummary}</span>
                </span>

                <Badge
                    className="flex space-x-1"
                    styleType={isDraft ? 'outline-outline' : 'success-outline'}
                    weight="semibold"
                >
                    <span>V{displayVersion}</span>

                    <span>{isDraft ? 'DRAFT' : 'PUBLISHED'}</span>
                </Badge>

                <DataSyncsLeftSidebarDropdownMenu current={false} dataSync={dataSync} />
            </div>
        </li>
    );
};

export default ProjectDataSyncListItem;
