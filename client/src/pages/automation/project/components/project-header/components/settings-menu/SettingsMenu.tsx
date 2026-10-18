import Button from '@/components/Button/Button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import LoadWorkflowVersionDialog from '@/pages/automation/project/components/LoadWorkflowVersionDialog';
import {ProjectShareDialog} from '@/pages/automation/project/components/ProjectShareDialog';
import ProjectVersionHistorySheet from '@/pages/automation/project/components/ProjectVersionHistorySheet';
import ProjectVisibilityDialog from '@/pages/automation/project/components/ProjectVisibilityDialog';
import {WorkflowShareDialog} from '@/pages/automation/project/components/WorkflowShareDialog';
import DeleteProjectAlertDialog from '@/pages/automation/project/components/project-header/components/settings-menu/components/DeleteProjectAlertDialog';
import ProjectTabButtons from '@/pages/automation/project/components/project-header/components/settings-menu/components/ProjectTabButtons/ProjectTabButtons';
import WorkflowTabButtons from '@/pages/automation/project/components/project-header/components/settings-menu/components/WorkflowTabButtons';
import {useSettingsMenu} from '@/pages/automation/project/components/project-header/components/settings-menu/hooks/useSettingsMenu';
import {useCreateProjectWorkflow} from '@/pages/automation/project/hooks/useCreateProjectWorkflow';
import {useImportProjectWorkflow} from '@/pages/automation/project/hooks/useImportProjectWorkflow';
import ProjectDialog from '@/pages/automation/projects/components/ProjectDialog';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import DeleteWorkflowAlertDialog from '@/shared/components/DeleteWorkflowAlertDialog';
import WorkflowDialog from '@/shared/components/workflow/WorkflowDialog';
import EEVersion from '@/shared/edition/EEVersion';
import {useIsResourceOwnerOrTenantAdmin} from '@/shared/hooks/useIsResourceOwnerOrTenantAdmin';
import {Project, Workflow} from '@/shared/middleware/automation/configuration';
import {ProjectWorkflowKeys} from '@/shared/queries/automation/projectWorkflows.queries';
import {useGetWorkflowQuery} from '@/shared/queries/automation/workflows.queries';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {useQueryClient} from '@tanstack/react-query';
import {LoaderCircleIcon, SettingsIcon} from 'lucide-react';
import {RefObject, Suspense, lazy, useState} from 'react';
import {PanelImperativeHandle} from 'react-resizable-panels';
import {useNavigate} from 'react-router-dom';
import {useShallow} from 'zustand/react/shallow';

interface ProjectHeaderSettingsMenuProps {
    bottomResizablePanelRef?: RefObject<PanelImperativeHandle | null>;
    project: Project;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    workflow: Workflow;
}

const ProjectGitConfigurationDialog = lazy(
    () => import('@/ee/pages/automation/project/components/ProjectGitConfigurationDialog')
);

const SettingsMenu = ({
    bottomResizablePanelRef,
    project,
    updateWorkflowMutation,
    workflow,
}: ProjectHeaderSettingsMenuProps) => {
    const [openDropdownMenu, setOpenDropdownMenu] = useState(false);
    const [showCreateWorkflowDialog, setShowCreateWorkflowDialog] = useState(false);
    const [showDeleteProjectAlertDialog, setShowDeleteProjectAlertDialog] = useState(false);
    const [showDeleteWorkflowAlertDialog, setShowDeleteWorkflowAlertDialog] = useState(false);
    const [showEditProjectDialog, setShowEditProjectDialog] = useState(false);
    const [showLoadWorkflowVersionDialog, setShowLoadWorkflowVersionDialog] = useState(false);
    const [showProjectGitConfigurationDialog, setShowProjectGitConfigurationDialog] = useState(false);
    const [showProjectShareDialog, setShowProjectShareDialog] = useState(false);
    const [showProjectVersionHistorySheet, setShowProjectVersionHistorySheet] = useState(false);
    const [showProjectVisibilityDialog, setShowProjectVisibilityDialog] = useState(false);
    const [showWorkflowShareDialog, setShowWorkflowShareDialog] = useState(false);

    const {setShowEditWorkflowDialog, showEditWorkflowDialog} = useWorkflowEditorStore(
        useShallow((state) => ({
            setShowEditWorkflowDialog: state.setShowEditWorkflowDialog,
            showEditWorkflowDialog: state.showEditWorkflowDialog,
        }))
    );

    const createProjectWorkflowMutation = useCreateProjectWorkflow({bottomResizablePanelRef, projectId: project.id!});

    const {
        handleN8nWorkflowFileChange,
        handleWorkflowFileChange,
        importN8nWorkflowDisabled,
        isImportingN8nWorkflow,
        n8nWorkflowFileInputRef,
        workflowFileInputRef,
    } = useImportProjectWorkflow(project.id!);

    const canManageProjectVisibility = useIsResourceOwnerOrTenantAdmin(project.createdBy);

    const navigate = useNavigate();
    const queryClient = useQueryClient();

    const {
        deleteProjectVersionPending,
        handleDeleteProjectAlertDialogClick,
        handleDeleteProjectVersion,
        handleDeleteWorkflowAlertDialogClick,
        handleDuplicateProjectClick,
        handleDuplicateWorkflowClick,
        handlePullProjectFromGitClick,
        handleRestoreProjectVersion,
        handleUpdateProjectGitConfigurationSubmit,
        projectGitConfiguration,
        projectVersions,
        restoreProjectVersionPending,
    } = useSettingsMenu({project, workflow});

    return (
        <>
            <DropdownMenu onOpenChange={setOpenDropdownMenu} open={openDropdownMenu}>
                <Tooltip>
                    <DropdownMenuTrigger
                        asChild
                        className="cursor-pointer data-[state=open]:bg-surface-brand-secondary data-[state=open]:text-content-brand-primary"
                    >
                        <TooltipTrigger asChild>
                            <Button
                                aria-label="Settings"
                                icon={
                                    isImportingN8nWorkflow ? (
                                        <LoaderCircleIcon className="animate-spin text-primary" />
                                    ) : (
                                        <SettingsIcon />
                                    )
                                }
                                size="icon"
                                variant="ghost"
                            />
                        </TooltipTrigger>
                    </DropdownMenuTrigger>

                    <TooltipContent>Project and workflow settings</TooltipContent>
                </Tooltip>

                <DropdownMenuContent align="end" className="p-0">
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
                                onShowLoadWorkflowVersionDialog={() => setShowLoadWorkflowVersionDialog(true)}
                                workflowId={workflow.id!}
                            />
                        </TabsContent>

                        <TabsContent className="mt-0" value="project">
                            <ProjectTabButtons
                                canManageVisibility={canManageProjectVisibility}
                                importN8nWorkflowDisabled={importN8nWorkflowDisabled}
                                onCloseDropdownMenuClick={() => setOpenDropdownMenu(false)}
                                onDeleteProjectClick={() => setShowDeleteProjectAlertDialog(true)}
                                onDuplicateProjectClick={handleDuplicateProjectClick}
                                onImportN8nWorkflowClick={() => n8nWorkflowFileInputRef.current?.click()}
                                onImportWorkflowClick={() => workflowFileInputRef.current?.click()}
                                onNewWorkflowClick={() => setShowCreateWorkflowDialog(true)}
                                onNewWorkflowFromTemplateClick={() =>
                                    navigate(`/automation/projects/${project.id}/templates`)
                                }
                                onPullProjectFromGitClick={handlePullProjectFromGitClick}
                                onShareProject={() => setShowProjectShareDialog(true)}
                                onShowEditProjectDialogClick={() => setShowEditProjectDialog(true)}
                                onShowProjectGitConfigurationDialog={() => setShowProjectGitConfigurationDialog(true)}
                                onShowProjectVersionHistorySheet={() => setShowProjectVersionHistorySheet(true)}
                                onShowVisibilityDialog={() => setShowProjectVisibilityDialog(true)}
                                projectGitConfigurationEnabled={projectGitConfiguration?.enabled ?? false}
                                projectId={project.id!}
                            />
                        </TabsContent>
                    </Tabs>
                </DropdownMenuContent>
            </DropdownMenu>

            <input
                accept=".json,.yaml,.yml"
                className="hidden"
                onChange={handleWorkflowFileChange}
                ref={workflowFileInputRef}
                type="file"
            />

            <input
                accept=".json"
                className="hidden"
                onChange={handleN8nWorkflowFileChange}
                ref={n8nWorkflowFileInputRef}
                type="file"
            />

            {showCreateWorkflowDialog && (
                <WorkflowDialog
                    createWorkflowMutation={createProjectWorkflowMutation}
                    onClose={() => setShowCreateWorkflowDialog(false)}
                    parentId={project.id}
                    useGetWorkflowQuery={useGetWorkflowQuery}
                />
            )}

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
                    onSave={() =>
                        queryClient.invalidateQueries({
                            queryKey: ProjectWorkflowKeys.projectWorkflow(project.id!, parseInt(workflow.id!)),
                        })
                    }
                    updateWorkflowMutation={updateWorkflowMutation}
                    useGetWorkflowQuery={useGetWorkflowQuery}
                    workflowId={workflow.id!}
                />
            )}

            {showLoadWorkflowVersionDialog && workflow.workflowUuid && (
                <LoadWorkflowVersionDialog
                    onClose={() => setShowLoadWorkflowVersionDialog(false)}
                    projectId={project.id!}
                    workflowUuid={workflow.workflowUuid}
                />
            )}

            {showProjectGitConfigurationDialog && (
                <EEVersion hidden={true}>
                    <Suspense fallback={null}>
                        <ProjectGitConfigurationDialog
                            onClose={() => setShowProjectGitConfigurationDialog(false)}
                            onUpdateProjectGitConfigurationSubmit={handleUpdateProjectGitConfigurationSubmit}
                            projectGitConfiguration={projectGitConfiguration}
                            projectId={project.id!}
                        />
                    </Suspense>
                </EEVersion>
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
                    deleteProjectVersionPending={deleteProjectVersionPending}
                    onDeleteProjectVersion={handleDeleteProjectVersion}
                    onRestoreProjectVersion={handleRestoreProjectVersion}
                    onSheetOpenChange={setShowProjectVersionHistorySheet}
                    projectId={project.id!}
                    projectVersions={projectVersions}
                    restoreProjectVersionPending={restoreProjectVersionPending}
                    sheetOpen={showProjectVersionHistorySheet}
                />
            )}

            {showProjectVisibilityDialog && (
                <ProjectVisibilityDialog
                    createdBy={project.createdBy}
                    onClose={() => setShowProjectVisibilityDialog(false)}
                    projectId={project.id!}
                    visibility={project.visibility}
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
