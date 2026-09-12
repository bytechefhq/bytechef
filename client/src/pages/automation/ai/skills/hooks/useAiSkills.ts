import {useAiSkillsStore} from '@/pages/automation/ai/skills/stores/useAiSkillsStore';
import getAiSkillsBasePath from '@/pages/automation/ai/skills/utils/getAiSkillsBasePath';
import invalidateSkillQueries from '@/pages/automation/ai/skills/utils/invalidateSkillQueries';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import useCopilotStateContributorRegistry from '@/shared/components/copilot/stores/useCopilotStateContributorRegistry';
import {Source} from '@/shared/components/copilot/stores/useCopilotStore';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback, useEffect} from 'react';
import {useLocation, useNavigate, useParams} from 'react-router-dom';

type AiSkillsRouteType = 'detail' | 'list';

const determineRoute = (skillId: string | undefined): AiSkillsRouteType => {
    return skillId ? 'detail' : 'list';
};

export default function useAiSkills() {
    const closeSkillDetail = useAiSkillsStore((state) => state.closeSkillDetail);
    const openSkillDetail = useAiSkillsStore((state) => state.openSkillDetail);
    const selectedSkillId = useAiSkillsStore((state) => state.selectedSkillId);
    const skillsHeaderInfo = useAiSkillsStore((state) => state.skillsHeaderInfo);
    const skillsView = useAiSkillsStore((state) => state.skillsView);

    const registerPostTurn = useCopilotPostTurnRegistry((state) => state.register);

    const {skillId} = useParams<{skillId?: string}>();

    const location = useLocation();
    const navigate = useNavigate();

    const queryClient = useQueryClient();

    const route = determineRoute(skillId);

    const isDetailView = route === 'detail';

    const headerTitle = isDetailView ? (skillsHeaderInfo.title ?? 'Skill') : 'AI Skills';

    const showToolbar = route === 'list';
    const showSearchAndCreate = skillsView !== 'empty';

    const skillsBasePath = getAiSkillsBasePath(location.pathname);

    const handleBack = useCallback(() => navigate(skillsBasePath), [navigate, skillsBasePath]);

    useEffect(() => {
        return registerPostTurn(Source.SKILLS, () => {
            invalidateSkillQueries(queryClient);
        });
    }, [queryClient, registerPostTurn]);

    useEffect(() => {
        return useCopilotStateContributorRegistry.getState().register(() => {
            const {selectedSkillId: activeSkillId, skillsHeaderInfo: activeHeaderInfo} = useAiSkillsStore.getState();

            if (activeSkillId == null) {
                return {};
            }

            return {
                currentSelectedSkillId: activeSkillId,
                currentSelectedSkillName: activeHeaderInfo.title,
            };
        });
    }, []);

    useEffect(() => {
        if (route === 'detail' && skillId && selectedSkillId !== skillId) {
            openSkillDetail(skillId, '');
        } else if (route === 'list' && skillsView === 'detail') {
            closeSkillDetail();
        }
    }, [closeSkillDetail, openSkillDetail, route, selectedSkillId, skillId, skillsView]);

    return {
        handleBack,
        headerTitle,
        isDetailView,
        showSearchAndCreate,
        showToolbar,
    };
}
