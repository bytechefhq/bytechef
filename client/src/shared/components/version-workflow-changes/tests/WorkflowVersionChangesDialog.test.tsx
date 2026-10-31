import {WorkflowChangeType, WorkflowNodeChangeKind, WorkflowNodeChangeTarget} from '@/shared/middleware/graphql';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

import WorkflowVersionChangesDialog from '../WorkflowVersionChangesDialog';

vi.mock('@/shared/components/MonacoDiffEditorWrapper', () => ({
    default: ({modified, original}: {modified: string; original: string}) => (
        <div data-modified={modified} data-original={original} data-testid="mock-diff-editor" />
    ),
}));

vi.mock('@/shared/components/MonacoEditorWrapper', () => ({
    default: ({value}: {value: string}) => <div data-testid="mock-editor" data-value={value} />,
}));

vi.mock('../WorkflowChangeCanvasTab', () => ({
    WorkflowChangeCanvasToolbar: ({
        onSelectedSideChange,
        selectedSide,
    }: {
        onSelectedSideChange: (side: string) => void;
        selectedSide: string;
    }) => (
        <button data-testid="mock-canvas-toolbar" onClick={() => onSelectedSideChange('old')}>
            {selectedSide}
        </button>
    ),
    default: ({selectedSide}: {selectedSide: string}) => <div data-testid="mock-canvas-tab">{selectedSide}</div>,
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

    it('opens an added workflow on its canvas without a Changes tab and shows its definition without a diff', async () => {
        render(
            <WorkflowVersionChangesDialog
                change={{
                    ...change,
                    baseVersion: null,
                    changeType: WorkflowChangeType.Added,
                    changes: [],
                    oldDefinition: null,
                }}
                isLoading={false}
                onClose={vi.fn()}
                open
                versionLabel="V2"
            />
        );

        expect(screen.getByText('V2')).toBeInTheDocument();
        expect(screen.getAllByRole('tab').map((tab) => tab.textContent)).toEqual(['Canvas', 'Definition']);
        expect(screen.getByTestId('mock-canvas-tab')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('tab', {name: 'Definition'}));

        expect(await screen.findByTestId('mock-editor')).toHaveAttribute('data-value', '{"new":true}');
        expect(screen.queryByTestId('mock-diff-editor')).not.toBeInTheDocument();
    });

    it('opens a removed workflow on its canvas and shows its old definition without a diff', async () => {
        render(
            <WorkflowVersionChangesDialog
                baseVersionLabel="V1"
                change={{...change, changeType: WorkflowChangeType.Removed, changes: [], newDefinition: null}}
                isLoading={false}
                onClose={vi.fn()}
                open
                versionLabel="V2"
            />
        );

        expect(screen.queryByRole('tab', {name: 'Changes'})).not.toBeInTheDocument();
        expect(screen.getByTestId('mock-canvas-tab')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('tab', {name: 'Definition'}));

        expect(await screen.findByTestId('mock-editor')).toHaveAttribute('data-value', '{"old":true}');
    });

    it('shows the loading icon while loading', () => {
        render(<WorkflowVersionChangesDialog isLoading onClose={vi.fn()} open versionLabel="V2" />);

        expect(document.querySelector('svg.animate-spin')).toBeInTheDocument();
    });

    it('orders the tabs Changes, Canvas, Definition', () => {
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

        expect(screen.getAllByRole('tab').map((tab) => tab.textContent)).toEqual(['Changes', 'Canvas', 'Definition']);
    });

    it('shows the canvas toolbar in the tab row only on the Canvas tab and passes the picked side to the canvas', async () => {
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

        expect(screen.queryByTestId('mock-canvas-toolbar')).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('tab', {name: 'Canvas'}));

        expect(screen.getByTestId('mock-canvas-tab')).toHaveTextContent('new');

        await userEvent.click(screen.getByTestId('mock-canvas-toolbar'));

        expect(screen.getByTestId('mock-canvas-tab')).toHaveTextContent('old');

        await userEvent.click(screen.getByRole('tab', {name: 'Changes'}));

        expect(screen.queryByTestId('mock-canvas-toolbar')).not.toBeInTheDocument();
    });

    it('shows an error instead of the spinner when loading failed', () => {
        render(<WorkflowVersionChangesDialog isError isLoading={false} onClose={vi.fn()} open versionLabel="V2" />);

        expect(screen.getByText("Couldn't load the workflow changes.")).toBeInTheDocument();
        expect(screen.getByText('Workflow changes')).toBeInTheDocument();
        expect(document.querySelector('svg.animate-spin')).not.toBeInTheDocument();
    });
});
