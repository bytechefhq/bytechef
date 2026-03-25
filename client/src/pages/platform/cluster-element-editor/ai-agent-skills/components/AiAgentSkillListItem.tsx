import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import AiAgentSkillDeleteAlertDialog from '@/pages/platform/cluster-element-editor/ai-agent-skills/components/AiAgentSkillDeleteAlertDialog';
import AiAgentSkillRenameDialog from '@/pages/platform/cluster-element-editor/ai-agent-skills/components/AiAgentSkillRenameDialog';
import useAgentSkillListItem from '@/pages/platform/cluster-element-editor/ai-agent-skills/hooks/useAgentSkillListItem';
import {AgentSkill} from '@/shared/middleware/graphql';
import {DownloadIcon, EllipsisVerticalIcon, PencilIcon, TrashIcon, ZapIcon} from 'lucide-react';

interface AiAgentSkillListItemProps {
    deleteSkill: (id: string) => Promise<void>;
    onDownload: (id: string, name: string) => void;
    onRename: (id: string, newName: string, description?: string | null) => void;
    skill: AgentSkill;
}

const AiAgentSkillListItem = ({deleteSkill, onDownload, onRename, skill}: AiAgentSkillListItemProps) => {
    const {
        handleClick,
        handleDeleteClick,
        handleDownloadClick,
        handleRenameClick,
        setShowDeleteDialog,
        setShowRenameDialog,
        showDeleteDialog,
        showRenameDialog,
        skillColor,
    } = useAgentSkillListItem({deleteSkill, onDownload, onRename, skill});

    return (
        <>
            <div
                className="flex cursor-pointer items-center justify-between rounded-md px-2 py-4 hover:bg-gray-50"
                onClick={handleClick}
            >
                <div className="flex flex-1 items-center gap-3">
                    <div className={`flex size-8 items-center justify-center rounded ${skillColor}`}>
                        <ZapIcon className="size-4 text-white" />
                    </div>

                    <div className="flex-1">
                        <div className="text-sm font-semibold">{skill.name}</div>

                        {skill.description && (
                            <div className="line-clamp-1 text-xs text-gray-500">{skill.description}</div>
                        )}
                    </div>
                </div>

                <div className="flex items-center gap-4">
                    {skill.lastModifiedDate && (
                        <Tooltip>
                            <TooltipTrigger className="text-xs text-gray-500">
                                {`Modified ${new Date(skill.lastModifiedDate).toLocaleDateString()}`}
                            </TooltipTrigger>

                            <TooltipContent>
                                {`Last modified ${new Date(skill.lastModifiedDate).toLocaleDateString()} ${new Date(skill.lastModifiedDate).toLocaleTimeString()}`}
                            </TooltipContent>
                        </Tooltip>
                    )}

                    <DropdownMenu>
                        <DropdownMenuTrigger asChild onClick={(event) => event.stopPropagation()}>
                            <Button
                                icon={<EllipsisVerticalIcon className="size-4 hover:cursor-pointer" />}
                                size="icon"
                                variant="ghost"
                            />
                        </DropdownMenuTrigger>

                        <DropdownMenuContent align="end" onClick={(event) => event.stopPropagation()}>
                            <DropdownMenuItem onClick={handleDownloadClick}>
                                <DownloadIcon className="mr-2 size-4" />
                                Download
                            </DropdownMenuItem>

                            <DropdownMenuItem onClick={() => setShowRenameDialog(true)}>
                                <PencilIcon className="mr-2 size-4" />
                                Rename
                            </DropdownMenuItem>

                            <DropdownMenuSeparator />

                            <DropdownMenuItem className="text-red-600" onClick={() => setShowDeleteDialog(true)}>
                                <TrashIcon className="mr-2 size-4" />
                                Delete
                            </DropdownMenuItem>
                        </DropdownMenuContent>
                    </DropdownMenu>
                </div>
            </div>

            {showDeleteDialog && (
                <AiAgentSkillDeleteAlertDialog
                    onClose={() => setShowDeleteDialog(false)}
                    onDelete={handleDeleteClick}
                />
            )}

            {showRenameDialog && (
                <AiAgentSkillRenameDialog
                    currentName={skill.name}
                    onClose={() => setShowRenameDialog(false)}
                    onRename={handleRenameClick}
                />
            )}
        </>
    );
};

export default AiAgentSkillListItem;
