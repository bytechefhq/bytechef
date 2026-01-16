import Button from '@/components/Button/Button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ProjectGitConfigurationDialog from '@/pages/automation/project/components/ProjectGitConfigurationDialog';
import {ProjectShareDialog} from '@/pages/automation/project/components/ProjectShareDialog';
import ProjectVersionHistorySheet from '@/pages/automation/project/components/ProjectVersionHistorySheet';
import {WorkflowShareDialog} from '@/pages/automation/project/components/WorkflowShareDialog';
import DeleteProjectAlertDialog from '@/pages/automation/project/components/project-header/components/settings-menu/components/DeleteProjectAlertDialog';
import ProjectTabButtons from '@/pages/automation/project/components/project-header/components/settings-menu/components/ProjectTabButtons/ProjectTabButtons';
import WorkflowTabButtons from '@/pages/automation/project/components/project-header/components/settings-menu/components/WorkflowTabButtons';
import {useSettingsMenu} from '@/pages/automation/project/components/project-header/components/settings-menu/hooks/useSettingsMenu';
import ProjectDialog from '@/pages/automation/projects/components/ProjectDialog';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import DeleteWorkflowAlertDialog from '@/shared/components/DeleteWorkflowAlertDialog';
import WorkflowDialog from '@/shared/components/workflow/WorkflowDialog';
import {Project, Workflow} from '@/shared/middleware/automation/configuration';
import {useGetWorkflowQuery} from '@/shared/queries/automation/workflows.queries';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {SettingsIcon} from 'lucide-react';
import {useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

interface ProjectHeaderSettingsMenuProps {
    project: Project;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    workflow: Workflow;
}

const SettingsMenu = ({project, updateWorkflowMutation, workflow}: ProjectHeaderSettingsMenuProps) => {
    const [openDropdownMenu, setOpenDropdownMenu] = useState(false);
    const [showDeleteProjectAlertDialog, setShowDeleteProjectAlertDialog] = useState(false);
    const [showDeleteWorkflowAlertDialog, setShowDeleteWorkflowAlertDialog] = useState(false);
    const [showEditProjectDialog, setShowEditProjectDialog] = useState(false);
    const [showProjectGitConfigurationDialog, setShowProjectGitConfigurationDialog] = useState(false);
    const [showProjectShareDialog, setShowProjectShareDialog] = useState(false);
    const [showProjectVersionHistorySheet, setShowProjectVersionHistorySheet] = useState(false);
    const [showWorkflowShareDialog, setShowWorkflowShareDialog] = useState(false);

    const {setShowEditWorkflowDialog, showEditWorkflowDialog} = useWorkflowEditorStore(
        useShallow((state) => ({
            setShowEditWorkflowDialog: state.setShowEditWorkflowDialog,
            showEditWorkflowDialog: state.showEditWorkflowDialog,
        }))
    );

    const {
        handleDeleteProjectAlertDialogClick,
        handleDeleteWorkflowAlertDialogClick,
        handleDuplicateProjectClick,
        handleDuplicateWorkflowClick,
        handlePullProjectFromGitClick,
        handleUpdateProjectGitConfigurationSubmit,
        projectGitConfiguration,
        projectVersions,
    } = useSettingsMenu({project, workflow});

    return (
        <>
            <DropdownMenu onOpenChange={setOpenDropdownMenu} open={openDropdownMenu}>
                <Tooltip>
                    <DropdownMenuTrigger
                        asChild
                        className="cursor-pointer [&[data-state=open]]:bg-surface-brand-secondary [&[data-state=open]]:text-content-brand-primary"
                    >
                        <TooltipTrigger asChild>
                            <Button aria-label="Settings" icon={<SettingsIcon />} size="icon" variant="ghost" />
                        </TooltipTrigger>
                    </DropdownMenuTrigger>

                    <TooltipContent>Project and workflow settings</TooltipContent>
                </Tooltip>

                <DropdownMenuContent className="p-0">
                    <Tabs aria-label="Settings menu" defaultValue="workflow">
                        <TabsList className="rounded-none">
                            <TabsTrigger
                                aria-label="Workflow tab"
                                className="w-1/2 px-9 py-1 data-[state=active]:shadow-none"
                                value="workflow"
                            >
                                Workflow
                            </TabsTrigger>

                            <TabsTrigger
                                aria-label="Project tab"
                                className="w-1/2 px-9 py-1 data-[state=active]:shadow-none"
                                value="project"
                            >
                                Project
                            </TabsTrigger>
                        </TabsList>

                        <TabsContent className="mt-0" value="workflow">
                            <WorkflowTabButtons
                                onCloseDropdownMenu={() => setOpenDropdownMenu(false)}
                                onDuplicateWorkflow={handleDuplicateWorkflowClick}
                                onShareWorkflow={() => setShowWorkflowShareDialog(true)}
                                onShowDeleteWorkflowAlertDialog={() => setShowDeleteWorkflowAlertDialog(true)}
                                onShowEditWorkflowDialog={() => setShowEditWorkflowDialog(true)}
                                workflowId={workflow.id!}
                            />
                        </TabsContent>

                        <TabsContent className="mt-0" value="project">
                            <ProjectTabButtons
                                onCloseDropdownMenuClick={() => setOpenDropdownMenu(false)}
                                onDeleteProjectClick={() => setShowDeleteProjectAlertDialog(true)}
                                onDuplicateProjectClick={handleDuplicateProjectClick}
                                onPullProjectFromGitClick={handlePullProjectFromGitClick}
                                onShareProject={() => setShowProjectShareDialog(true)}
                                onShowEditProjectDialogClick={() => setShowEditProjectDialog(true)}
                                onShowProjectGitConfigurationDialog={() => setShowProjectGitConfigurationDialog(true)}
                                onShowProjectVersionHistorySheet={() => setShowProjectVersionHistorySheet(true)}
                                projectGitConfigurationEnabled={projectGitConfiguration?.enabled ?? false}
                                projectId={project.id!}
                            />
                        </TabsContent>
                    </Tabs>
                </DropdownMenuContent>
            </DropdownMenu>

            {showDeleteProjectAlertDialog && (
                <DeleteProjectAlertDialog
                    onClose={() => setShowDeleteProjectAlertDialog(false)}
                    onDelete={handleDeleteProjectAlertDialogClick}
                />
            )}

            {showDeleteWorkflowAlertDialog && (
                <DeleteWorkflowAlertDialog
                    onClose={() => setShowDeleteWorkflowAlertDialog(false)}
                    onDelete={() => {
                        handleDeleteWorkflowAlertDialogClick();

                        setShowDeleteWorkflowAlertDialog(false);
                    }}
                />
            )}

            {showEditProjectDialog && (
                <ProjectDialog onClose={() => setShowEditProjectDialog(false)} project={project} />
            )}

            {showEditWorkflowDialog && (
                <WorkflowDialog
                    onClose={() => setShowEditWorkflowDialog(false)}
                    projectId={project.id}
                    updateWorkflowMutation={updateWorkflowMutation}
                    useGetWorkflowQuery={useGetWorkflowQuery}
                    workflowId={workflow.id!}
                />
            )}

            {showProjectGitConfigurationDialog && (
                <ProjectGitConfigurationDialog
                    onClose={() => setShowProjectGitConfigurationDialog(false)}
                    onUpdateProjectGitConfigurationSubmit={handleUpdateProjectGitConfigurationSubmit}
                    projectGitConfiguration={projectGitConfiguration}
                    projectId={project.id!}
                />
            )}

            {showProjectShareDialog && (
                <ProjectShareDialog
                    onOpenChange={() => setShowProjectShareDialog(false)}
                    open={showProjectShareDialog}
                    projectId={project.id!}
                    projectUuid={project.uuid!}
                    projectVersion={project.lastProjectVersion!}
                />
            )}

            {showProjectVersionHistorySheet && projectVersions && (
                <ProjectVersionHistorySheet
                    onSheetOpenChange={setShowProjectVersionHistorySheet}
                    projectVersions={projectVersions}
                    sheetOpen={showProjectVersionHistorySheet}
                />
            )}

            {showWorkflowShareDialog && (
                <WorkflowShareDialog
                    onOpenChange={() => setShowWorkflowShareDialog(false)}
                    open={showWorkflowShareDialog}
                    projectVersion={project.lastProjectVersion!}
                    workflowId={workflow.id!}
                    workflowUuid={workflow.workflowUuid!}
                />
            )}
        </>
    );
};

export default SettingsMenu;
