import {Button} from '@/components/ui/button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ProjectHeaderProjectTabButtons from '@/pages/automation/project/components/project-header/ProjectHeaderProjectTabButtons';
import ProjectHeaderWorkflowTabButtons from '@/pages/automation/project/components/project-header/ProjectHeaderWorkflowTabButtons';
import {Project} from '@/shared/middleware/automation/configuration';
import {SettingsIcon} from 'lucide-react';
import {useState} from 'react';

interface ProjectHeaderSettingsMenuProps {
    project: Project;
    setShowDeleteProjectAlertDialog: (value: boolean) => void;
    setShowEditProjectDialog: (value: boolean) => void;
    setShowDeleteWorkflowAlertDialog: (value: boolean) => void;
    setShowProjectVersionHistorySheet: (value: boolean) => void;
    workflowId: string;
}

const ProjectHeaderSettingsMenu = ({
    project,
    setShowDeleteProjectAlertDialog,
    setShowDeleteWorkflowAlertDialog,
    setShowEditProjectDialog,
    setShowProjectVersionHistorySheet,
    workflowId,
}: ProjectHeaderSettingsMenuProps) => {
    const [openDropdownMenu, setOpenDropdownMenu] = useState(false);

    return (
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
                        <ProjectHeaderWorkflowTabButtons
                            handleCloseDropdownMenu={() => setOpenDropdownMenu(false)}
                            handleShowDeleteWorkflowAlertDialog={() => setShowDeleteWorkflowAlertDialog(true)}
                            project={project}
                            workflowId={workflowId}
                        />
                    </TabsContent>

                    <TabsContent className="mt-0" value="project">
                        <ProjectHeaderProjectTabButtons
                            handleCloseDropdownMenu={() => setOpenDropdownMenu(false)}
                            handleDeleteProject={() => setShowDeleteProjectAlertDialog(true)}
                            handleEditProject={() => setShowEditProjectDialog(true)}
                            handleShowProjectVersionHistorySheet={() => setShowProjectVersionHistorySheet(true)}
                            project={project}
                        />
                    </TabsContent>
                </Tabs>
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default ProjectHeaderSettingsMenu;
