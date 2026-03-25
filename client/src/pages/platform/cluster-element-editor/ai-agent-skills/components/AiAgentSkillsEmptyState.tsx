import Button from '@/components/Button/Button';
import AiAgentSkillsCreateDropdown from '@/pages/platform/cluster-element-editor/ai-agent-skills/components/AiAgentSkillsCreateDropdown';
import {BookOpenIcon, BrainCircuitIcon, RepeatIcon, ZapIcon} from 'lucide-react';

const AiAgentSkillsEmptyState = () => {
    return (
        <div className="flex flex-1 flex-col items-center justify-center px-8 py-12">
            <div className="flex flex-col items-center gap-3">
                <div className="flex size-12 items-center justify-center rounded-full bg-amber-100">
                    <ZapIcon className="size-6 text-amber-600" />
                </div>

                <h2 className="text-xl font-semibold">Skills</h2>

                <p className="max-w-md text-center text-sm text-gray-500">
                    Teach your agents what they need to know. Skills package expert instructions, coding guidelines, and
                    process documentation into portable modules any agent can use.
                </p>
            </div>

            <div className="mb-8 mt-20 grid max-w-2xl grid-cols-3 gap-6">
                <div className="flex flex-col items-center gap-2 text-center">
                    <BrainCircuitIcon className="size-5 text-gray-400" />

                    <h3 className="text-sm font-medium">Domain expertise</h3>

                    <p className="text-xs text-gray-500">
                        Capture specialized knowledge your agents can apply on demand.
                    </p>
                </div>

                <div className="flex flex-col items-center gap-2 text-center">
                    <RepeatIcon className="size-5 text-gray-400" />

                    <h3 className="text-sm font-medium">Plug and play</h3>

                    <p className="text-xs text-gray-500">Share a single skill across multiple agents with one click.</p>
                </div>

                <div className="flex flex-col items-center gap-2 text-center">
                    <BookOpenIcon className="size-5 text-gray-400" />

                    <h3 className="text-sm font-medium">AI-powered creation</h3>

                    <p className="text-xs text-gray-500">
                        Draft skills with AI, write your own, or upload existing files.
                    </p>
                </div>
            </div>

            <div className="flex items-center gap-3">
                <Button variant="outline">Read more</Button>

                <AiAgentSkillsCreateDropdown />
            </div>
        </div>
    );
};

export default AiAgentSkillsEmptyState;
