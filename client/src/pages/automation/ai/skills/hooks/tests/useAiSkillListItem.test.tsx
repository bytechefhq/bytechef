import {AiSkill} from '@/shared/middleware/graphql';
import {act, renderHook, resetAll} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, describe, expect, it, vi} from 'vitest';

import useAiSkillListItem from '../useAiSkillListItem';

const {navigateMock, openSkillDetailMock} = vi.hoisted(() => ({
    navigateMock: vi.fn(),
    openSkillDetailMock: vi.fn(),
}));

vi.mock('@/pages/automation/ai/skills/stores/useAiSkillsStore', () => ({
    useAiSkillsStore: () => ({openSkillDetail: openSkillDetailMock}),
}));

vi.mock('react-router-dom', async (importOriginal) => ({
    ...(await importOriginal<typeof import('react-router-dom')>()),
    useNavigate: () => navigateMock,
}));

const skill = {id: '7', name: 'billing-runbook'} as AiSkill;

const renderListItem = (
    overrides: Partial<Parameters<typeof useAiSkillListItem>[0]> = {},
    pathname = '/automation/settings/ai/skills'
) => {
    const deleteSkill = vi.fn().mockResolvedValue(undefined);
    const onDownload = vi.fn();
    const onUpdate = vi.fn();

    const wrapper = ({children}: {children: ReactNode}) => (
        <MemoryRouter initialEntries={[pathname]}>{children}</MemoryRouter>
    );

    const {result} = renderHook(() => useAiSkillListItem({deleteSkill, onDownload, onUpdate, skill, ...overrides}), {
        wrapper,
    });

    return {deleteSkill, onDownload, onUpdate, result};
};

describe('useAiSkillListItem', () => {
    afterEach(() => {
        resetAll();
    });

    it('opens the detail view on the mount the row was clicked from', () => {
        const {result} = renderListItem();

        act(() => result.current.handleClick());

        expect(openSkillDetailMock).toHaveBeenCalledWith('7', 'billing-runbook');
        expect(navigateMock).toHaveBeenCalledWith('/automation/settings/ai/skills/7');
    });

    it('keeps an embedded visitor on the embedded mount', () => {
        const {result} = renderListItem({}, '/embedded/settings/ai/skills');

        act(() => result.current.handleClick());

        expect(navigateMock).toHaveBeenCalledWith('/embedded/settings/ai/skills/7');
    });

    it('closes the delete dialog once the skill is gone', async () => {
        const {deleteSkill, result} = renderListItem();

        act(() => result.current.setShowDeleteDialog(true));

        await act(async () => await result.current.handleDeleteClick());

        expect(deleteSkill).toHaveBeenCalledWith('7');
        expect(result.current.showDeleteDialog).toBe(false);
    });

    it('closes the delete dialog even when the delete fails', async () => {
        const deleteSkill = vi.fn().mockRejectedValue(new Error('nope'));
        const {result} = renderListItem({deleteSkill});

        act(() => result.current.setShowDeleteDialog(true));

        await act(async () => await result.current.handleDeleteClick());

        expect(result.current.showDeleteDialog).toBe(false);
    });

    it('passes the skill through to the download handler', () => {
        const {onDownload, result} = renderListItem();

        act(() => result.current.handleDownloadClick());

        expect(onDownload).toHaveBeenCalledWith('7', 'billing-runbook');
    });

    it('saves an edit and closes the edit dialog', () => {
        const {onUpdate, result} = renderListItem();

        act(() => result.current.setShowEditDialog(true));
        act(() => result.current.handleEditSave('renamed', 'new description'));

        expect(onUpdate).toHaveBeenCalledWith('7', 'renamed', 'new description');
        expect(result.current.showEditDialog).toBe(false);
    });
});
