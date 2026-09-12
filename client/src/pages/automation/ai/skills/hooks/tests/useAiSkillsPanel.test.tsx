import {AiSkill} from '@/shared/middleware/graphql';
import {renderHook, resetAll} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {useAiSkillsStore} from '../../stores/useAiSkillsStore';
import useAiSkillsPanel from '../useAiSkillsPanel';

const {isErrorRef, setSkillsPanelOpenMock, setSkillsViewMock, skillsRef, toastErrorMock} = vi.hoisted(() => ({
    isErrorRef: {current: false},
    setSkillsPanelOpenMock: vi.fn(),
    setSkillsViewMock: vi.fn(),
    skillsRef: {current: [] as AiSkill[]},
    toastErrorMock: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAiSkillsQuery: () => ({
        data: {aiSkills: skillsRef.current},
        isError: isErrorRef.current,
        isLoading: false,
    }),
}));

vi.mock('sonner', () => ({toast: {error: toastErrorMock}}));

const skills = [{id: '1', name: 'billing-runbook'}] as unknown as AiSkill[];

describe('useAiSkillsPanel', () => {
    beforeEach(() => {
        isErrorRef.current = false;
        skillsRef.current = [];

        useAiSkillsStore.setState({
            setSkillsPanelOpen: setSkillsPanelOpenMock,
            setSkillsView: setSkillsViewMock,
            skillsView: 'list',
        });
    });

    afterEach(() => {
        resetAll();
    });

    it('returns the skills the query carries', () => {
        skillsRef.current = skills;

        const {result} = renderHook(() => useAiSkillsPanel());

        expect(result.current.skills).toEqual(skills);
        expect(result.current.isLoading).toBe(false);
    });

    it('turns an empty view into the list once a skill arrives', () => {
        skillsRef.current = skills;

        useAiSkillsStore.setState({skillsView: 'empty'});

        renderHook(() => useAiSkillsPanel());

        expect(setSkillsViewMock).toHaveBeenCalledWith('list');
    });

    it('turns the list into the empty view once the last skill goes', () => {
        renderHook(() => useAiSkillsPanel());

        expect(setSkillsViewMock).toHaveBeenCalledWith('empty');
    });

    it('leaves the detail view alone', () => {
        useAiSkillsStore.setState({skillsView: 'detail'});

        renderHook(() => useAiSkillsPanel());

        expect(setSkillsViewMock).not.toHaveBeenCalled();
    });

    it('closes a detail back to the list rather than closing the panel', () => {
        useAiSkillsStore.setState({skillsView: 'detail'});

        const {result} = renderHook(() => useAiSkillsPanel());

        result.current.handleClose();

        expect(setSkillsViewMock).toHaveBeenCalledWith('list');
        expect(setSkillsPanelOpenMock).not.toHaveBeenCalled();
    });

    it('closes the panel from the list view', () => {
        skillsRef.current = skills;

        const {result} = renderHook(() => useAiSkillsPanel());

        result.current.handleClose();

        expect(setSkillsPanelOpenMock).toHaveBeenCalledWith(false);
    });

    it('reports a failed load as a single toast', () => {
        isErrorRef.current = true;

        const {rerender} = renderHook(() => useAiSkillsPanel());

        rerender();

        expect(toastErrorMock).toHaveBeenCalledTimes(1);
        expect(toastErrorMock).toHaveBeenCalledWith('Failed to load skills', {id: 'skills-load-error'});
    });
});
