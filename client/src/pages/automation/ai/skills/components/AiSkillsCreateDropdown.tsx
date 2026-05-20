import Button from '@/components/Button/Button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {ChevronDownIcon, PencilIcon, UploadIcon} from 'lucide-react';
import {useNavigate} from 'react-router-dom';

const CREATE_PATHS = {
    uploadForm: '/automation/ai/skills/create/upload',
    writeForm: '/automation/ai/skills/create/write',
} as const;

const AiSkillsCreateDropdown = () => {
    const navigate = useNavigate();

    const openCreateRoute = (view: keyof typeof CREATE_PATHS) => {
        navigate(CREATE_PATHS[view]);
    };

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button>
                    Create Skill
                    <ChevronDownIcon className="ml-1 size-4" />
                </Button>
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end" className="w-72">
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
