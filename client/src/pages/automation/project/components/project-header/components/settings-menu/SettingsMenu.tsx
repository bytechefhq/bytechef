import {Button} from '@/components/ui/button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ProjectGitConfigurationDialog from '@/pages/automation/project/components/ProjectGitConfigurationDialog';
import ProjectVersionHistorySheet from '@/pages/automation/project/components/ProjectVersionHistorySheet';
import DeleteProjectAlertDialog from '@/pages/automation/project/components/project-header/components/settings-menu/components/DeleteProjectAlertDialog';
import DeleteWorkflowAlertDialog from '@/pages/automation/project/components/project-header/components/settings-menu/components/DeleteWorkflowAlertDialog';
import ProjectTabButtons from '@/pages/automation/project/components/project-header/components/settings-menu/components/ProjectTabButtons';
import WorkflowTabButtons from '@/pages/automation/project/components/project-header/components/settings-menu/components/WorkflowTabButtons';
import {useSettingsMenu} from '@/pages/automation/project/components/project-header/components/settings-menu/hooks/useSettingsMenu';
import ProjectDialog from '@/pages/automation/projects/components/ProjectDialog';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import WorkflowDialog from '@/shared/components/workflow/WorkflowDialog';
import {Project, Workflow} from '@/shared/middleware/automation/configuration';
import {useGetWorkflowQuery} from '@/shared/queries/automation/workflows.queries';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {SettingsIcon} from 'lucide-react';
import {ChangeEvent, useState} from 'react';

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
    const [showProjectVersionHistorySheet, setShowProjectVersionHistorySheet] = useState(false);

    const {setShowEditWorkflowDialog, showEditWorkflowDialog} = useWorkflowEditorStore();

    const {
        handleDeleteProjectAlertDialogClick,
        handleDeleteWorkflowAlertDialogClick,
        handleDuplicateProjectClick,
        handleDuplicateWorkflowClick,
        handleImportProjectWorkflowClick,
        handlePullProjectFromGitClick,
        handleUpdateProjectGitConfigurationSubmit,
        hiddenFileInputRef,
        projectGitConfiguration,
        projectVersions,
    } = useSettingsMenu({project, workflow});

    const handleFileChange = async (e: ChangeEvent<HTMLInputElement>) => {
        if (e.target.files) {
            const definition = await e.target.files[0].text();

            handleImportProjectWorkflowClick(definition);
        }
    };

    return (
        <>
            <DropdownMenu onOpenChange={setOpenDropdownMenu} open={openDropdownMenu}>
                <Tooltip>
                    <TooltipTrigger asChild>
                        <DropdownMenuTrigger
                            asChild
                            className="cursor-pointer [&[data-state=open]]:bg-surface-brand-secondary [&[data-state=open]]:text-content-brand-primary-pressed"
                        >
                            <Button
                                className="hover:bg-surface-neutral-primary-hover active:bg-surface-brand-secondary active:text-content-brand-primary-pressed [&_svg]:size-5"
                                size="icon"
                                variant="ghost"
                            >
                                <SettingsIcon />

                                <TooltipContent>Project and workflow settings</TooltipContent>
                            </Button>
                        </DropdownMenuTrigger>
                    </TooltipTrigger>
                </Tooltip>

                <DropdownMenuContent className="p-0">
                    <Tabs defaultValue="workflow">
                        <TabsList className="rounded-none">
                            <TabsTrigger
                                className="w-1/2 rounded-sm px-9 py-1 data-[state=active]:shadow-none"
                                value="workflow"
                            >
                                Workflow
                            </TabsTrigger>

                            <TabsTrigger
                                className="w-1/2 rounded-sm px-9 py-1 data-[state=active]:shadow-none"
                                value="project"
                            >
                                Project
                            </TabsTrigger>
                        </TabsList>

                        <TabsContent className="mt-0" value="workflow">
                            <WorkflowTabButtons
                                onCloseDropdownMenu={() => setOpenDropdownMenu(false)}
                                onDuplicateWorkflow={handleDuplicateWorkflowClick}
                                onShowDeleteWorkflowAlertDialog={() => setShowDeleteWorkflowAlertDialog(true)}
                                onShowEditWorkflowDialog={() => setShowEditWorkflowDialog(true)}
                                workflowId={workflow.id!}
                            />
                        </TabsContent>

                        <TabsContent className="mt-0" value="project">
                            <ProjectTabButtons
                                hiddenFileInputRef={hiddenFileInputRef}
                                onCloseDropdownMenuClick={() => setOpenDropdownMenu(false)}
                                onDeleteProjectClick={() => setShowDeleteProjectAlertDialog(true)}
                                onDuplicateProjectClick={handleDuplicateProjectClick}
                                onPullProjectFromGitClick={handlePullProjectFromGitClick}
                                onShowEditProjectDialogClick={() => setShowEditProjectDialog(true)}
                                onShowProjectGitConfigurationDialog={() => setShowProjectGitConfigurationDialog(true)}
                                onShowProjectVersionHistorySheet={() => setShowProjectVersionHistorySheet(true)}
                                projectGitConfigurationEnabled={projectGitConfiguration?.enabled ?? false}
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
                />
            )}

            {showProjectVersionHistorySheet && projectVersions && (
                <ProjectVersionHistorySheet
                    onSheetOpenChange={setShowProjectVersionHistorySheet}
                    projectVersions={projectVersions}
                    sheetOpen={showProjectVersionHistorySheet}
                />
            )}

            <input className="hidden" onChange={handleFileChange} ref={hiddenFileInputRef} type="file" />
        </>
    );
};

export default SettingsMenu;
