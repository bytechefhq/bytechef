import {
    VersionWorkflowChange,
    WorkflowChangeType,
    WorkflowNodeChangeKind,
    WorkflowNodeChangeTarget,
} from '@/shared/middleware/graphql';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

import WorkflowChangeCanvasTab from '../WorkflowChangeCanvasTab';

vi.mock('../WorkflowChangeCanvas', () => ({
    default: ({highlights, workflow}: {highlights: Record<string, string>; workflow: {label: string}}) => (
        <div
            data-highlights={JSON.stringify(highlights)}
            data-label={workflow.label}
            data-testid="mock-change-canvas"
        />
    ),
}));

vi.mock('@/shared/queries/automation/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: () => ({data: []}),
}));

vi.mock('@/shared/queries/platform/taskDispatcherDefinitions.queries', () => ({
    useGetTaskDispatcherDefinitionsQuery: () => ({data: []}),
}));

const entry = (
    kind: WorkflowNodeChangeKind,
    name: string,
    target = WorkflowNodeChangeTarget.Task,
    parentPath: string | null = null
) => ({kind, label: null, name, parentPath, target, type: 'http/v1/get', valueChanges: []});

const BASE_CHANGE: VersionWorkflowChange = {
    baseVersion: 1,
    changeType: WorkflowChangeType.Modified,
    changes: [
        entry(WorkflowNodeChangeKind.Added, 'added_1'),
        entry(WorkflowNodeChangeKind.Modified, 'modified_1'),
        entry(WorkflowNodeChangeKind.Moved, 'moved_1', WorkflowNodeChangeTarget.Trigger),
        entry(WorkflowNodeChangeKind.Removed, 'removed_1'),
        entry(WorkflowNodeChangeKind.Modified, 'input_1', WorkflowNodeChangeTarget.Input),
    ],
    label: 'Workflow',
    newDefinition: '{"label":"New label","tasks":[]}',
    oldDefinition: '{"label":"Old label","tasks":[]}',
    version: 2,
    workflowUuid: 'uuid-1',
};

const getHighlights = () => JSON.parse(screen.getByTestId('mock-change-canvas').getAttribute('data-highlights')!);

describe('WorkflowChangeCanvasTab', () => {
    it('shows the new canvas with added, modified and moved highlights by default', () => {
        render(<WorkflowChangeCanvasTab baseVersionLabel="V1" change={BASE_CHANGE} versionLabel="V2" />);

        expect(screen.getByTestId('mock-change-canvas')).toHaveAttribute('data-label', 'New label');
        expect(getHighlights()).toEqual({added_1: 'ADDED', modified_1: 'MODIFIED', moved_1: 'MOVED'});
    });

    it('switches to the old canvas with removed, modified and moved highlights', async () => {
        render(<WorkflowChangeCanvasTab baseVersionLabel="V1" change={BASE_CHANGE} versionLabel="V2" />);

        await userEvent.click(screen.getByRole('button', {name: 'V1'}));

        expect(screen.getByTestId('mock-change-canvas')).toHaveAttribute('data-label', 'Old label');
        expect(getHighlights()).toEqual({modified_1: 'MODIFIED', moved_1: 'MOVED', removed_1: 'REMOVED'});
    });

    it('hides the toggle for an added workflow', () => {
        render(
            <WorkflowChangeCanvasTab
                change={{...BASE_CHANGE, baseVersion: null, changeType: WorkflowChangeType.Added, oldDefinition: null}}
                versionLabel="V2"
            />
        );

        expect(screen.queryByRole('button', {name: 'V2'})).not.toBeInTheDocument();
        expect(screen.getByTestId('mock-change-canvas')).toHaveAttribute('data-label', 'New label');
    });

    it('hides the toggle and shows the old canvas for a removed workflow', () => {
        render(
            <WorkflowChangeCanvasTab
                baseVersionLabel="V1"
                change={{...BASE_CHANGE, changeType: WorkflowChangeType.Removed, newDefinition: null}}
                versionLabel="V2"
            />
        );

        expect(screen.queryByRole('button', {name: 'V1'})).not.toBeInTheDocument();
        expect(screen.getByTestId('mock-change-canvas')).toHaveAttribute('data-label', 'Old label');
    });

    it('highlights the root task of a changed cluster element as modified unless the task has its own change', () => {
        render(
            <WorkflowChangeCanvasTab
                baseVersionLabel="V1"
                change={{
                    ...BASE_CHANGE,
                    changes: [
                        entry(
                            WorkflowNodeChangeKind.Added,
                            'tools_1',
                            WorkflowNodeChangeTarget.ClusterElement,
                            'agent_1.clusterElements.tools'
                        ),
                        entry(
                            WorkflowNodeChangeKind.Modified,
                            'model_1',
                            WorkflowNodeChangeTarget.ClusterElement,
                            'agent_2.clusterElements.model'
                        ),
                        entry(WorkflowNodeChangeKind.Added, 'agent_2'),
                    ],
                }}
                versionLabel="V2"
            />
        );

        expect(getHighlights()).toEqual({agent_1: 'MODIFIED', agent_2: 'ADDED'});
    });

    it('renders no canvas for a malformed definition', () => {
        render(
            <WorkflowChangeCanvasTab
                change={{...BASE_CHANGE, baseVersion: null, newDefinition: 'null', oldDefinition: null}}
                versionLabel="V2"
            />
        );

        expect(screen.queryByTestId('mock-change-canvas')).not.toBeInTheDocument();
    });
});
