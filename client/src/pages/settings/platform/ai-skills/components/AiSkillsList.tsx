import {Input} from '@/components/ui/input';
import AiSkillListItem from '@/shared/components/ai-skills/components/AiSkillListItem';
import AiSkillsCreateDropdown from '@/shared/components/ai-skills/components/AiSkillsCreateDropdown';
import useAiSkillsList from '@/shared/components/ai-skills/hooks/useAiSkillsList';
import {AiSkill} from '@/shared/middleware/graphql';
import {SearchIcon} from 'lucide-react';

interface AiSkillsListProps {
    skills: AiSkill[];
}

const AiSkillsList = ({skills}: AiSkillsListProps) => {
    const {deleteSkill, filteredSkills, handleDownloadSkill, renameSkill, searchQuery, setSearchQuery} =
        useAiSkillsList(skills);

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

                <AiSkillsCreateDropdown />
            </div>

            <div className="flex-1 divide-y divide-border/50">
                {filteredSkills.length > 0 ? (
                    filteredSkills.map((skill) => (
                        <AiSkillListItem
                            deleteSkill={deleteSkill}
                            key={skill.id}
                            onDownload={handleDownloadSkill}
                            onRename={renameSkill}
                            skill={skill}
                        />
                    ))
                ) : (
                    <div className="flex items-center justify-center py-12 text-sm text-content-neutral-secondary">
                        No skills found matching your search.
                    </div>
                )}
            </div>
        </div>
    );
};

export default AiSkillsList;
