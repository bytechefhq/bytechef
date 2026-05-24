const SKILL_COLORS = ['bg-blue-500', 'bg-green-500', 'bg-purple-500', 'bg-orange-500', 'bg-pink-500', 'bg-teal-500'];

export default function getSkillColor(skillId: string): string {
    let hash = 0;

    for (let index = 0; index < skillId.length; index++) {
        hash = skillId.charCodeAt(index) + ((hash << 5) - hash);
    }

    return SKILL_COLORS[Math.abs(hash) % SKILL_COLORS.length];
}
