import {WorkflowChangeType, WorkflowNodeChangeKind, WorkflowNodeChangeTarget} from '@/shared/middleware/graphql';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

import WorkflowVersionChangesDialog from '../WorkflowVersionChangesDialog';

vi.mock('@/shared/components/MonacoDiffEditorWrapper', () => ({
    default: ({modified, original}: {modified: string; original: string}) => (
        <div data-modified={modified} data-original={original} data-testid="mock-diff-editor" />
    ),
}));

vi.mock('../WorkflowChangeCanvasTab', () => ({
    default: ({versionLabel}: {versionLabel: string}) => <div data-testid="mock-canvas-tab">{versionLabel}</div>,
}));

const change = {
    baseVersion: 1,
    changeType: WorkflowChangeType.Modified,
    changes: [
        {
            kind: WorkflowNodeChangeKind.Modified,
            label: null,
            name: 'http_1',
            parentPath: null,
            target: WorkflowNodeChangeTarget.Task,
            type: 'http/v1/get',
            valueChanges: [{newValue: '"b"', oldValue: '"a"', path: 'parameters.url'}],
        },
    ],
    label: 'My workflow',
    newDefinition: '{"new":true}',
    oldDefinition: '{"old":true}',
    version: 2,
    workflowUuid: 'uuid-1',
};

describe('WorkflowVersionChangesDialog', () => {
    it('shows the workflow label and the version range in the header', () => {
        render(
            <WorkflowVersionChangesDialog
                baseVersionLabel="V1"
                change={change}
                isLoading={false}
                onClose={vi.fn()}
                open
                versionLabel="V2"
            />
        );

        expect(screen.getByText('My workflow')).toBeInTheDocument();
        expect(screen.getByText('V1 → V2')).toBeInTheDocument();
    });

    it('renders the structured changes on the Changes tab', () => {
        render(
            <WorkflowVersionChangesDialog
                baseVersionLabel="V1"
                change={change}
                isLoading={false}
                onClose={vi.fn()}
                open
                versionLabel="V2"
            />
        );

        expect(screen.getByText('http_1')).toBeInTheDocument();
        expect(screen.getByText('"a" → "b"')).toBeInTheDocument();
    });

    it('passes old and new definitions to the diff editor on the Definition tab', async () => {
        render(
            <WorkflowVersionChangesDialog
                baseVersionLabel="V1"
                change={change}
                isLoading={false}
                onClose={vi.fn()}
                open
                versionLabel="V2"
            />
        );

        await userEvent.click(screen.getByRole('tab', {name: 'Definition'}));

        const diffEditor = await screen.findByTestId('mock-diff-editor');

        expect(diffEditor).toHaveAttribute('data-original', '{"old":true}');
        expect(diffEditor).toHaveAttribute('data-modified', '{"new":true}');
    });

    it('renders an added workflow without a baseline', async () => {
        render(
            <WorkflowVersionChangesDialog
                change={{...change, baseVersion: null, changeType: WorkflowChangeType.Added, oldDefinition: null}}
                isLoading={false}
                onClose={vi.fn()}
                open
                versionLabel="V2"
            />
        );

        expect(screen.getByText('V2')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('tab', {name: 'Definition'}));

        expect(await screen.findByTestId('mock-diff-editor')).toHaveAttribute('data-original', '');
    });

    it('shows the loading icon while loading', () => {
        render(<WorkflowVersionChangesDialog isLoading onClose={vi.fn()} open versionLabel="V2" />);

        expect(document.querySelector('svg.animate-spin')).toBeInTheDocument();
    });

    it('shows the canvas tab', async () => {
        render(
            <WorkflowVersionChangesDialog
                baseVersionLabel="V1"
                change={change}
                isLoading={false}
                onClose={vi.fn()}
                open
                versionLabel="V2"
            />
        );

        await userEvent.click(screen.getByRole('tab', {name: 'Canvas'}));

        expect(screen.getByTestId('mock-canvas-tab')).toHaveTextContent('V2');
    });

    it('shows an error instead of the spinner when loading failed', () => {
        render(<WorkflowVersionChangesDialog isError isLoading={false} onClose={vi.fn()} open versionLabel="V2" />);

        expect(screen.getByText("Couldn't load the workflow changes.")).toBeInTheDocument();
        expect(screen.getByText('Workflow changes')).toBeInTheDocument();
        expect(document.querySelector('svg.animate-spin')).not.toBeInTheDocument();
    });

    it('says an added workflow was added on the Changes tab', () => {
        render(
            <WorkflowVersionChangesDialog
                change={{...change, baseVersion: null, changeType: WorkflowChangeType.Added, changes: []}}
                isLoading={false}
                onClose={vi.fn()}
                open
                versionLabel="V2"
            />
        );

        expect(screen.getByText('Workflow added in this version.')).toBeInTheDocument();
    });
});
