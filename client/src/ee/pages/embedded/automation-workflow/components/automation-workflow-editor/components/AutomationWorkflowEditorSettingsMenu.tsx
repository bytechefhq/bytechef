import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {CopyIcon, DownloadIcon, EditIcon, HistoryIcon, SettingsIcon, Trash2Icon} from 'lucide-react';
import {useState} from 'react';

interface AutomationWorkflowEditorSettingsMenuProps {
    onDeleteProjectClick: () => void;
    onDeleteWorkflowClick: () => void;
    onDuplicateProjectClick: () => void;
    onDuplicateWorkflowClick: () => void;
    onEditProjectClick: () => void;
    onEditWorkflowClick: () => void;
    onExportProjectClick: () => void;
    onExportWorkflowClick: () => void;
    onProjectHistoryClick: () => void;
}

const AutomationWorkflowEditorSettingsMenu = ({
    onDeleteProjectClick,
    onDeleteWorkflowClick,
    onDuplicateProjectClick,
    onDuplicateWorkflowClick,
    onEditProjectClick,
    onEditWorkflowClick,
    onExportProjectClick,
    onExportWorkflowClick,
    onProjectHistoryClick,
}: AutomationWorkflowEditorSettingsMenuProps) => {
    const [open, setOpen] = useState(false);

    return (
        <DropdownMenu onOpenChange={setOpen} open={open}>
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
                        <DropdownMenuItem
                            className="dropdown-menu-item"
                            onClick={() => {
                                setOpen(false);
                                onEditWorkflowClick();
                            }}
                        >
                            <EditIcon className="mr-2 size-4" />
                            Edit
                        </DropdownMenuItem>

                        <DropdownMenuItem
                            className="dropdown-menu-item"
                            onClick={() => {
                                setOpen(false);
                                onDuplicateWorkflowClick();
                            }}
                        >
                            <CopyIcon className="mr-2 size-4" />
                            Duplicate
                        </DropdownMenuItem>

                        <DropdownMenuItem
                            className="dropdown-menu-item"
                            onClick={() => {
                                setOpen(false);
                                onExportWorkflowClick();
                            }}
                        >
                            <DownloadIcon className="mr-2 size-4" />
                            Export
                        </DropdownMenuItem>

                        <DropdownMenuSeparator />

                        <DropdownMenuItem
                            className="dropdown-menu-item-destructive"
                            onClick={() => {
                                setOpen(false);
                                onDeleteWorkflowClick();
                            }}
                        >
                            <Trash2Icon className="mr-2 size-4" />
                            Delete
                        </DropdownMenuItem>
                    </TabsContent>

                    <TabsContent className="mt-0" value="project">
                        <DropdownMenuItem
                            className="dropdown-menu-item"
                            onClick={() => {
                                setOpen(false);
                                onEditProjectClick();
                            }}
                        >
                            <EditIcon className="mr-2 size-4" />
                            Edit
                        </DropdownMenuItem>

                        <DropdownMenuItem
                            className="dropdown-menu-item"
                            onClick={() => {
                                setOpen(false);
                                onDuplicateProjectClick();
                            }}
                        >
                            <CopyIcon className="mr-2 size-4" />
                            Duplicate
                        </DropdownMenuItem>

                        <DropdownMenuItem
                            className="dropdown-menu-item"
                            onClick={() => {
                                setOpen(false);
                                onExportProjectClick();
                            }}
                        >
                            <DownloadIcon className="mr-2 size-4" />
                            Export
                        </DropdownMenuItem>

                        <DropdownMenuItem
                            className="dropdown-menu-item"
                            onClick={() => {
                                setOpen(false);
                                onProjectHistoryClick();
                            }}
                        >
                            <HistoryIcon className="mr-2 size-4" />
                            Project History
                        </DropdownMenuItem>

                        <DropdownMenuSeparator />

                        <DropdownMenuItem
                            className="dropdown-menu-item-destructive"
                            onClick={() => {
                                setOpen(false);
                                onDeleteProjectClick();
                            }}
                        >
                            <Trash2Icon className="mr-2 size-4" />
                            Delete
                        </DropdownMenuItem>
                    </TabsContent>
                </Tabs>
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default AutomationWorkflowEditorSettingsMenu;
