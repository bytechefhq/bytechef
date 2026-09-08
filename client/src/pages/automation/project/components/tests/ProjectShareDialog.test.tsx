import {TEMPLATE_SHARING_DOCUMENTATION_URL} from '@/shared/constants';
import {fireEvent, render, screen} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {ProjectShareDialog} from '../ProjectShareDialog';

const hoisted = vi.hoisted(() => ({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    shared: undefined as any,
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useDeleteSharedProjectMutation: () => ({isPending: false, mutate: vi.fn()}),
    useExportSharedProjectMutation: () => ({isPending: false, mutate: vi.fn()}),
    useSharedProjectQuery: () => ({data: {sharedProject: hoisted.shared}, refetch: vi.fn()}),
}));

describe('ProjectShareDialog', () => {
    beforeEach(() => {
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
});
