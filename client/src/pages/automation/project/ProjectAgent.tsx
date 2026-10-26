import AgentDetailContent from '@/pages/automation/agents/AgentDetailContent';
import AgentDetailHeader from '@/pages/automation/agents/components/detail/AgentDetailHeader';
import AgentTestChatPanel from '@/pages/automation/agents/components/detail/AgentTestChatPanel';
import ProjectBreadcrumb from '@/pages/automation/project/components/project-header/components/ProjectBreadcrumb';
import ProjectItemSelect from '@/pages/automation/project/components/project-header/components/ProjectItemSelect';
import ProjectsLeftSidebar from '@/pages/automation/project/components/projects-sidebar/ProjectsLeftSidebar';
import useProjectsLeftSidebarStore from '@/pages/automation/project/stores/useProjectsLeftSidebarStore';
import useOpenCopilot from '@/shared/components/copilot/hooks/useOpenCopilot';
import {MODE, Source} from '@/shared/components/copilot/stores/useCopilotStore';
import useElementWidth from '@/shared/hooks/useElementWidth';
import LeftSidebarButton from '@/shared/layout/LeftSidebarButton';
import {useAiAgentQuery} from '@/shared/middleware/graphql';
import {useGetProjectWorkflowsQuery} from '@/shared/queries/automation/projectWorkflows.queries';
import {useGetProjectQuery} from '@/shared/queries/automation/projects.queries';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useRef, useState} from 'react';
import {useNavigate, useParams} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

const FORM_MIN_WIDTH = 420;

const TEST_PANEL_MIN_WIDTH = 320;

const TEST_PANEL_MAX_WIDTH = 580;

const TEST_PANEL_MARGIN = 24;

const TEST_PANEL_HIDE_THRESHOLD = FORM_MIN_WIDTH + TEST_PANEL_MIN_WIDTH + TEST_PANEL_MARGIN;

const ProjectAgent = () => {
    const [testPanelOpen, setTestPanelOpen] = useState(true);

    const contentRowRef = useRef<HTMLDivElement>(null);

    const {projectLeftSidebarOpen, setProjectLeftSidebarOpen} = useProjectsLeftSidebarStore(
        useShallow((state) => ({
            projectLeftSidebarOpen: state.projectLeftSidebarOpen,
            setProjectLeftSidebarOpen: state.setProjectLeftSidebarOpen,
        }))
    );

    const copilotEnabled = useApplicationInfoStore((state) => state.ai.copilot.enabled);

    const contentRowWidth = useElementWidth(contentRowRef);
    const openCopilot = useOpenCopilot();

    const hasRoomForTestPanel = contentRowWidth === 0 || contentRowWidth >= TEST_PANEL_HIDE_THRESHOLD;

    const testPanelVisible = testPanelOpen && hasRoomForTestPanel;

    const testPanelWidth = Math.min(
        TEST_PANEL_MAX_WIDTH,
        Math.max(TEST_PANEL_MIN_WIDTH, contentRowWidth - FORM_MIN_WIDTH - TEST_PANEL_MARGIN)
    );

    const handleToggleTestPanel = () => {
        const opening = !testPanelVisible;

        if (opening && !hasRoomForTestPanel && projectLeftSidebarOpen) {
            setProjectLeftSidebarOpen(false);
        }

        setTestPanelOpen(opening);
    };

    const navigate = useNavigate();
    const {agentId, projectId} = useParams<{agentId: string; projectId: string}>();

    const {data} = useAiAgentQuery({id: agentId ?? ''}, {enabled: !!agentId});

    const agent = data?.aiAgent;

    const {data: project} = useGetProjectQuery(+(agent?.projectId ?? 0), undefined, !!agent);

    const {data: projectWorkflows} = useGetProjectWorkflowsQuery(+(agent?.projectId ?? 0), !!agent);

    return (
        <div className="flex size-full">
            <div
                className={twMerge(
                    'h-full w-[355px] shrink-0 transition-[margin-left,opacity] duration-300 ease-[cubic-bezier(0.33,1,0.68,1)]',
                    projectLeftSidebarOpen ? 'ml-0 opacity-100' : 'ml-[-355px] opacity-0'
                )}
            >
                <ProjectsLeftSidebar
                    currentAgentId={agentId}
                    currentWorkflowId=""
                    onProjectClick={(clickedProjectId, projectWorkflowId) =>
                        navigate(`/automation/projects/${clickedProjectId}/project-workflows/${projectWorkflowId}`)
                    }
                    projectId={+(projectId ?? 0)}
                />
            </div>

            <div className="flex min-w-0 flex-1 flex-col">
                {agent && (
                    <AgentDetailHeader
                        description={agent.description}
                        id={agent.id}
                        lastPublishedVersion={agent.lastPublishedVersion}
                        leading={
                            <div className="flex items-center gap-2">
                                <LeftSidebarButton
                                    onLeftSidebarOpenClick={() => setProjectLeftSidebarOpen(!projectLeftSidebarOpen)}
                                />

                                {project && (
                                    <ProjectBreadcrumb
                                        itemSelect={
                                            <ProjectItemSelect
                                                currentAgentId={agent.id}
                                                currentLabel={agent.title}
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
                        onAskCopilot={
                            copilotEnabled
                                ? () =>
                                      openCopilot({
                                          mode: MODE.ASK,
                                          parameters: {aiAgentId: agent.id},
                                          source: Source.AI_AGENT,
                                      })
                                : undefined
                        }
                        onToggleTestPanel={handleToggleTestPanel}
                        project={project}
                        projectId={agent.projectId}
                        testPanelOpen={testPanelVisible}
                        title={agent.title}
                    />
                )}

                <div className="flex min-h-0 flex-1" ref={contentRowRef}>
                    <div className="min-w-[420px] flex-1 overflow-y-auto">
                        {agentId && <AgentDetailContent agentId={agentId} />}
                    </div>

                    {agent && testPanelVisible && (
                        <div
                            className="m-3 shrink-0 overflow-hidden rounded-lg border border-stroke-neutral-secondary bg-background"
                            style={{width: testPanelWidth}}
                        >
                            <AgentTestChatPanel key={agent.id} workflowId={agent.draftWorkflowId} />
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default ProjectAgent;
