import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import AiSkillDeleteAlertDialog from '@/pages/automation/ai/skills/components/AiSkillDeleteAlertDialog';
import AiSkillEditDialog from '@/pages/automation/ai/skills/components/AiSkillEditDialog';
import useAiSkillListItem from '@/pages/automation/ai/skills/hooks/useAiSkillListItem';
import {AiSkill} from '@/shared/middleware/graphql';
import {DownloadIcon, EllipsisVerticalIcon, PencilIcon, TrashIcon, ZapIcon} from 'lucide-react';

interface AiSkillListItemProps {
    deleteSkill: (id: string) => Promise<void>;
    onDownload: (id: string, name: string) => void;
    onUpdate: (id: string, name: string, description: string | null) => void;
    skill: AiSkill;
}

const AiSkillListItem = ({deleteSkill, onDownload, onUpdate, skill}: AiSkillListItemProps) => {
    const {
        handleClick,
        handleDeleteClick,
        handleDownloadClick,
        handleEditSave,
        setShowDeleteDialog,
        setShowEditDialog,
        showDeleteDialog,
        showEditDialog,
        skillColor,
    } = useAiSkillListItem({deleteSkill, onDownload, onUpdate, skill});

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
                            <div className="line-clamp-1 text-xs text-content-neutral-secondary">
                                {skill.description}
                            </div>
                        )}
                    </div>
                </div>

                <div className="flex items-center gap-4">
                    {skill.lastModifiedDate && (
                        <Tooltip>
                            <TooltipTrigger className="text-xs text-content-neutral-secondary">
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

                            <DropdownMenuItem onClick={() => setShowEditDialog(true)}>
                                <PencilIcon className="mr-2 size-4" />
                                Edit
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
                <AiSkillDeleteAlertDialog onClose={() => setShowDeleteDialog(false)} onDelete={handleDeleteClick} />
            )}

            {showEditDialog && (
                <AiSkillEditDialog
                    currentDescription={skill.description}
                    currentName={skill.name}
                    onClose={() => setShowEditDialog(false)}
                    onSave={handleEditSave}
                />
            )}
        </>
    );
};

export default AiSkillListItem;
