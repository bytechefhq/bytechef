import Button from '@/components/Button/Button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {useAiAgentSkillsStore} from '@/pages/platform/cluster-element-editor/ai-agent-skills/stores/useAiAgentSkillsStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {ChevronDownIcon, PencilIcon, SparklesIcon, UploadIcon} from 'lucide-react';

const AiAgentSkillsCreateDropdown = () => {
    const {setSkillsView} = useAiAgentSkillsStore();

    const ff_4554 = useFeatureFlagsStore()('ff-4554');

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button>
                    Create Skill
                    <ChevronDownIcon className="ml-1 size-4" />
                </Button>
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end" className="w-72">
                {ff_4554 && (
                    <DropdownMenuItem
                        className="flex flex-col items-start gap-0.5 p-3"
                        onClick={() => setSkillsView('createWithAi')}
                    >
                        <div className="flex items-center gap-2 font-medium">
                            <SparklesIcon className="size-4" />
                            Create With AI
                        </div>

                        <span className="ml-6 text-xs text-gray-500">Let AI generate a skill for you.</span>
                    </DropdownMenuItem>
                )}

                <DropdownMenuItem
                    className="flex flex-col items-start gap-0.5 p-3"
                    onClick={() => setSkillsView('uploadForm')}
                >
                    <div className="flex items-center gap-2 font-medium">
                        <UploadIcon className="size-4" />
                        Upload Files
                    </div>

                    <span className="ml-6 text-xs text-gray-500">Upload a file to create a skill.</span>
                </DropdownMenuItem>

                <DropdownMenuItem
                    className="flex flex-col items-start gap-0.5 p-3"
                    onClick={() => setSkillsView('writeForm')}
                >
                    <div className="flex items-center gap-2 font-medium">
                        <PencilIcon className="size-4" />
                        Write Skill Instructions
                    </div>

                    <span className="ml-6 text-xs text-gray-500">Enter a name and description for the skill.</span>
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default AiAgentSkillsCreateDropdown;
