import Button from '@/components/Button/Button';
import {ButtonGroup} from '@/components/ui/button-group';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import AgentDialog from '@/pages/automation/agents/components/AgentDialog';
import useImportAiAgent from '@/pages/automation/agents/hooks/useImportAiAgent';
import {Project} from '@/shared/middleware/automation/configuration';
import {ChevronDownIcon, LoaderCircleIcon, UploadIcon} from 'lucide-react';
import {useState} from 'react';

interface ProjectAgentCreationActionsProps {
    placement: 'emptyState' | 'tabRow';
    project: Project;
}

const ProjectAgentCreationActions = ({placement, project}: ProjectAgentCreationActionsProps) => {
    const [showAgentDialog, setShowAgentDialog] = useState(false);

    const {fileInputRef, handleImportFileChange, isImporting, triggerImport} = useImportAiAgent({
        projectId: project.id!,
        workspaceId: project.workspaceId,
    });

    const compact = placement === 'tabRow';

    return (
        <>
            <ButtonGroup aria-label="Agent Creation Actions" className={compact ? undefined : 'mx-auto'}>
                <Button
                    aria-label="Create Agent"
                    onClick={(event) => {
                        event.stopPropagation();

                        setShowAgentDialog(true);
                    }}
                    size={compact ? 'sm' : undefined}
                    variant={compact ? 'outline' : 'default'}
                >
                    {compact ? 'New Agent' : 'Create Agent'}
                </Button>

                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <Button
                            aria-label="More Agent Creation Actions"
                            icon={isImporting ? <LoaderCircleIcon className="animate-spin" /> : <ChevronDownIcon />}
                            size={compact ? 'sm' : undefined}
                            variant={compact ? 'outline' : 'default'}
                        >
                            <> </>
                        </Button>
                    </DropdownMenuTrigger>

                    <DropdownMenuContent align="end" className="p-0">
                        <DropdownMenuItem
                            aria-label="Import Agent"
                            className="dropdown-menu-item"
                            disabled={isImporting}
                            onClick={(event) => {
                                event.stopPropagation();

                                triggerImport();
                            }}
                        >
                            <UploadIcon /> Import Agent
                        </DropdownMenuItem>
                    </DropdownMenuContent>
                </DropdownMenu>
            </ButtonGroup>

            {showAgentDialog && (
                <AgentDialog onOpenChange={setShowAgentDialog} open={showAgentDialog} projectId={project.id!} />
            )}

            <input accept=".json" className="hidden" onChange={handleImportFileChange} ref={fileInputRef} type="file" />
        </>
    );
};

export default ProjectAgentCreationActions;
export type {ProjectAgentCreationActionsProps};
