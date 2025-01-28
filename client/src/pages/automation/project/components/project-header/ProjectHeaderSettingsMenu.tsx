import {Button} from '@/components/ui/button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import ProjectHeaderProjectTabButtons from '@/pages/automation/project/components/project-header/ProjectHeaderProjectTabButtons';
import ProjectHeaderWorkflowTabButtons from '@/pages/automation/project/components/project-header/ProjectHeaderWorkflowTabButtons';
import {Project} from '@/shared/middleware/automation/configuration';
import {SettingsIcon} from 'lucide-react';

interface ProjectHeaderSettingsMenuProps {
    project: Project;
    setShowDeleteProjectAlertDialog: (value: boolean) => void;
    setShowEditProjectDialog: (value: boolean) => void;
    setShowDeleteWorkflowAlertDialog: (value: boolean) => void;
    workflowId: string;
}

const ProjectHeaderSettingsMenu = ({
    project,
    setShowDeleteProjectAlertDialog,
    setShowDeleteWorkflowAlertDialog,
    setShowEditProjectDialog,
    workflowId,
}: ProjectHeaderSettingsMenuProps) => {
    const handleTabClick = (event: React.MouseEvent) => {
        event.stopPropagation();
    };

    return (
        <Tabs defaultValue="workflow">
            <DropdownMenu>
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
                    </Button>
                </DropdownMenuTrigger>

                <DropdownMenuContent className="p-0">
                    <DropdownMenuItem className="p-0">
                        <TabsList className="rounded-none" onClick={handleTabClick}>
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
                    </DropdownMenuItem>

                    <TabsContent className="mt-0" value="workflow">
                        <ProjectHeaderWorkflowTabButtons
                            onShowDeleteWorkflowAlertDialog={() => setShowDeleteWorkflowAlertDialog(true)}
                            project={project}
                            workflowId={workflowId}
                        />
                    </TabsContent>

                    <TabsContent className="mt-0" value="project">
                        <ProjectHeaderProjectTabButtons
                            onDelete={() => setShowDeleteProjectAlertDialog(true)}
                            onEdit={() => setShowEditProjectDialog(true)}
                            project={project}
                        />
                    </TabsContent>
                </DropdownMenuContent>
            </DropdownMenu>
        </Tabs>
    );
};

export default ProjectHeaderSettingsMenu;
