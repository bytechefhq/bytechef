import DataSyncDetailHeader from '@/pages/automation/data-syncs/components/detail/DataSyncDetailHeader';
import DataSyncWizard from '@/pages/automation/data-syncs/components/wizard/DataSyncWizard';
import ProjectBreadcrumb from '@/pages/automation/project/components/project-header/components/ProjectBreadcrumb';
import ProjectItemSelect from '@/pages/automation/project/components/project-header/components/ProjectItemSelect';
import ProjectsLeftSidebar from '@/pages/automation/project/components/projects-sidebar/ProjectsLeftSidebar';
import useProjectsLeftSidebarStore from '@/pages/automation/project/stores/useProjectsLeftSidebarStore';
import LeftSidebarButton from '@/shared/layout/LeftSidebarButton';
import {DataSync, useDataSyncQuery} from '@/shared/middleware/graphql';
import {useGetProjectWorkflowsQuery} from '@/shared/queries/automation/projectWorkflows.queries';
import {useGetProjectQuery} from '@/shared/queries/automation/projects.queries';
import {useNavigate, useParams} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

const ProjectDataSync = () => {
    const {projectLeftSidebarOpen, setProjectLeftSidebarOpen} = useProjectsLeftSidebarStore(
        useShallow((state) => ({
            projectLeftSidebarOpen: state.projectLeftSidebarOpen,
            setProjectLeftSidebarOpen: state.setProjectLeftSidebarOpen,
        }))
    );

    const navigate = useNavigate();
    const {dataSyncId, projectId} = useParams<{dataSyncId: string; projectId: string}>();

    const {data} = useDataSyncQuery({id: dataSyncId ?? ''}, {enabled: !!dataSyncId});

    const dataSync = data?.dataSync;

    const {data: project} = useGetProjectQuery(+(dataSync?.projectId ?? 0), undefined, !!dataSync);

    const {data: projectWorkflows} = useGetProjectWorkflowsQuery(+(dataSync?.projectId ?? 0), !!dataSync);

    return (
        <div className="flex size-full">
            <div
                className={twMerge(
                    'h-full w-[355px] shrink-0 transition-[margin-left,opacity] duration-300 ease-[cubic-bezier(0.33,1,0.68,1)]',
                    projectLeftSidebarOpen ? 'ml-0 opacity-100' : 'ml-[-355px] opacity-0'
                )}
            >
                <ProjectsLeftSidebar
                    currentDataSyncId={dataSyncId}
                    currentWorkflowId=""
                    onProjectClick={(clickedProjectId, projectWorkflowId) =>
                        navigate(`/automation/projects/${clickedProjectId}/project-workflows/${projectWorkflowId}`)
                    }
                    projectId={+(projectId ?? 0)}
                />
            </div>

            <div className="flex min-w-0 flex-1 flex-col">
                {dataSync && (
                    <DataSyncDetailHeader
                        description={dataSync.description}
                        id={dataSync.id}
                        lastPublishedVersion={dataSync.lastPublishedVersion}
                        leading={
                            <div className="flex items-center gap-2">
                                <LeftSidebarButton
                                    onLeftSidebarOpenClick={() => setProjectLeftSidebarOpen(!projectLeftSidebarOpen)}
                                />

                                {project && (
                                    <ProjectBreadcrumb
                                        itemSelect={
                                            <ProjectItemSelect
                                                currentDataSyncId={dataSync.id}
                                                currentLabel={dataSync.title}
                                                onWorkflowValueChange={(projectWorkflowId) =>
                                                    navigate(
                                                        `/automation/projects/${project.id}/project-workflows/${projectWorkflowId}`
                                                    )
                                                }
                                                projectId={project.id!}
                                                projectWorkflows={projectWorkflows ?? []}
                                            />
                                        }
                                        project={project}
                                    />
                                )}
                            </div>
                        }
                        project={project}
                        projectId={dataSync.projectId}
                        title={dataSync.title}
                    />
                )}

                <div className="flex min-h-0 flex-1">
                    {dataSync && (
                        <div className="min-w-0 flex-1 overflow-y-auto">
                            <DataSyncWizard dataSync={dataSync as DataSync} key={dataSync.id} />
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default ProjectDataSync;
