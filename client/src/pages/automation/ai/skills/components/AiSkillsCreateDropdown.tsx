import Button from '@/components/Button/Button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {ChevronDownIcon, PencilIcon, SparklesIcon, UploadIcon} from 'lucide-react';
import {ReactNode} from 'react';
import {useNavigate} from 'react-router-dom';

const CREATE_PATHS = {
    uploadForm: '/automation/ai/skills/create/upload',
    writeForm: '/automation/ai/skills/create/write',
} as const;

interface AiSkillsCreateDropdownProps {
    trigger?: ReactNode;
}

const AiSkillsCreateDropdown = ({trigger}: AiSkillsCreateDropdownProps = {}) => {
    const navigate = useNavigate();

    const setCopilotPanelOpen = useCopilotPanelStore((state) => state.setCopilotPanelOpen);
    const setContext = useCopilotStore((state) => state.setContext);

    const ff_4554 = useFeatureFlagsStore()('ff-4554');

    const openCreateRoute = (view: keyof typeof CREATE_PATHS) => {
        navigate(CREATE_PATHS[view]);
    };

    const openCopilot = () => {
        setContext({mode: MODE.BUILD, parameters: {}, source: Source.SKILLS});

        setCopilotPanelOpen(true);
    };

    return (
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
                    <DropdownMenuItem className="flex flex-col items-start gap-0.5 p-3" onClick={openCopilot}>
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
                    onClick={() => openCreateRoute('uploadForm')}
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
                    onClick={() => openCreateRoute('writeForm')}
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
    );
};

export default AiSkillsCreateDropdown;
