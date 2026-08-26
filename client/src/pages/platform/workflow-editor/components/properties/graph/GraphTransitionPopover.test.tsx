import {TooltipProvider} from '@/components/ui/tooltip';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {GraphTransitionType} from '@/shared/types';
import {act, fireEvent, render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactNode} from 'react';
import {beforeAll, beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowDataStore from '../../../stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '../../../stores/useWorkflowNodeDetailsPanelStore';
import GraphTransitionPopover from './GraphTransitionPopover';

const {recordedProperties, saveGraphTransitionsMock, taskDispatcherDefinitionQueryMock, updateWorkflowMutationMock} =
    vi.hoisted(() => ({
        // Hoisted alongside the mocks that read it: a `vi.mock` factory runs before any module-scope
        // const is initialised, so a ref declared beside the tests would be in its temporal dead zone.
        recordedProperties: {value: [] as Array<{name?: string; props: Record<string, unknown>}>},
        saveGraphTransitionsMock: vi.fn(),
        taskDispatcherDefinitionQueryMock: vi.fn(),
        updateWorkflowMutationMock: {isPending: false, mutate: vi.fn()},
    }));

vi.mock('../../../utils/graph/saveGraphParameters', () => ({saveGraphTransitions: saveGraphTransitionsMock}));

vi.mock('../../../providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({updateWorkflowMutation: updateWorkflowMutationMock}),
}));

vi.mock('@/shared/queries/platform/taskDispatcherDefinitions.queries', () => ({
    useGetTaskDispatcherDefinitionQuery: (...args: unknown[]) => taskDispatcherDefinitionQueryMock(...args),
}));

// `Property` is the shared formula-mode editor with its own coverage; rendering it as a stub keeps
// this test about WHICH property the popover addresses, not about TipTap. The popover renders more
// than one, so each is identified by the property it was handed.
vi.mock('../Property', () => ({
    default: (propertyProps: Record<string, unknown>) => {
        const propertyName = (propertyProps.property as {name?: string} | undefined)?.name;

        recordedProperties.value = [
            ...recordedProperties.value.filter((recorded) => recorded.name !== propertyName),
            {name: propertyName, props: propertyProps},
        ];

        return <div data-testid={`${propertyName}-property`}>{propertyProps.deletePropertyButton as ReactNode}</div>;
    },
}));

const GRAPH_ID = 'graph_1';

const GRAPH_TASK_DISPATCHER_DEFINITION = {
    name: 'graph',
    properties: [
        {name: 'startNode', type: 'STRING'},
        {
            items: [
                {
                    properties: [
                        {name: 'from', type: 'STRING'},
                        {name: 'to', type: 'STRING'},
                        {controlType: 'FORMULA_MODE', label: 'Condition', name: 'condition', type: 'STRING'},
                    ],
                    type: 'OBJECT',
                },
            ],
            name: 'transitions',
            type: 'ARRAY',
        },
    ],
    version: 1,
};

function seedGraph(transitions: GraphTransitionType[], memberNames = ['task_1', 'task_2', 'task_3']) {
    const graphTask: WorkflowTask = {
        name: GRAPH_ID,
        parameters: {
            nodes: memberNames.map((memberName) => ({name: memberName, type: 'random/v1/randomInt'})),
            transitions,
        },
        type: 'graph/v1',
    };

    useWorkflowDataStore.setState({
        // One selected transition edge per declared transition would be wrong: only the edge whose
        // editor is open is selected, which is what the sole-selection guard reads.
        edges: [{id: `${GRAPH_ID}-transition-0`, selected: true, source: 'a', target: 'b', type: 'graphTransition'}],
        nodes: [
            {
                data: {componentName: 'graph', taskDispatcher: true, type: 'graph/v1', workflowNodeName: GRAPH_ID},
                id: GRAPH_ID,
                position: {x: 0, y: 0},
            },
        ],
        workflow: {nodeNames: [], tasks: [graphTask]},
    });
}

function renderPopover(transitions: GraphTransitionType[], index = 0, memberNames = ['task_1', 'task_2', 'task_3']) {
    seedGraph(transitions, memberNames);

    return render(
        <TooltipProvider>
            <GraphTransitionPopover graphId={GRAPH_ID} index={index} />
        </TooltipProvider>
    );
}

/** Applies the mutation `saveGraphTransitions` was last handed, to inspect the list it builds. */
function applyLastSavedMutation(transitions: Array<GraphTransitionType>): Array<GraphTransitionType> {
    const [graphId, mutate] = saveGraphTransitionsMock.mock.calls.at(-1)!;

    expect(graphId).toBe(GRAPH_ID);

    return (mutate as (currentTransitions: Array<GraphTransitionType>) => Array<GraphTransitionType>)(transitions);
}

describe('GraphTransitionPopover', () => {
    beforeAll(() => {
        // Radix's Select trigger uses the Pointer Capture API, which jsdom does not implement.
        Element.prototype.hasPointerCapture = vi.fn(() => false);
        Element.prototype.setPointerCapture = vi.fn();
        Element.prototype.releasePointerCapture = vi.fn();
    });

    beforeEach(() => {
        recordedProperties.value = [];
        saveGraphTransitionsMock.mockClear();
        taskDispatcherDefinitionQueryMock.mockReturnValue({data: GRAPH_TASK_DISPATCHER_DEFINITION});
        useWorkflowNodeDetailsPanelStore.setState({currentNode: undefined});
    });

    it('names the transition it edits', () => {
        renderPopover(
            [
                {from: 'task_1', to: 'task_2'},
                {condition: '=x > 1', from: 'task_2', to: 'task_3'},
            ],
            1
        );

        expect(screen.getByLabelText('Transition task_2 to task_3')).toBeInTheDocument();
        expect(screen.getByText('task_2 → task_3')).toBeInTheDocument();
    });

    it('renders nothing for a transition index the graph does not declare', () => {
        const {container} = renderPopover([{from: 'task_1', to: 'task_2'}], 4);

        expect(container).toBeEmptyDOMElement();
    });

    // The popover is anchored on the edge's selection, so dismissing it is deselecting the edge —
    // not hiding a panel that would then disagree with what the canvas shows as selected.
    it('closes by deselecting the edge it is anchored on', async () => {
        renderPopover([{from: 'task_1', to: 'task_2'}]);

        // Seeded after the render, which sets up its own edges.
        useWorkflowDataStore.setState({
            edges: [
                {id: `${GRAPH_ID}-transition-0`, selected: true, source: 'task_1', target: 'task_2'},
                {id: `${GRAPH_ID}-transition-1`, selected: true, source: 'task_2', target: 'task_3'},
            ],
        });

        await userEvent.click(screen.getByRole('button', {name: 'Close'}));

        expect(useWorkflowDataStore.getState().edges.map((edge) => edge.selected)).toEqual([false, true]);
    });

    it('removes only the transition it edits when deleted', () => {
        renderPopover(
            [
                {from: 'task_1', to: 'task_2'},
                {from: 'task_2', to: 'task_3'},
            ],
            1
        );

        fireEvent.click(screen.getByRole('button', {name: 'Delete transition'}));

        expect(
            applyLastSavedMutation([
                {from: 'task_1', to: 'task_2'},
                {from: 'task_2', to: 'task_3'},
            ])
        ).toEqual([{from: 'task_1', to: 'task_2'}]);
    });

    it('repoints the transition at the member picked from the To field', async () => {
        renderPopover([{from: 'task_1', to: 'task_2'}]);

        await userEvent.click(screen.getByRole('combobox', {name: 'To'}));

        await userEvent.click(screen.getByRole('option', {name: 'task_3'}));

        expect(applyLastSavedMutation([{from: 'task_1', to: 'task_2'}])).toEqual([{from: 'task_1', to: 'task_3'}]);
    });

    // Choosing between a node and an expression is a mode, not a destination, so it belongs on a
    // switch beside the field rather than masquerading as one more thing to route to.
    it('offers the To field only real members, never a mode disguised as one', async () => {
        renderPopover([{from: 'task_1', to: 'task_2'}]);

        await userEvent.click(screen.getByRole('combobox', {name: 'To'}));

        expect(screen.getAllByRole('option').map((option) => option.textContent)).toEqual([
            'task_1',
            'task_2',
            'task_3',
        ]);
    });

    // The expression side is the SAME editor a condition gets, so a target expression can resolve a
    // data pill rather than being typed blind into a bare text box.
    it('switches the To field to the shared formula editor addressed at this transition', async () => {
        renderPopover([{from: 'task_1', to: 'task_2'}]);

        await userEvent.click(screen.getByRole('switch', {name: 'Dynamic'}));

        expect(screen.getByTestId('to-property')).toBeInTheDocument();

        const toProperty = recordedProperties.value.find((recorded) => recorded.name === 'to');

        expect(toProperty?.props.path).toBe('transitions[0].to');
        expect(toProperty?.props.property).toMatchObject({controlType: 'FORMULA_MODE', name: 'to'});
    });

    it('opens the To field on the expression editor when the transition already carries one', () => {
        renderPopover([{from: 'task_1', to: '=nextStep'}]);

        expect(screen.getByTestId('to-property')).toBeInTheDocument();
        expect(screen.queryByRole('combobox', {name: 'To'})).not.toBeInTheDocument();
    });

    // Switching back is a change of mind about how to say it, not an instruction to forget what was
    // said: the transition keeps routing by the expression until a member is actually picked.
    it('keeps a saved expression when the To field is switched back to the member list', async () => {
        renderPopover([{from: 'task_1', to: '=nextStep'}]);

        await userEvent.click(screen.getByRole('switch', {name: 'Dynamic'}));

        expect(screen.getByRole('combobox', {name: 'To'})).toBeInTheDocument();
        expect(saveGraphTransitionsMock).not.toHaveBeenCalled();
    });

    it('edits the condition through the definition sub-property addressed at this transition', () => {
        renderPopover(
            [
                {from: 'task_1', to: 'task_2'},
                {from: 'task_2', to: 'task_3'},
            ],
            1
        );

        expect(screen.getByTestId('condition-property')).toBeInTheDocument();

        const conditionProperty = recordedProperties.value.find((recorded) => recorded.name === 'condition');

        expect(conditionProperty?.props.path).toBe('transitions[1].condition');
        expect(conditionProperty?.props.property).toMatchObject({controlType: 'FORMULA_MODE', name: 'condition'});
    });

    it('leaves the condition editor out until the definition arrives', () => {
        taskDispatcherDefinitionQueryMock.mockReturnValue({data: undefined});

        renderPopover([{from: 'task_1', to: 'task_2'}]);

        expect(screen.queryByTestId('condition-property')).not.toBeInTheDocument();
    });

    it('makes the graph the details panel current node, so the condition saves onto the graph task', () => {
        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: {componentName: 'random', name: 'task_1', workflowNodeName: 'task_1'},
        });

        renderPopover([{from: 'task_1', to: 'task_2'}]);

        expect(useWorkflowNodeDetailsPanelStore.getState().currentNode?.workflowNodeName).toBe(GRAPH_ID);
    });

    // Pointing the panel at the CONTAINER is what the condition has to save through, but it is the
    // wrong place to resolve data pills from: the outputs query anchored there returns only what
    // precedes the whole graph, so a condition could reach none of the graph's own members — not even
    // the one its transition leaves.
    it('resolves the condition data pills against the member the transition enters', () => {
        renderPopover([{from: 'task_1', to: 'task_2'}]);

        expect(useWorkflowNodeDetailsPanelStore.getState().currentNode?.dataPillAnchorNodeName).toBe('task_2');
    });

    // A target written as an expression names no member to resolve against, so the panel is left on
    // the container rather than pointed at something that only looks like a node name.
    it('leaves the data pill anchor unset when the target is an expression', () => {
        renderPopover([{from: 'task_1', to: '=nextStep'}]);

        expect(useWorkflowNodeDetailsPanelStore.getState().currentNode?.dataPillAnchorNodeName).toBeUndefined();
    });

    it('refreshes the graph parameter snapshot when a transition is removed while it is open', () => {
        renderPopover(
            [
                {condition: '=a', from: 'task_1', to: 'task_2'},
                {condition: '=b', from: 'task_1', to: 'task_3'},
            ],
            1
        );

        // The Transitions panel deletes index 0. `saveGraphParameters` persists through
        // `saveWorkflowDefinition`, which never touches the panel store — so only re-pointing keeps
        // the snapshot honest, and the surviving transition is now index 0.
        act(() => {
            seedGraph([{condition: '=b', from: 'task_1', to: 'task_3'}]);
        });

        expect(useWorkflowNodeDetailsPanelStore.getState().currentNode?.parameters?.transitions).toEqual([
            {condition: '=b', from: 'task_1', to: 'task_3'},
        ]);
    });

    it('replaces a stale graph snapshot the details panel already held, keeping its other fields', () => {
        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: {
                componentName: 'graph',
                label: 'My graph',
                name: GRAPH_ID,
                parameters: {
                    transitions: [
                        {condition: '=a', from: 'task_1', to: 'task_2'},
                        {condition: '=b', from: 'task_1', to: 'task_3'},
                    ],
                },
                workflowNodeName: GRAPH_ID,
            },
        });

        renderPopover([{condition: '=b', from: 'task_1', to: 'task_3'}]);

        const {currentNode} = useWorkflowNodeDetailsPanelStore.getState();

        expect(currentNode?.parameters?.transitions).toEqual([{condition: '=b', from: 'task_1', to: 'task_3'}]);
        expect(currentNode?.label).toBe('My graph');
    });

    it('gives the details panel back the node it displaced when the edge is deselected', () => {
        const displacedNode = {componentName: 'random', name: 'task_1', workflowNodeName: 'task_1'};

        useWorkflowNodeDetailsPanelStore.setState({currentNode: displacedNode});

        const {unmount} = renderPopover([{from: 'task_1', to: 'task_2'}]);

        expect(useWorkflowNodeDetailsPanelStore.getState().currentNode?.workflowNodeName).toBe(GRAPH_ID);

        unmount();

        expect(useWorkflowNodeDetailsPanelStore.getState().currentNode).toBe(displacedNode);
    });

    it('leaves the shared current-node slot alone while more than one transition is selected', () => {
        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: {componentName: 'random', name: 'task_1', workflowNodeName: 'task_1'},
        });

        seedGraph([{from: 'task_1', to: 'task_2'}]);

        useWorkflowDataStore.setState({
            edges: [
                {id: 'graph_1-transition-0', selected: true, source: 'a', target: 'b', type: 'graphTransition'},
                {id: 'graph_2-transition-0', selected: true, source: 'c', target: 'd', type: 'graphTransition'},
            ],
        });

        render(
            <TooltipProvider>
                <GraphTransitionPopover graphId={GRAPH_ID} index={0} />
            </TooltipProvider>
        );

        expect(useWorkflowNodeDetailsPanelStore.getState().currentNode?.workflowNodeName).toBe('task_1');
        expect(screen.queryByTestId('condition-property')).not.toBeInTheDocument();
        expect(screen.getByText('Select a single transition to edit its condition.')).toBeInTheDocument();
    });
});
