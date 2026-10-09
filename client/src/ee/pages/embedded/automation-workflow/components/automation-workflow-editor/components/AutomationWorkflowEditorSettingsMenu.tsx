import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {
    CopyIcon,
    DownloadIcon,
    EditIcon,
    HistoryIcon,
    PlusIcon,
    SettingsIcon,
    Trash2Icon,
    UploadIcon,
} from 'lucide-react';
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
    onImportWorkflowClick: () => void;
    onNewWorkflowClick: () => void;
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
    onImportWorkflowClick,
    onNewWorkflowClick,
    onProjectHistoryClick,
}: AutomationWorkflowEditorSettingsMenuProps) => {
    const [open, setOpen] = useState(false);

    return (
        <DropdownMenu onOpenChange={setOpen} open={open}>
            <Tooltip>
                <DropdownMenuTrigger
                    asChild
                    className="cursor-pointer data-[state=open]:bg-surface-brand-secondary data-[state=open]:text-content-brand-primary"
                >
                    <TooltipTrigger asChild>
                        <Button aria-label="Settings" icon={<SettingsIcon />} size="icon" variant="ghost" />
                    </TooltipTrigger>
                </DropdownMenuTrigger>

                <TooltipContent>Project and workflow settings</TooltipContent>
            </Tooltip>

            <DropdownMenuContent align="end">
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
                            icon={<EditIcon />}
                            label="Edit"
                            onClick={() => {
                                setOpen(false);
                                onEditWorkflowClick();
                            }}
                        />

                        <DropdownMenuItem
                            icon={<CopyIcon />}
                            label="Duplicate"
                            onClick={() => {
                                setOpen(false);
                                onDuplicateWorkflowClick();
                            }}
                        />

                        <DropdownMenuItem
                            icon={<DownloadIcon />}
                            label="Export"
                            onClick={() => {
                                setOpen(false);
                                onExportWorkflowClick();
                            }}
                        />

                        <DropdownMenuSeparator />

                        <DropdownMenuItem
                            icon={<Trash2Icon />}
                            label="Delete"
                            onClick={() => {
                                setOpen(false);
                                onDeleteWorkflowClick();
                            }}
                            variant="destructive"
                        />
                    </TabsContent>

                    <TabsContent className="mt-0" value="project">
                        <DropdownMenuItem
                            icon={<EditIcon />}
                            label="Edit"
                            onClick={() => {
                                setOpen(false);
                                onEditProjectClick();
                            }}
                        />

                        <DropdownMenuItem
                            icon={<CopyIcon />}
                            label="Duplicate"
                            onClick={() => {
                                setOpen(false);
                                onDuplicateProjectClick();
                            }}
                        />

                        <DropdownMenuItem
                            icon={<DownloadIcon />}
                            label="Export"
                            onClick={() => {
                                setOpen(false);
                                onExportProjectClick();
                            }}
                        />

                        <DropdownMenuSeparator />

                        <DropdownMenuItem
                            icon={<PlusIcon />}
                            label="New Workflow"
                            onClick={() => {
                                setOpen(false);
                                onNewWorkflowClick();
                            }}
                        />

                        <DropdownMenuItem
                            icon={<UploadIcon />}
                            label="Import Workflow"
                            onClick={() => {
                                setOpen(false);
                                onImportWorkflowClick();
                            }}
                        />

                        <DropdownMenuSeparator />

                        <DropdownMenuItem
                            icon={<HistoryIcon />}
                            label="Project History"
                            onClick={() => {
                                setOpen(false);
                                onProjectHistoryClick();
                            }}
                        />

                        <DropdownMenuSeparator />

                        <DropdownMenuItem
                            icon={<Trash2Icon />}
                            label="Delete"
                            onClick={() => {
                                setOpen(false);
                                onDeleteProjectClick();
                            }}
                            variant="destructive"
                        />
                    </TabsContent>
                </Tabs>
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default AutomationWorkflowEditorSettingsMenu;
