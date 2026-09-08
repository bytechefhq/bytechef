import {TEMPLATE_SHARING_DOCUMENTATION_URL} from '@/shared/constants';
import {fireEvent, render, screen} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {WorkflowShareDialog} from '../WorkflowShareDialog';

const hoisted = vi.hoisted(() => ({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    shared: undefined as any,
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useDeleteSharedWorkflowMutation: () => ({isPending: false, mutate: vi.fn()}),
    useExportSharedWorkflowMutation: () => ({isPending: false, mutate: vi.fn()}),
    useSharedWorkflowQuery: () => ({data: {sharedWorkflow: hoisted.shared}, refetch: vi.fn()}),
}));

describe('WorkflowShareDialog', () => {
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
            <WorkflowShareDialog
                onOpenChange={vi.fn()}
                open
                projectVersion={2}
                workflowId="workflow-id"
                workflowUuid="workflow-uuid"
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
