import {create} from 'zustand';

type SkillsViewType = 'createWithAi' | 'detail' | 'empty' | 'list' | 'uploadForm' | 'writeForm';

interface SkillsHeaderInfoI {
    subtitle?: string;
    title?: string;
}

const SKILLS_HEADER_INFO: Record<SkillsViewType, SkillsHeaderInfoI> = {
    createWithAi: {subtitle: 'Let AI generate a skill for you.', title: 'Create with AI'},
    detail: {},
    empty: {},
    list: {title: 'Skills'},
    uploadForm: {subtitle: 'Upload a file to create a skill.', title: 'Upload Skill Files'},
    writeForm: {subtitle: 'Write a name, description, and instructions for the skill.', title: 'Add a Skill'},
};

interface AiAgentSkillsStoreI {
    closeSkillDetail: () => void;
    openSkillDetail: (id: string, name: string) => void;
    selectedSkillId: string | null;
    setSkillsPanelOpen: (open: boolean) => void;
    setSkillsView: (view: SkillsViewType) => void;
    skillsHeaderInfo: SkillsHeaderInfoI;
    skillsPanelOpen: boolean;
    skillsView: SkillsViewType;
}

export const useAiAgentSkillsStore = create<AiAgentSkillsStoreI>(() => ({
    closeSkillDetail: () => {
        useAiAgentSkillsStore.setState({
            selectedSkillId: null,
            skillsHeaderInfo: SKILLS_HEADER_INFO.list,
            skillsView: 'list',
        });
    },
    openSkillDetail: (id: string, name: string) => {
        useAiAgentSkillsStore.setState({
            selectedSkillId: id,
            skillsHeaderInfo: {title: name},
            skillsPanelOpen: true,
            skillsView: 'detail',
        });
    },
    selectedSkillId: null,
    setSkillsPanelOpen: (open: boolean) => {
        useAiAgentSkillsStore.setState({skillsPanelOpen: open});
    },
    setSkillsView: (view: SkillsViewType) => {
        useAiAgentSkillsStore.setState({
            selectedSkillId: view === 'detail' ? useAiAgentSkillsStore.getState().selectedSkillId : null,
            skillsHeaderInfo: SKILLS_HEADER_INFO[view],
            skillsView: view,
        });
    },
    skillsHeaderInfo: SKILLS_HEADER_INFO.empty,
    skillsPanelOpen: false,
    skillsView: 'empty',
}));
