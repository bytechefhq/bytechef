import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {fireEvent, render, screen} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import DuplicateNodeNamesBanner from './DuplicateNodeNamesBanner';

const task = (name: string): WorkflowTask => ({name, type: 'example/v1/action'}) as WorkflowTask;

const hoisted = vi.hoisted(() => ({
    setShowWorkflowCodeEditorSheet: vi.fn(),
    workflowState: {workflow: {tasks: [] as WorkflowTask[], triggers: []}},
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowDataStore', () => ({
    default: (selector: (state: typeof hoisted.workflowState) => unknown) => selector(hoisted.workflowState),
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowEditorStore', () => {
    const state = {setShowWorkflowCodeEditorSheet: hoisted.setShowWorkflowCodeEditorSheet};

    return {default: (selector: (currentState: typeof state) => unknown) => selector(state)};
});

const getDismissButton = () =>
    screen.getAllByRole('button').find((button) => !button.textContent?.includes('Open code editor'));

describe('DuplicateNodeNamesBanner', () => {
    beforeEach(() => {
        hoisted.setShowWorkflowCodeEditorSheet.mockClear();
        hoisted.workflowState.workflow = {tasks: [], triggers: []};
    });

    it('renders nothing when all node names are unique', () => {
        hoisted.workflowState.workflow = {tasks: [task('a'), task('b')], triggers: []};

        const {container} = render(<DuplicateNodeNamesBanner />);

        expect(container).toBeEmptyDOMElement();
    });

    it('renders a singular label for a single duplicate name', () => {
        hoisted.workflowState.workflow = {tasks: [task('dup'), task('dup')], triggers: []};

        render(<DuplicateNodeNamesBanner />);

        expect(screen.getByText(/Duplicate node name:/)).toBeInTheDocument();
        expect(screen.getByText('dup')).toBeInTheDocument();
    });

    it('renders a plural label and joins multiple duplicate names', () => {
        hoisted.workflowState.workflow = {tasks: [task('a'), task('a'), task('b'), task('b')], triggers: []};

        render(<DuplicateNodeNamesBanner />);

        expect(screen.getByText(/Duplicate node names:/)).toBeInTheDocument();
        expect(screen.getByText('a, b')).toBeInTheDocument();
    });

    it('opens the code editor sheet when the action button is clicked', () => {
        hoisted.workflowState.workflow = {tasks: [task('dup'), task('dup')], triggers: []};

        render(<DuplicateNodeNamesBanner />);

        fireEvent.click(screen.getByText('Open code editor'));

        expect(hoisted.setShowWorkflowCodeEditorSheet).toHaveBeenCalledWith(true);
    });

    it('hides the banner once it is dismissed', () => {
        hoisted.workflowState.workflow = {tasks: [task('dup'), task('dup')], triggers: []};

        render(<DuplicateNodeNamesBanner />);

        const dismissButton = getDismissButton();

        expect(dismissButton).toBeDefined();

        fireEvent.click(dismissButton!);

        expect(screen.queryByText(/Duplicate node name:/)).not.toBeInTheDocument();
    });
});
