import DataSyncsLeftSidebarDropdownMenu from '@/pages/automation/data-syncs/components/DataSyncsLeftSidebarDropdownMenu';
import useDataSyncs from '@/pages/automation/data-syncs/hooks/useDataSyncs';
import {findElement} from '@/pages/automation/data-syncs/utils/dataSyncElements';
import getDataSyncPath from '@/pages/automation/data-syncs/utils/getDataSyncPath';
import WorkflowComponentsIcon, {
    WorkflowComponentIconDefinitionType,
} from '@/pages/automation/project/components/projects-sidebar/components/WorkflowComponentsIcon';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {DataSyncElementKind} from '@/shared/middleware/graphql';
import {useMemo} from 'react';
import {Link} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

interface ProjectDataSyncsListDataSyncI {
    description?: string | null;
    elements?: ({componentName: string; kind: DataSyncElementKind} | null)[] | null;
    id: string;
    lastModifiedDate?: string | null;
    projectId: string;
    title: string;
}

interface ProjectDataSyncsListProps {
    calculateTimeDifference: (date?: string) => string;
    currentDataSyncId?: string;
    emptyMessage: string;
    projectId: number;
}

interface ProjectDataSyncsListItemProps {
    calculateTimeDifference: (date?: string) => string;
    current: boolean;
    dataSync: ProjectDataSyncsListDataSyncI;
}

const ProjectDataSyncsListItem = ({calculateTimeDifference, current, dataSync}: ProjectDataSyncsListItemProps) => {
    const {componentDefinitions} = useWorkflowDataStore(
        useShallow((state) => ({componentDefinitions: state.componentDefinitions}))
    );

    const source = findElement(dataSync, DataSyncElementKind.Source);
    const destination = findElement(dataSync, DataSyncElementKind.Destination);

    const workflowComponentDefinitions = useMemo(() => {
        const definitions: Record<string, WorkflowComponentIconDefinitionType | undefined> = {};

        [source, destination].forEach((element) => {
            if (element) {
                definitions[element.componentName] = componentDefinitions?.find(
                    (definition) => definition.name === element.componentName
                );
            }
        });

        return definitions;
    }, [componentDefinitions, source, destination]);

    const componentNames = [source?.componentName, destination?.componentName].filter(
        (componentName): componentName is string => !!componentName
    );

    return (
        <li
            className={twMerge(
                'group flex w-full items-center rounded-md border border-transparent pr-1 hover:bg-background',
                current && 'border-stroke-brand-primary bg-background'
            )}
        >
            <Link
                className="flex min-w-0 flex-1 cursor-pointer flex-col gap-3 overflow-hidden py-3 pl-3"
                to={getDataSyncPath(dataSync)}
            >
                {componentNames.length > 0 && (
                    <div className="flex shrink-0 items-center">
                        {componentNames.map((componentName) => (
                            <WorkflowComponentsIcon
                                key={componentName}
                                name={componentName}
                                workflowComponentDefinitions={workflowComponentDefinitions}
                                workflowTaskDispatcherDefinitions={{}}
                            />
                        ))}
                    </div>
                )}

                <div className="flex min-w-0 flex-col gap-1 text-start">
                    <span className="truncate text-sm font-medium">{dataSync.title}</span>

                    <div className="flex gap-1 text-xs text-content-neutral-secondary">
                        <span>Edited</span>

                        {calculateTimeDifference(dataSync.lastModifiedDate ?? undefined)}
                    </div>
                </div>
            </Link>

            <DataSyncsLeftSidebarDropdownMenu current={current} dataSync={dataSync} />
        </li>
    );
};

const ProjectDataSyncsList = ({
    calculateTimeDifference,
    currentDataSyncId,
    emptyMessage,
    projectId,
}: ProjectDataSyncsListProps) => {
    const {dataSyncs} = useDataSyncs();

    const projectDataSyncs = useMemo(
        () => dataSyncs.filter((dataSync) => projectId === 0 || +dataSync.projectId === projectId),
        [dataSyncs, projectId]
    );

    if (projectDataSyncs.length === 0) {
        return <span className="w-full py-2 text-sm text-muted-foreground">{emptyMessage}</span>;
    }

    return (
        <ul className="flex flex-col gap-2">
            {projectDataSyncs.map((dataSync) => (
                <ProjectDataSyncsListItem
                    calculateTimeDifference={calculateTimeDifference}
                    current={dataSync.id === currentDataSyncId}
                    dataSync={dataSync}
                    key={dataSync.id}
                />
            ))}
        </ul>
    );
};

export default ProjectDataSyncsList;
