import {TEMPLATE_SHARING_DOCUMENTATION_URL} from '@/shared/constants';
import {fireEvent, render, screen} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {ProjectShareDialog} from '../ProjectShareDialog';

const hoisted = vi.hoisted(() => ({
    deleteSharedProjectMutate: vi.fn(),
    exportSharedProjectMutate: vi.fn(),
    grantedScopes: [] as string[],
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    shared: undefined as any,
}));

vi.mock('@/shared/hooks/useHasWorkspaceScope', () => ({
    useHasWorkspaceScope: (_workspaceId: number | undefined, scope: string) => hoisted.grantedScopes.includes(scope),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useDeleteSharedProjectMutation: () => ({isPending: false, mutate: hoisted.deleteSharedProjectMutate}),
    useExportSharedProjectMutation: () => ({isPending: false, mutate: hoisted.exportSharedProjectMutate}),
    useSharedProjectQuery: () => ({data: {sharedProject: hoisted.shared}, refetch: vi.fn()}),
}));

describe('ProjectShareDialog', () => {
    beforeEach(() => {
        hoisted.deleteSharedProjectMutate.mockReset();
        hoisted.exportSharedProjectMutate.mockReset();
        hoisted.grantedScopes = ['PROJECT_SETTINGS'];
        hoisted.shared = undefined;

        vi.spyOn(window, 'open')
            .mockImplementation(() => null)
            .mockClear();
    });

    afterEach(() => {
        vi.mocked(window.open).mockRestore();
    });

    const renderDialog = () =>
        render(
            <ProjectShareDialog
                onOpenChange={vi.fn()}
                open
                projectId={1}
                projectUuid="project-uuid"
                projectVersion={2}
            />
        );

    it('opens the documentation from Learn more before anything is shared', () => {
        renderDialog();

        screen.getAllByText('Learn more').forEach((learnMoreButton) => fireEvent.click(learnMoreButton));

        expect(window.open).toHaveBeenCalledWith(TEMPLATE_SHARING_DOCUMENTATION_URL, '_blank', 'noopener,noreferrer');
    });

    it('opens the documentation from Learn more while the template is disabled', () => {
        hoisted.shared = {exported: false};

        renderDialog();

        screen.getAllByText('Learn more').forEach((learnMoreButton) => fireEvent.click(learnMoreButton));

        expect(window.open).toHaveBeenCalledWith(TEMPLATE_SHARING_DOCUMENTATION_URL, '_blank', 'noopener,noreferrer');
    });

    it('opens the documentation from Learn more when the shared version is stale', () => {
        hoisted.shared = {exported: true, projectVersion: 1};

        renderDialog();

        screen.getAllByText('Learn more').forEach((learnMoreButton) => fireEvent.click(learnMoreButton));

        expect(window.open).toHaveBeenCalledWith(TEMPLATE_SHARING_DOCUMENTATION_URL, '_blank', 'noopener,noreferrer');
    });

    it('lets a member holding PROJECT_SETTINGS disable a shared template', () => {
        hoisted.shared = {exported: true, projectVersion: 2};

        renderDialog();

        const sharedTemplateSwitch = screen.getByRole('switch', {name: 'Shared template'});

        expect(sharedTemplateSwitch).toBeEnabled();

        fireEvent.click(sharedTemplateSwitch);

        expect(hoisted.deleteSharedProjectMutate).toHaveBeenCalledWith({id: '1'}, expect.anything());
    });

    it('disables the shared template switches without PROJECT_SETTINGS', () => {
        hoisted.grantedScopes = [];
        hoisted.shared = {exported: true, projectVersion: 2};

        const {unmount} = renderDialog();

        const exportedSwitch = screen.getByRole('switch', {name: 'Shared template'});

        expect(exportedSwitch).toBeDisabled();

        fireEvent.click(exportedSwitch);

        unmount();

        hoisted.shared = {exported: false};

        renderDialog();

        const disabledTemplateSwitch = screen.getByRole('switch', {name: 'Shared template'});

        expect(disabledTemplateSwitch).toBeDisabled();

        fireEvent.click(disabledTemplateSwitch);

        expect(hoisted.deleteSharedProjectMutate).not.toHaveBeenCalled();
        expect(hoisted.exportSharedProjectMutate).not.toHaveBeenCalled();
    });
});
