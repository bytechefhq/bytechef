import {Input} from '@/components/ui/input';
import AiAgentSkillListItem from '@/pages/platform/cluster-element-editor/ai-agent-skills/components/AiAgentSkillListItem';
import AiAgentSkillsCreateDropdown from '@/pages/platform/cluster-element-editor/ai-agent-skills/components/AiAgentSkillsCreateDropdown';
import useAgentSkillsList from '@/pages/platform/cluster-element-editor/ai-agent-skills/hooks/useAgentSkillsList';
import {AgentSkill} from '@/shared/middleware/graphql';
import {SearchIcon} from 'lucide-react';

interface AiAgentSkillsListProps {
    skills: AgentSkill[];
}

const AiAgentSkillsList = ({skills}: AiAgentSkillsListProps) => {
    const {deleteSkill, filteredSkills, handleDownloadSkill, renameSkill, searchQuery, setSearchQuery} =
        useAgentSkillsList(skills);

    return (
        <div className="flex flex-1 flex-col py-4">
            <div className="mb-4 flex items-center gap-2">
                <div className="relative flex-1">
                    <SearchIcon className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-gray-400" />

                    <Input
                        className="pl-9"
                        onChange={(event) => setSearchQuery(event.target.value)}
                        placeholder="Search skills..."
                        value={searchQuery}
                    />
                </div>

                <AiAgentSkillsCreateDropdown />
            </div>

            <div className="flex-1 divide-y divide-border/50">
                {filteredSkills.length > 0 ? (
                    filteredSkills.map((skill) => (
                        <AiAgentSkillListItem
                            deleteSkill={deleteSkill}
                            key={skill.id}
                            onDownload={handleDownloadSkill}
                            onRename={renameSkill}
                            skill={skill}
                        />
                    ))
                ) : (
                    <div className="flex items-center justify-center py-12 text-sm text-gray-500">
                        No skills found matching your search.
                    </div>
                )}
            </div>
        </div>
    );
};

export default AiAgentSkillsList;
