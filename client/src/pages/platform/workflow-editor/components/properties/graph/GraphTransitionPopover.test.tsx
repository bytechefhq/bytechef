import {TooltipProvider} from '@/components/ui/tooltip';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {GraphTransitionType} from '@/shared/types';
import {act, fireEvent, render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactNode} from 'react';
import {beforeAll, beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowDataStore from '../../../stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '../../../stores/useWorkflowNodeDetailsPanelStore';
import deleteProperty from '../../../utils/deleteProperty';
import {getPropertySaveOverride} from '../../../utils/propertySaveOverrides';
import saveProperty from '../../../utils/saveProperty';
import GraphTransitionPopover from './GraphTransitionPopover';

const {recordedProperties, saveGraphTransitionsMock, taskDispatcherDefinitionQueryMock, updateWorkflowMutationMock} =
    vi.hoisted(() => ({
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

interface RenderPopoverProps {
    index?: number;
    memberNames?: string[];
    transitions: GraphTransitionType[];
}

function renderPopover({index = 0, memberNames = ['task_1', 'task_2', 'task_3'], transitions}: RenderPopoverProps) {
    seedGraph(transitions, memberNames);

    return render(
        <TooltipProvider>
            <GraphTransitionPopover graphId={GRAPH_ID} index={index} />
        </TooltipProvider>
    );
}

function applyLastSavedMutation(transitions: Array<GraphTransitionType>): Array<GraphTransitionType> {
    const [{graphId, mutate}] = saveGraphTransitionsMock.mock.calls.at(-1)!;

    expect(graphId).toBe(GRAPH_ID);

    return (mutate as (currentTransitions: Array<GraphTransitionType>) => Array<GraphTransitionType>)(transitions);
}

describe('GraphTransitionPopover', () => {
    beforeAll(() => {
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
        renderPopover({
            index: 1,
            transitions: [
                {from: 'task_1', to: 'task_2'},
                {condition: '=x > 1', from: 'task_2', to: 'task_3'},
            ],
        });

        expect(screen.getByLabelText('Transition task_2 to task_3')).toBeInTheDocument();
        expect(screen.getByText('task_2 → task_3')).toBeInTheDocument();
    });

    it('renders nothing for a transition index the graph does not declare', () => {
        const {container} = renderPopover({index: 4, transitions: [{from: 'task_1', to: 'task_2'}]});

        expect(container).toBeEmptyDOMElement();
    });

    it('closes by deselecting the edge it is anchored on', async () => {
        renderPopover({transitions: [{from: 'task_1', to: 'task_2'}]});

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
        renderPopover({
            index: 1,
            transitions: [
                {from: 'task_1', to: 'task_2'},
                {from: 'task_2', to: 'task_3'},
            ],
        });

        fireEvent.click(screen.getByRole('button', {name: 'Delete transition'}));

        expect(
            applyLastSavedMutation([
                {from: 'task_1', to: 'task_2'},
                {from: 'task_2', to: 'task_3'},
            ])
        ).toEqual([{from: 'task_1', to: 'task_2'}]);
    });

    it('repoints the transition at the member picked from the To field', async () => {
        renderPopover({transitions: [{from: 'task_1', to: 'task_2'}]});

        await userEvent.click(screen.getByRole('combobox', {name: 'To'}));

        await userEvent.click(screen.getByRole('option', {name: 'task_3'}));

        expect(applyLastSavedMutation([{from: 'task_1', to: 'task_2'}])).toEqual([{from: 'task_1', to: 'task_3'}]);
    });

    it('offers the To field only real members, never a mode disguised as one', async () => {
        renderPopover({transitions: [{from: 'task_1', to: 'task_2'}]});

        await userEvent.click(screen.getByRole('combobox', {name: 'To'}));

        expect(screen.getAllByRole('option').map((option) => option.textContent)).toEqual([
            'task_1',
            'task_2',
            'task_3',
        ]);
    });

    it('switches the To field to the shared formula editor addressed at this transition', async () => {
        renderPopover({transitions: [{from: 'task_1', to: 'task_2'}]});

        await userEvent.click(screen.getByRole('switch', {name: 'Formula'}));

        expect(screen.getByTestId('to-property')).toBeInTheDocument();

        const toProperty = recordedProperties.value.find((recorded) => recorded.name === 'to');

        expect(toProperty?.props.path).toBe('transitions[0].to');
        expect(toProperty?.props.property).toMatchObject({controlType: 'FORMULA_MODE', name: 'to'});
    });

    it('opens the To field on the expression editor when the transition already carries one', () => {
        renderPopover({transitions: [{from: 'task_1', to: '=nextStep'}]});

        expect(screen.getByTestId('to-property')).toBeInTheDocument();
        expect(screen.queryByRole('combobox', {name: 'To'})).not.toBeInTheDocument();
    });

    it('keeps a saved expression when the To field is switched back to the member list', async () => {
        renderPopover({transitions: [{from: 'task_1', to: '=nextStep'}]});

        await userEvent.click(screen.getByRole('switch', {name: 'Formula'}));

        expect(screen.getByRole('combobox', {name: 'To'})).toBeInTheDocument();
        expect(saveGraphTransitionsMock).not.toHaveBeenCalled();
    });

    it('edits the condition through the definition sub-property addressed at this transition', () => {
        renderPopover({
            index: 1,
            transitions: [
                {from: 'task_1', to: 'task_2'},
                {from: 'task_2', to: 'task_3'},
            ],
        });

        expect(screen.getByTestId('condition-property')).toBeInTheDocument();

        const conditionProperty = recordedProperties.value.find((recorded) => recorded.name === 'condition');

        expect(conditionProperty?.props.path).toBe('transitions[1].condition');
        expect(conditionProperty?.props.property).toMatchObject({controlType: 'FORMULA_MODE', name: 'condition'});
    });

    it('leaves the condition editor out until the definition arrives', () => {
        taskDispatcherDefinitionQueryMock.mockReturnValue({data: undefined});

        renderPopover({transitions: [{from: 'task_1', to: 'task_2'}]});

        expect(screen.queryByTestId('condition-property')).not.toBeInTheDocument();
    });

    it('makes the graph the details panel current node, so the condition saves onto the graph task', () => {
        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: {componentName: 'random', name: 'task_1', workflowNodeName: 'task_1'},
        });

        renderPopover({transitions: [{from: 'task_1', to: 'task_2'}]});

        expect(useWorkflowNodeDetailsPanelStore.getState().currentNode?.workflowNodeName).toBe(GRAPH_ID);
    });

    it('resolves the condition data pills against the member the transition leaves, which has just run', () => {
        renderPopover({transitions: [{from: 'task_1', to: 'task_2'}]});

        expect(useWorkflowNodeDetailsPanelStore.getState().currentNode?.dataPillAnchorNodeName).toBe('task_1');
    });

    it('resolves a loop-back transition against the member it leaves', () => {
        renderPopover({
            index: 1,
            transitions: [
                {from: 'task_1', to: 'task_2'},
                {from: 'task_2', to: 'task_1'},
            ],
        });

        expect(useWorkflowNodeDetailsPanelStore.getState().currentNode?.dataPillAnchorNodeName).toBe('task_2');
    });

    it('anchors a transition with an expression target at the member it leaves', () => {
        renderPopover({transitions: [{from: 'task_1', to: '=nextStep'}]});

        expect(useWorkflowNodeDetailsPanelStore.getState().currentNode?.dataPillAnchorNodeName).toBe('task_1');
    });

    it('refreshes the graph parameter snapshot when a transition is removed while it is open', () => {
        renderPopover({
            index: 1,
            transitions: [
                {condition: '=a', from: 'task_1', to: 'task_2'},
                {condition: '=b', from: 'task_1', to: 'task_3'},
            ],
        });

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

        renderPopover({transitions: [{condition: '=b', from: 'task_1', to: 'task_3'}]});

        const {currentNode} = useWorkflowNodeDetailsPanelStore.getState();

        expect(currentNode?.parameters?.transitions).toEqual([{condition: '=b', from: 'task_1', to: 'task_3'}]);
        expect(currentNode?.label).toBe('My graph');
    });

    it('gives the details panel back the node it displaced when the edge is deselected', () => {
        const displacedNode = {componentName: 'random', name: 'task_1', workflowNodeName: 'task_1'};

        useWorkflowNodeDetailsPanelStore.setState({currentNode: displacedNode});

        const {unmount} = renderPopover({transitions: [{from: 'task_1', to: 'task_2'}]});

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

    it('saves a condition edit through the guarded graph queue, never as a by-index node parameter write', () => {
        const transitions: GraphTransitionType[] = [
            {from: 'task_1', to: 'task_2'},
            {condition: '=x > 1', from: 'task_2', to: 'task_3'},
        ];
        const mutateAsync = vi.fn();

        renderPopover({index: 1, transitions});

        saveProperty({
            path: 'transitions[1].condition',
            type: 'STRING',
            updateWorkflowNodeParameterMutation: {mutateAsync} as never,
            value: '=x > 2',
            workflowId: 'workflow-1',
        });

        expect(mutateAsync).not.toHaveBeenCalled();
        expect(saveGraphTransitionsMock).toHaveBeenCalledTimes(1);
        expect(applyLastSavedMutation(transitions)).toEqual([
            {from: 'task_1', to: 'task_2'},
            {condition: '=x > 2', from: 'task_2', to: 'task_3'},
        ]);
    });

    it('drops a condition edit that runs after a delete shifted another transition into its index', () => {
        const transitions: GraphTransitionType[] = [
            {from: 'task_1', to: 'task_2'},
            {condition: '=x > 1', from: 'task_2', to: 'task_3'},
            {condition: '=y', from: 'task_3', to: 'task_1'},
        ];

        renderPopover({index: 1, transitions});

        getPropertySaveOverride(GRAPH_ID, 'transitions[1].condition')!('=x > 2');

        const transitionsAfterDelete = [transitions[0], transitions[2]];

        expect(applyLastSavedMutation(transitionsAfterDelete)).toBe(transitionsAfterDelete);
    });

    it('keeps every keystroke of a condition typed faster than the saves land', () => {
        const transitions: GraphTransitionType[] = [{condition: '=x', from: 'task_1', to: 'task_2'}];

        renderPopover({transitions});

        const saveCondition = getPropertySaveOverride(GRAPH_ID, 'transitions[0].condition')!;

        saveCondition('=x1');
        saveCondition('=x12');

        const [[firstSave], [secondSave]] = saveGraphTransitionsMock.mock.calls as Array<
            [{mutate: (currentTransitions: Array<GraphTransitionType>) => Array<GraphTransitionType>}]
        >;

        const transitionsAfterFirstSave = firstSave.mutate(transitions);

        expect(transitionsAfterFirstSave).toEqual([{condition: '=x1', from: 'task_1', to: 'task_2'}]);
        expect(secondSave.mutate(transitionsAfterFirstSave)).toEqual([
            {condition: '=x12', from: 'task_1', to: 'task_2'},
        ]);
    });

    it('saves an expression To and a cleared condition through the guarded graph queue', () => {
        const transitions: GraphTransitionType[] = [{condition: '=x', from: 'task_1', to: 'task_2'}];
        const deleteMutateAsync = vi.fn();

        renderPopover({transitions});

        getPropertySaveOverride(GRAPH_ID, 'transitions[0].to')!('=${next}');

        expect(applyLastSavedMutation(transitions)).toEqual([{condition: '=x', from: 'task_1', to: '=${next}'}]);

        deleteProperty('workflow-1', 'transitions[0].condition', {mutateAsync: deleteMutateAsync} as never);

        expect(deleteMutateAsync).not.toHaveBeenCalled();
        expect(applyLastSavedMutation(transitions)).toEqual([{condition: undefined, from: 'task_1', to: 'task_2'}]);
    });

    it('stops intercepting the transition paths once it closes', () => {
        const {unmount} = renderPopover({transitions: [{from: 'task_1', to: 'task_2'}]});

        expect(getPropertySaveOverride(GRAPH_ID, 'transitions[0].condition')).toBeDefined();

        unmount();

        expect(getPropertySaveOverride(GRAPH_ID, 'transitions[0].condition')).toBeUndefined();
        expect(getPropertySaveOverride(GRAPH_ID, 'transitions[0].to')).toBeUndefined();
    });
});
