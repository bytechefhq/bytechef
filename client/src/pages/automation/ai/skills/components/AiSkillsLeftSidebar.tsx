import {Input} from '@/components/Input/Input';
import PageLoader from '@/components/PageLoader';
import useAiSkillsLeftSidebar from '@/pages/automation/ai/skills/hooks/useAiSkillsLeftSidebar';
import getSkillColor from '@/pages/automation/ai/skills/utils/getSkillColor';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {ZapIcon} from 'lucide-react';

interface AiSkillsLeftSidebarProps {
    currentId?: string;
}

const AiSkillsLeftSidebar = ({currentId}: AiSkillsLeftSidebarProps) => {
    const {error, filteredSkills, handleSearchChange, isLoading, search} = useAiSkillsLeftSidebar();

    return (
        <div className="flex h-full flex-col">
            <div className="space-y-2 px-3 pt-0.5 pb-3">
                <Input
                    onChange={(event) => handleSearchChange(event.target.value)}
                    placeholder="Search skills..."
                    value={search}
                />
            </div>

            <div className="flex-1 overflow-y-auto">
                <PageLoader errors={[error]} loading={isLoading}>
                    <LeftSidebarNav
                        body={
                            <>
                                {filteredSkills.map((skill) => {
                                    const active = currentId != null ? skill.id === currentId : false;

                                    return (
                                        <LeftSidebarNavItem
                                            icon={
                                                <div
                                                    className={`flex size-5 shrink-0 items-center justify-center rounded ${getSkillColor(skill.id)}`}
                                                >
                                                    <ZapIcon className="size-3 text-white" />
                                                </div>
                                            }
                                            item={{current: active, name: skill.name}}
                                            key={skill.id}
                                            toLink={`/automation/ai/skills/${skill.id}`}
                                        />
                                    );
                                })}

                                {filteredSkills.length === 0 && (
                                    <div className="px-2 py-2 text-sm text-muted-foreground">No skills found</div>
                                )}
                            </>
                        }
                    />
                </PageLoader>
            </div>
        </div>
    );
};

export default AiSkillsLeftSidebar;
