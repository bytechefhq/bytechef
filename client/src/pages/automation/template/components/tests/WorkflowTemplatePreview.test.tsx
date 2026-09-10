import {Workflow} from '@/shared/middleware/platform/configuration';
import {act, render, resetAll, screen} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import WorkflowTemplatePreview from '../WorkflowTemplatePreview';

const {editorPropsMock, resizeCallbacks, useComponentDefinitionsMock, useTaskDispatcherDefinitionsMock} = vi.hoisted(
    () => ({
        editorPropsMock: vi.fn(),
        resizeCallbacks: [] as ((entries: {contentRect: {width: number}}[]) => void)[],
        useComponentDefinitionsMock: vi.fn(),
        useTaskDispatcherDefinitionsMock: vi.fn(),
    })
);

vi.mock('@/shared/queries/automation/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: useComponentDefinitionsMock,
}));

vi.mock('@/shared/queries/platform/taskDispatcherDefinitions.queries', () => ({
    useGetTaskDispatcherDefinitionsQuery: useTaskDispatcherDefinitionsMock,
}));

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    WorkflowReadOnlyProvider: ({children}: {children: React.ReactNode}) => <>{children}</>,
}));

vi.mock('@xyflow/react', () => ({
    ReactFlowProvider: ({children}: {children: React.ReactNode}) => <>{children}</>,
}));

vi.mock('@/pages/platform/workflow-editor/components/WorkflowEditor', () => ({
    default: (props: {customCanvasWidth: number; onFitView: () => void}) => {
        editorPropsMock(props);

        return (
            <button data-testid="workflow-editor" onClick={props.onFitView} type="button">
                editor
            </button>
        );
    },
}));

const disconnectMock = vi.fn();

class MockResizeObserver {
    constructor(callback: (entries: {contentRect: {width: number}}[]) => void) {
        resizeCallbacks.push(callback);
    }
    disconnect = disconnectMock;
    observe() {}
    unobserve() {}
}

const workflow = {id: 'wf-1', label: 'Sync contacts'} as Workflow;

const resizeTo = (width: number) => act(() => resizeCallbacks.forEach((cb) => cb([{contentRect: {width}}])));

describe('WorkflowTemplatePreview', () => {
    beforeEach(() => {
        resizeCallbacks.length = 0;

        (window as unknown as {ResizeObserver: unknown}).ResizeObserver = MockResizeObserver;

        useComponentDefinitionsMock.mockReturnValue({data: [{name: 'slack'}], error: null, isLoading: false});
        useTaskDispatcherDefinitionsMock.mockReturnValue({data: [{name: 'condition'}], error: null, isLoading: false});
    });

    afterEach(() => {
        resetAll();
    });

    it('waits for the canvas to report a width before mounting the editor', () => {
        render(<WorkflowTemplatePreview workflow={workflow} />);

        expect(screen.queryByTestId('workflow-editor')).not.toBeInTheDocument();
    });

    it('mounts the editor once the canvas has a width', async () => {
        render(<WorkflowTemplatePreview workflow={workflow} />);

        resizeTo(800);

        expect(await screen.findByTestId('workflow-editor')).toBeInTheDocument();
    });

    it('passes the measured width and the workflow down to the editor', async () => {
        render(<WorkflowTemplatePreview workflow={workflow} />);

        resizeTo(640);

        await screen.findByTestId('workflow-editor');

        expect(editorPropsMock).toHaveBeenCalledWith(
            expect.objectContaining({customCanvasWidth: 640, preview: true, readOnlyWorkflow: workflow})
        );
    });

    it('keeps the canvas hidden until the editor reports it has fitted the view', async () => {
        const {container} = render(<WorkflowTemplatePreview workflow={workflow} />);

        resizeTo(800);

        await screen.findByTestId('workflow-editor');

        expect(container.querySelector('.opacity-0')).toBeInTheDocument();

        act(() => screen.getByTestId('workflow-editor').click());

        expect(container.querySelector('.opacity-100')).toBeInTheDocument();
    });

    it('does not mount the editor while the definitions are still loading', () => {
        useComponentDefinitionsMock.mockReturnValue({data: undefined, error: null, isLoading: true});

        render(<WorkflowTemplatePreview workflow={workflow} />);

        resizeTo(800);

        expect(screen.queryByTestId('workflow-editor')).not.toBeInTheDocument();
    });

    it('stops observing the canvas when it unmounts', () => {
        const {unmount} = render(<WorkflowTemplatePreview workflow={workflow} />);

        unmount();

        expect(disconnectMock).toHaveBeenCalled();
    });
});
