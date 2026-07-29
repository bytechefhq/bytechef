import Button from '@/components/Button/Button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import AiSkillGenerateDialog from '@/pages/automation/ai/skills/components/AiSkillGenerateDialog';
import AiSkillUploadDialog from '@/pages/automation/ai/skills/components/AiSkillUploadDialog';
import AiSkillWriteDialog from '@/pages/automation/ai/skills/components/AiSkillWriteDialog';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {ChevronDownIcon, PencilIcon, SparklesIcon, UploadIcon} from 'lucide-react';
import {ReactNode, useState} from 'react';
import {useNavigate} from 'react-router-dom';

interface AiSkillsCreateDropdownProps {
    trigger?: ReactNode;
}

const AiSkillsCreateDropdown = ({trigger}: AiSkillsCreateDropdownProps = {}) => {
    const [showGenerateDialog, setShowGenerateDialog] = useState(false);
    const [showUploadDialog, setShowUploadDialog] = useState(false);
    const [showWriteDialog, setShowWriteDialog] = useState(false);

    const ff_4554 = useFeatureFlagsStore()('ff-4554');

    const navigate = useNavigate();

    const handleCreated = (createdSkillId: string | null) => {
        if (createdSkillId) {
            navigate(`/automation/ai/skills/${createdSkillId}`);
        }
    };

    return (
        <>
            <DropdownMenu>
                <DropdownMenuTrigger asChild>
                    {trigger ?? (
                        <Button>
                            Create Skill
                            <ChevronDownIcon className="ml-1 size-4" />
                        </Button>
                    )}
                </DropdownMenuTrigger>

                <DropdownMenuContent align="end" className="w-72">
                    {ff_4554 && (
                        <DropdownMenuItem
                            className="flex flex-col items-start gap-0.5 p-3"
                            onClick={() => setShowGenerateDialog(true)}
                        >
                            <div className="flex items-center gap-2 font-medium">
                                <SparklesIcon className="size-4" />
                                Create With AI
                            </div>

                            <span className="ml-6 text-xs text-content-neutral-secondary">
                                Let AI generate a skill for you.
                            </span>
                        </DropdownMenuItem>
                    )}

                    <DropdownMenuItem
                        className="flex flex-col items-start gap-0.5 p-3"
                        onClick={() => setShowUploadDialog(true)}
                    >
                        <div className="flex items-center gap-2 font-medium">
                            <UploadIcon className="size-4" />
                            Upload Files
                        </div>

                        <span className="ml-6 text-xs text-content-neutral-secondary">
                            Upload a file to create a skill.
                        </span>
                    </DropdownMenuItem>

                    <DropdownMenuItem
                        className="flex flex-col items-start gap-0.5 p-3"
                        onClick={() => setShowWriteDialog(true)}
                    >
                        <div className="flex items-center gap-2 font-medium">
                            <PencilIcon className="size-4" />
                            Write Skill Instructions
                        </div>

                        <span className="ml-6 text-xs text-content-neutral-secondary">
                            Enter a name and description for the skill.
                        </span>
                    </DropdownMenuItem>
                </DropdownMenuContent>
            </DropdownMenu>

            <AiSkillGenerateDialog
                onCreated={handleCreated}
                onOpenChange={setShowGenerateDialog}
                open={showGenerateDialog}
            />

            <AiSkillUploadDialog onCreated={handleCreated} onOpenChange={setShowUploadDialog} open={showUploadDialog} />

            <AiSkillWriteDialog onCreated={handleCreated} onOpenChange={setShowWriteDialog} open={showWriteDialog} />
        </>
    );
};

export default AiSkillsCreateDropdown;
