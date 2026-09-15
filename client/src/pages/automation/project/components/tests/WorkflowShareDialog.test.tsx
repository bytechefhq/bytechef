import {TEMPLATE_SHARING_DOCUMENTATION_URL} from '@/shared/constants';
import {fireEvent, render, screen} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {WorkflowShareDialog} from '../WorkflowShareDialog';

const hoisted = vi.hoisted(() => ({
    deleteSharedWorkflowMutate: vi.fn(),
    exportSharedWorkflowMutate: vi.fn(),
    grantedScopes: [] as string[],
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    shared: undefined as any,
}));

vi.mock('@/shared/hooks/useHasWorkspaceScope', () => ({
    useHasWorkspaceScope: (_workspaceId: number | undefined, scope: string) => hoisted.grantedScopes.includes(scope),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useDeleteSharedWorkflowMutation: () => ({isPending: false, mutate: hoisted.deleteSharedWorkflowMutate}),
    useExportSharedWorkflowMutation: () => ({isPending: false, mutate: hoisted.exportSharedWorkflowMutate}),
    useSharedWorkflowQuery: () => ({data: {sharedWorkflow: hoisted.shared}, refetch: vi.fn()}),
}));

describe('WorkflowShareDialog', () => {
    beforeEach(() => {
        hoisted.deleteSharedWorkflowMutate.mockReset();
        hoisted.exportSharedWorkflowMutate.mockReset();
        hoisted.grantedScopes = ['WORKFLOW_EDIT', 'WORKFLOW_DELETE'];
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

    it('lets a member holding WORKFLOW_DELETE disable a shared template', () => {
        hoisted.shared = {exported: true, projectVersion: 2};

        renderDialog();

        const sharedTemplateSwitch = screen.getByRole('switch', {name: 'Shared template'});

        expect(sharedTemplateSwitch).toBeEnabled();

        fireEvent.click(sharedTemplateSwitch);

        expect(hoisted.deleteSharedWorkflowMutate).toHaveBeenCalledWith({workflowId: 'workflow-id'}, expect.anything());
    });

    it('disables the shared template switch without WORKFLOW_DELETE', () => {
        hoisted.grantedScopes = ['WORKFLOW_EDIT'];
        hoisted.shared = {exported: true, projectVersion: 2};

        renderDialog();

        const sharedTemplateSwitch = screen.getByRole('switch', {name: 'Shared template'});

        expect(sharedTemplateSwitch).toBeDisabled();

        fireEvent.click(sharedTemplateSwitch);

        expect(hoisted.deleteSharedWorkflowMutate).not.toHaveBeenCalled();
    });

    it('keeps re-enabling a disabled template available without WORKFLOW_DELETE', () => {
        hoisted.grantedScopes = ['WORKFLOW_EDIT'];
        hoisted.shared = {exported: false};

        renderDialog();

        const sharedTemplateSwitch = screen.getByRole('switch', {name: 'Shared template'});

        expect(sharedTemplateSwitch).toBeEnabled();

        fireEvent.click(sharedTemplateSwitch);

        expect(hoisted.exportSharedWorkflowMutate).toHaveBeenCalled();
    });
});
