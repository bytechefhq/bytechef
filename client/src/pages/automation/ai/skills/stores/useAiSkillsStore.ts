import {create} from 'zustand';

type SkillsViewType = 'detail' | 'empty' | 'list' | 'uploadForm' | 'writeForm';

interface SkillsHeaderInfoI {
    subtitle?: string;
    title?: string;
}

const SKILLS_HEADER_INFO: Record<SkillsViewType, SkillsHeaderInfoI> = {
    detail: {},
    empty: {},
    list: {title: 'Skills'},
    uploadForm: {subtitle: 'Upload a file to create a skill.', title: 'Upload Skill Files'},
    writeForm: {subtitle: 'Write a name, description, and instructions for the skill.', title: 'Add a Skill'},
};

interface AiSkillsStoreI {
    closeSkillDetail: () => void;
    copilotPanelOpen: boolean;
    openSkillDetail: (id: string, name: string) => void;
    searchQuery: string;
    selectedSkillId: string | null;
    setCopilotPanelOpen: (open: boolean) => void;
    setSearchQuery: (query: string) => void;
    setSkillsPanelOpen: (open: boolean) => void;
    setSkillsView: (view: SkillsViewType) => void;
    skillsHeaderInfo: SkillsHeaderInfoI;
    skillsPanelOpen: boolean;
    skillsView: SkillsViewType;
}

export const useAiSkillsStore = create<AiSkillsStoreI>(() => ({
    closeSkillDetail: () => {
        useAiSkillsStore.setState({
            selectedSkillId: null,
            skillsHeaderInfo: SKILLS_HEADER_INFO.list,
            skillsView: 'list',
        });
    },
    copilotPanelOpen: false,
    openSkillDetail: (id: string, name: string) => {
        useAiSkillsStore.setState({
            selectedSkillId: id,
            skillsHeaderInfo: {title: name},
            skillsPanelOpen: true,
            skillsView: 'detail',
        });
    },
    searchQuery: '',
    selectedSkillId: null,
    setCopilotPanelOpen: (open: boolean) => {
        useAiSkillsStore.setState({copilotPanelOpen: open});
    },
    setSearchQuery: (query: string) => {
        useAiSkillsStore.setState({searchQuery: query});
    },
    setSkillsPanelOpen: (open: boolean) => {
        useAiSkillsStore.setState({skillsPanelOpen: open});
    },
    setSkillsView: (view: SkillsViewType) => {
        useAiSkillsStore.setState({
            selectedSkillId: view === 'detail' ? useAiSkillsStore.getState().selectedSkillId : null,
            skillsHeaderInfo: SKILLS_HEADER_INFO[view],
            skillsView: view,
        });
    },
    skillsHeaderInfo: SKILLS_HEADER_INFO.empty,
    skillsPanelOpen: false,
    skillsView: 'empty',
}));
