import AiSkillListItem from '@/pages/automation/ai/skills/components/AiSkillListItem';
import useAiSkillsList from '@/pages/automation/ai/skills/hooks/useAiSkillsList';
import {AiSkill} from '@/shared/middleware/graphql';

interface AiSkillsListProps {
    skills: AiSkill[];
}

const AiSkillsList = ({skills}: AiSkillsListProps) => {
    const {deleteSkill, filteredSkills, handleDownloadSkill, updateSkill} = useAiSkillsList(skills);

    return (
        <div className="flex flex-1 flex-col">
            <div className="flex-1">
                {filteredSkills.length > 0 ? (
                    filteredSkills.map((skill) => (
                        <AiSkillListItem
                            deleteSkill={deleteSkill}
                            key={skill.id}
                            onDownload={handleDownloadSkill}
                            onUpdate={updateSkill}
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
