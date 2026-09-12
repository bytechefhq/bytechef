import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import useCopilotStateContributorRegistry from '@/shared/components/copilot/stores/useCopilotStateContributorRegistry';
import {Source} from '@/shared/components/copilot/stores/useCopilotStore';
import {createTestQueryClientWrapper, renderHook, resetAll} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {useAiSkillsStore} from '../../stores/useAiSkillsStore';
import useAiSkills from '../useAiSkills';

const {closeSkillDetailMock, invalidateSkillQueriesMock, navigateMock, openSkillDetailMock} = vi.hoisted(() => ({
    closeSkillDetailMock: vi.fn(),
    invalidateSkillQueriesMock: vi.fn(),
    navigateMock: vi.fn(),
    openSkillDetailMock: vi.fn(),
}));

vi.mock('@/pages/automation/ai/skills/utils/invalidateSkillQueries', () => ({default: invalidateSkillQueriesMock}));

vi.mock('react-router-dom', async (importOriginal) => ({
    ...(await importOriginal<typeof import('react-router-dom')>()),
    useNavigate: () => navigateMock,
}));

const LIST_PATH = '/automation/settings/ai/skills';

const renderAiSkills = (pathname = LIST_PATH) => {
    const wrapper = ({children}: {children: ReactNode}) => {
        const element = createTestQueryClientWrapper()({children});

        return (
            <MemoryRouter initialEntries={[pathname]}>
                <Routes>
                    <Route element={element} path="/:platform/settings/ai/skills/:skillId" />

                    <Route element={element} path="/:platform/settings/ai/skills" />
                </Routes>
            </MemoryRouter>
        );
    };

    return renderHook(() => useAiSkills(), {wrapper});
};

describe('useAiSkills', () => {
    beforeEach(() => {
        useAiSkillsStore.setState({
            closeSkillDetail: closeSkillDetailMock,
            openSkillDetail: openSkillDetailMock,
            selectedSkillId: null,
            skillsHeaderInfo: {},
            skillsView: 'list',
        });

        useCopilotStateContributorRegistry.setState({contributors: []});
    });

    afterEach(() => {
        resetAll();
    });

    it('titles the list route and keeps it on the list toolbar', () => {
        const {result} = renderAiSkills();

        expect(result.current.headerTitle).toBe('AI Skills');
        expect(result.current.isDetailView).toBe(false);
        expect(result.current.showToolbar).toBe(true);
    });

    it('titles the detail route with the skill name and drops the list toolbar', () => {
        useAiSkillsStore.setState({selectedSkillId: '42', skillsHeaderInfo: {title: 'billing-runbook'}});

        const {result} = renderAiSkills(`${LIST_PATH}/42`);

        expect(result.current.headerTitle).toBe('billing-runbook');
        expect(result.current.isDetailView).toBe(true);
        expect(result.current.showToolbar).toBe(false);
    });

    it('falls back to Skill while the detail header carries no name', () => {
        useAiSkillsStore.setState({selectedSkillId: '42'});

        const {result} = renderAiSkills(`${LIST_PATH}/42`);

        expect(result.current.headerTitle).toBe('Skill');
    });

    it('hides the search and create controls while the list is empty', () => {
        useAiSkillsStore.setState({skillsView: 'empty'});

        const {result} = renderAiSkills();

        expect(result.current.showSearchAndCreate).toBe(false);
    });

    it('sends the back control to the skills mount the visitor is already on', () => {
        const {result} = renderAiSkills('/embedded/settings/ai/skills/42');

        result.current.handleBack();

        expect(navigateMock).toHaveBeenCalledWith('/embedded/settings/ai/skills');
    });

    it('opens the skill the route points at', () => {
        renderAiSkills(`${LIST_PATH}/42`);

        expect(openSkillDetailMock).toHaveBeenCalledWith('42', '');
    });

    it('leaves the store alone when it already holds the skill the route points at', () => {
        useAiSkillsStore.setState({selectedSkillId: '42', skillsView: 'detail'});

        renderAiSkills(`${LIST_PATH}/42`);

        expect(openSkillDetailMock).not.toHaveBeenCalled();
    });

    it('closes an open detail when the route returns to the list', () => {
        useAiSkillsStore.setState({selectedSkillId: '42', skillsView: 'detail'});

        renderAiSkills();

        expect(closeSkillDetailMock).toHaveBeenCalled();
    });

    it('refreshes the skill queries after a copilot turn, until it unmounts', () => {
        const {unmount} = renderAiSkills();

        useCopilotPostTurnRegistry.getState().runFor(Source.SKILLS);

        expect(invalidateSkillQueriesMock).toHaveBeenCalledTimes(1);

        unmount();

        useCopilotPostTurnRegistry.getState().runFor(Source.SKILLS);

        expect(invalidateSkillQueriesMock).toHaveBeenCalledTimes(1);
    });

    it('contributes the selected skill to the copilot state', () => {
        useAiSkillsStore.setState({selectedSkillId: '42', skillsHeaderInfo: {title: 'billing-runbook'}});

        renderAiSkills(`${LIST_PATH}/42`);

        expect(useCopilotStateContributorRegistry.getState().contribute()).toEqual({
            currentSelectedSkillId: '42',
            currentSelectedSkillName: 'billing-runbook',
        });
    });

    it('contributes nothing while no skill is selected', () => {
        const {unmount} = renderAiSkills();

        expect(useCopilotStateContributorRegistry.getState().contribute()).toEqual({});

        unmount();

        expect(useCopilotStateContributorRegistry.getState().contributors).toHaveLength(0);
    });
});
