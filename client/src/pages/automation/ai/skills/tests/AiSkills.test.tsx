import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import {Source} from '@/shared/components/copilot/stores/useCopilotStore';
import {applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {render, resetAll, screen} from '@/shared/util/test-utils';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AiSkills from '../AiSkills';
import {useAiSkillsStore} from '../stores/useAiSkillsStore';

const {closeSkillDetailMock, invalidateSkillQueriesMock, navigateMock, openSkillDetailMock, tagFilterGroupsRef} =
    vi.hoisted(() => ({
        closeSkillDetailMock: vi.fn(),
        invalidateSkillQueriesMock: vi.fn(),
        navigateMock: vi.fn(),
        openSkillDetailMock: vi.fn(),
        tagFilterGroupsRef: {current: [] as unknown[]},
    }));

vi.mock('@/pages/automation/ai/skills/AiSkillsPanel', () => ({
    default: () => <div data-testid="skills-panel" />,
}));

vi.mock('@/pages/automation/ai/skills/components/AiSkillsCreateDropdown', () => ({
    default: () => <button type="button">Create Skill</button>,
}));

vi.mock('@/pages/automation/ai/skills/hooks/useAiSkillsTagFilterGroups', () => ({
    default: () => tagFilterGroupsRef.current,
}));

vi.mock('@/pages/automation/ai/skills/utils/invalidateSkillQueries', () => ({
    default: invalidateSkillQueriesMock,
}));

vi.mock('@/shared/components/copilot/CopilotButton', () => ({
    default: () => <button type="button">Copilot</button>,
}));

vi.mock('react-router-dom', async (importOriginal) => ({
    ...(await importOriginal<typeof import('react-router-dom')>()),
    useNavigate: () => navigateMock,
}));

const LIST_PATH = '/automation/settings/ai/skills';

const renderPage = (pathname = LIST_PATH) =>
    render(
        <MemoryRouter initialEntries={[pathname]}>
            <Routes>
                <Route element={<AiSkills />} path="/:platform/settings/ai/skills/:skillId" />

                <Route element={<AiSkills />} path="/:platform/settings/ai/skills" />
            </Routes>
        </MemoryRouter>
    );

describe('AiSkills', () => {
    beforeEach(() => {
        useAiSkillsStore.setState({
            closeSkillDetail: closeSkillDetailMock,
            openSkillDetail: openSkillDetailMock,
            searchQuery: '',
            selectedSkillId: null,
            skillsHeaderInfo: {},
            skillsView: 'list',
        });

        tagFilterGroupsRef.current = [];
    });

    afterEach(() => {
        resetAll();

        applicationInfoStore.setState((state) => ({...state, featureFlags: {}}));
    });

    it('renders the list toolbar with the search box and the create control', () => {
        renderPage();

        expect(screen.getByText('AI Skills')).toBeInTheDocument();
        expect(screen.getByPlaceholderText('Search skills...')).toBeInTheDocument();
        expect(screen.getByText('Create Skill')).toBeInTheDocument();
    });

    it('hides the search box and the create control while the list is empty', () => {
        useAiSkillsStore.setState({skillsView: 'empty'});

        renderPage();

        expect(screen.queryByPlaceholderText('Search skills...')).not.toBeInTheDocument();
        expect(screen.queryByText('Create Skill')).not.toBeInTheDocument();
    });

    it('opens the skill the route points at', () => {
        renderPage(`${LIST_PATH}/42`);

        expect(openSkillDetailMock).toHaveBeenCalledWith('42', '');
    });

    it('closes an open detail when the route returns to the list', () => {
        useAiSkillsStore.setState({selectedSkillId: '42', skillsView: 'detail'});

        renderPage();

        expect(closeSkillDetailMock).toHaveBeenCalled();
    });

    it('titles the detail route with the skill name and falls back to Skill', () => {
        useAiSkillsStore.setState({selectedSkillId: '42', skillsHeaderInfo: {title: 'billing-runbook'}});

        const {unmount} = renderPage(`${LIST_PATH}/42`);

        expect(screen.getByText('billing-runbook')).toBeInTheDocument();

        unmount();

        useAiSkillsStore.setState({skillsHeaderInfo: {}});

        renderPage(`${LIST_PATH}/42`);

        expect(screen.getByText('Skill')).toBeInTheDocument();
    });

    it('sends the back control to the skills mount the visitor is already on', () => {
        renderPage('/embedded/settings/ai/skills/42');

        screen.getByLabelText('Back to skills').click();

        expect(navigateMock).toHaveBeenCalledWith('/embedded/settings/ai/skills');
    });

    it('refreshes the skill queries after a copilot turn', () => {
        renderPage();

        expect(invalidateSkillQueriesMock).not.toHaveBeenCalled();

        useCopilotPostTurnRegistry.getState().runFor(Source.SKILLS);

        expect(invalidateSkillQueriesMock).toHaveBeenCalled();
    });
});
