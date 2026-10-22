import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {ProjectStatus} from '@/shared/middleware/automation/configuration';
import {NodeDataType} from '@/shared/types';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {fireEvent, render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import LoadWorkflowVersionDialog from '../LoadWorkflowVersionDialog';

const hoisted = vi.hoisted(() => ({
    clearWorkflowHistory: vi.fn(),
    mutate: vi.fn(),
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    mutationOptions: undefined as any,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    projectVersions: undefined as any,
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowDataStore', () => ({
    clearWorkflowHistory: hoisted.clearWorkflowHistory,
}));

vi.mock('@/shared/queries/automation/projectVersions.queries', () => ({
    ProjectVersionKeys: {projectProjectVersions: (projectId: number) => ['projectVersions', 'projects', projectId]},
    useGetProjectWorkflowVersionsQuery: () => ({data: hoisted.projectVersions, isLoading: false}),
}));

vi.mock('@/shared/mutations/automation/workflows.mutations', () => ({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    useRestoreProjectVersionWorkflowMutation: (options: any) => {
        hoisted.mutationOptions = options;

        return {isPending: false, mutate: hoisted.mutate};
    },
}));

describe('LoadWorkflowVersionDialog', () => {
    const onClose = vi.fn();

    beforeEach(() => {
        hoisted.clearWorkflowHistory.mockReset();
        hoisted.mutate.mockReset();
        hoisted.projectVersions = [
            {
                description: 'Second release',
                publishedDate: new Date('2026-10-10T10:00:00Z'),
                status: ProjectStatus.Published,
                version: 2,
            },
            {publishedDate: new Date('2026-10-09T10:00:00Z'), status: ProjectStatus.Published, version: 1},
        ];

        onClose.mockReset();
    });

    const renderDialog = () =>
        render(
            <QueryClientProvider client={new QueryClient()}>
                <LoadWorkflowVersionDialog onClose={onClose} projectId={7} workflowUuid="workflow-uuid" />
            </QueryClientProvider>
        );

    it('lists the published versions that contain the workflow', () => {
        renderDialog();

        expect(screen.getByRole('option', {name: 'Version 2'})).toBeInTheDocument();
        expect(screen.getByRole('option', {name: 'Version 1'})).toBeInTheDocument();
        expect(screen.getByText('Second release')).toBeInTheDocument();
    });

    it('loads the selected version into the draft', () => {
        renderDialog();

        const loadButton = screen.getByRole('button', {name: 'Load into draft'});

        expect(loadButton).toBeDisabled();

        fireEvent.click(screen.getByRole('option', {name: 'Version 1'}));
        fireEvent.click(loadButton);

        expect(hoisted.mutate).toHaveBeenCalledWith({id: 7, projectVersion: 1, workflowUuid: 'workflow-uuid'});
    });

    it('selects a version from the keyboard', () => {
        renderDialog();

        fireEvent.keyDown(screen.getByRole('option', {name: 'Version 2'}), {key: 'Enter'});

        expect(screen.getByRole('option', {name: 'Version 2'})).toHaveAttribute('aria-selected', 'true');

        fireEvent.click(screen.getByRole('button', {name: 'Load into draft'}));

        expect(hoisted.mutate).toHaveBeenCalledWith({id: 7, projectVersion: 2, workflowUuid: 'workflow-uuid'});
    });

    it('clears the undo history and closes after a successful load', () => {
        renderDialog();

        hoisted.mutationOptions.onSuccess(undefined, {id: 7, projectVersion: 1, workflowUuid: 'workflow-uuid'});

        expect(hoisted.clearWorkflowHistory).toHaveBeenCalled();
        expect(onClose).toHaveBeenCalled();
    });

    it('closes the node details panel after a successful load', () => {
        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: {name: 'dataStorage_1'} as NodeDataType,
            workflowNodeDetailsPanelOpen: true,
        });

        renderDialog();

        hoisted.mutationOptions.onSuccess(undefined, {id: 7, projectVersion: 1, workflowUuid: 'workflow-uuid'});

        expect(useWorkflowNodeDetailsPanelStore.getState().currentNode).toBeUndefined();
        expect(useWorkflowNodeDetailsPanelStore.getState().workflowNodeDetailsPanelOpen).toBe(false);
    });

    it('explains when the workflow has no published versions', () => {
        hoisted.projectVersions = [];

        renderDialog();

        expect(screen.getByText('This workflow is not part of any published version yet.')).toBeInTheDocument();
    });
});
