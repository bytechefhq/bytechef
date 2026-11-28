import {TooltipProvider} from '@/components/Tooltip/Tooltip';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {NodeDataType, PropertyAllType} from '@/shared/types';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ComponentProps} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {ClusterElementContextI, ClusterElementProvider} from '../../ClusterElementContext';
import PropertyMultiSelect from '../PropertyMultiSelect';

const hoisted = vi.hoisted(() => ({
    clusterElementOptionsQuery: vi.fn(),
    contextClusterElementOptionsQuery: vi.fn(),
    workflowNodeOptionsQuery: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async () => {
    const actual = await vi.importActual<typeof import('@/shared/middleware/graphql')>('@/shared/middleware/graphql');

    return {
        ...actual,
        useClusterElementOptionsQuery: (...args: unknown[]) => hoisted.contextClusterElementOptionsQuery(...args),
    };
});

vi.mock('@/shared/queries/platform/workflowNodeOptions.queries', () => ({
    useGetClusterElementNodeOptionsQuery: (...args: unknown[]) => hoisted.clusterElementOptionsQuery(...args),
    useGetWorkflowNodeOptionsQuery: (...args: unknown[]) => hoisted.workflowNodeOptionsQuery(...args),
}));

const PROPERTY = {
    description: 'Channels to post to',
    label: 'Channels',
    name: 'channels',
    placeholder: 'Pick channels',
    required: true,
} as PropertyAllType;

const STATIC_OPTIONS = [
    {label: 'General', value: 'general'},
    {label: 'Random', value: 'random'},
];

const OPTIONS_DATA_SOURCE = {optionsLookupDependsOn: []};

type PropsType = ComponentProps<typeof PropertyMultiSelect>;

const renderMultiSelect = (props: Partial<PropsType> = {}) => {
    const onChange = vi.fn();

    const view = render(
        <TooltipProvider>
            <PropertyMultiSelect
                deletePropertyButton={<button type="button">Delete property</button>}
                onChange={onChange}
                path="parameters.channels"
                property={PROPERTY}
                showInputTypeSwitchButton={false}
                value={[]}
                workflowId="workflow-1"
                {...props}
            />
        </TooltipProvider>
    );

    return {onChange, view};
};

const getTrigger = () => screen.getAllByRole('button').find((button) => button.getAttribute('aria-haspopup'))!;

const lastQueryCall = (query: ReturnType<typeof vi.fn>) => query.mock.calls[query.mock.calls.length - 1];

const setCurrentNode = (currentNode: Partial<NodeDataType>) =>
    useWorkflowNodeDetailsPanelStore.setState({
        currentNode: {name: 'slack_1', workflowNodeName: 'slack_1', ...currentNode} as NodeDataType,
    });

beforeEach(() => {
    environmentStore.setState({currentEnvironmentId: 7});

    useWorkflowEditorStore.setState({rootClusterElementNodeData: undefined});

    useWorkflowNodeDetailsPanelStore.setState({operationChangeInProgress: false});

    setCurrentNode({});

    hoisted.clusterElementOptionsQuery.mockReset();
    hoisted.clusterElementOptionsQuery.mockReturnValue({data: undefined, isLoading: false});

    hoisted.workflowNodeOptionsQuery.mockReset();
    hoisted.workflowNodeOptionsQuery.mockReturnValue({data: undefined, isLoading: false});

    hoisted.contextClusterElementOptionsQuery.mockReset();
    hoisted.contextClusterElementOptionsQuery.mockReturnValue({data: undefined, isLoading: false});
});

describe('PropertyMultiSelect inside a cluster element form outside the workflow editor', () => {
    const CLUSTER_ELEMENT_CONTEXT: ClusterElementContextI = {
        clusterElementName: 'mcpTools',
        componentName: 'mcpClient',
        componentVersion: 1,
        connectionId: 5,
        connectionRequired: true,
        inputParameters: {toolFilterType: 'INCLUDE'},
    };

    const TOOLS_PROPERTY = {label: 'Tools to Include', name: 'toolsToInclude'} as PropertyAllType;

    const renderInClusterElementForm = (clusterElementContext: ClusterElementContextI = CLUSTER_ELEMENT_CONTEXT) =>
        render(
            <TooltipProvider>
                <ClusterElementProvider value={clusterElementContext}>
                    <PropertyMultiSelect
                        deletePropertyButton={null}
                        lookupDependsOnPaths={['toolFilterType']}
                        optionsDataSource={{optionsLookupDependsOn: ['toolFilterType']}}
                        path="toolsToInclude"
                        property={TOOLS_PROPERTY}
                        showInputTypeSwitchButton={false}
                        value={[]}
                        workflowId=""
                    />
                </ClusterElementProvider>
            </TooltipProvider>
        );

    beforeEach(() => {
        useWorkflowNodeDetailsPanelStore.setState({currentNode: undefined});
    });

    it('loads the options by component, cluster element and connection', async () => {
        hoisted.contextClusterElementOptionsQuery.mockReturnValue({
            data: {clusterElementOptions: [{label: 'notion-search', value: 'notion-search'}]},
            isLoading: false,
        });

        renderInClusterElementForm();

        const [variables, queryOptions] = lastQueryCall(hoisted.contextClusterElementOptionsQuery);

        expect(variables).toEqual({
            clusterElementName: 'mcpTools',
            componentName: 'mcpClient',
            componentVersion: 1,
            connectionId: 5,
            inputParameters: {toolFilterType: 'INCLUDE'},
            lookupDependsOnPaths: ['toolFilterType'],
            propertyName: 'toolsToInclude',
        });
        expect(queryOptions.enabled).toBe(true);

        await userEvent.click(getTrigger());

        expect(await screen.findByText('notion-search')).toBeInTheDocument();
    });

    it('says the options are loading while the slow server answers', () => {
        hoisted.contextClusterElementOptionsQuery.mockReturnValue({data: undefined, isLoading: true});

        renderInClusterElementForm();

        expect(screen.getByRole('status')).toHaveTextContent('Loading options...');
        expect(getTrigger()).toBeDisabled();
    });

    it('does not say the options are loading while the query waits for a connection', () => {
        hoisted.contextClusterElementOptionsQuery.mockReturnValue({data: undefined, isLoading: true});

        renderInClusterElementForm({...CLUSTER_ELEMENT_CONTEXT, connectionId: undefined});

        expect(screen.queryByRole('status')).not.toBeInTheDocument();
    });

    it('shows the default placeholder once the options are loaded', () => {
        hoisted.contextClusterElementOptionsQuery.mockReturnValue({
            data: {clusterElementOptions: [{label: 'notion-search', value: 'notion-search'}]},
            isLoading: false,
        });

        renderInClusterElementForm();

        expect(getTrigger()).toHaveTextContent('Select...');
        expect(getTrigger()).toBeEnabled();
    });

    it('waits for a required connection', () => {
        renderInClusterElementForm({...CLUSTER_ELEMENT_CONTEXT, connectionId: undefined});

        expect(lastQueryCall(hoisted.contextClusterElementOptionsQuery)[1].enabled).toBe(false);
    });

    it('waits for the properties its options depend on', () => {
        renderInClusterElementForm({...CLUSTER_ELEMENT_CONTEXT, inputParameters: {}});

        expect(lastQueryCall(hoisted.contextClusterElementOptionsQuery)[1].enabled).toBe(false);
    });

    it('leaves the options to the workflow node queries inside the workflow editor', () => {
        setCurrentNode({});

        renderInClusterElementForm();

        expect(lastQueryCall(hoisted.contextClusterElementOptionsQuery)[1].enabled).toBe(false);
    });
});

describe('PropertyMultiSelect', () => {
    describe('header', () => {
        it('shows the label with its required mark and description', async () => {
            renderMultiSelect({options: STATIC_OPTIONS});

            expect(screen.getByText('Channels')).toBeInTheDocument();
            expect(screen.getByText('*')).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Delete property'})).toBeInTheDocument();

            await userEvent.hover(screen.getByText('Channels').nextElementSibling as HTMLElement);

            expect(await screen.findByRole('tooltip')).toHaveTextContent('Channels to post to');
        });

        it('switches to a formula from the switch when it is offered', async () => {
            const handleInputTypeSwitchButtonClick = vi.fn();

            renderMultiSelect({
                handleInputTypeSwitchButtonClick,
                options: STATIC_OPTIONS,
                showInputTypeSwitchButton: true,
            });

            await userEvent.click(screen.getByRole('switch'));

            expect(handleInputTypeSwitchButtonClick).toHaveBeenCalledTimes(1);
        });

        it('offers no formula switch unless asked to', () => {
            renderMultiSelect({handleInputTypeSwitchButtonClick: vi.fn(), options: STATIC_OPTIONS});

            expect(screen.queryByRole('switch')).not.toBeInTheDocument();
        });

        it('renders no header without a label', () => {
            renderMultiSelect({options: STATIC_OPTIONS, property: {...PROPERTY, label: undefined}});

            expect(screen.queryByRole('button', {name: 'Delete property'})).not.toBeInTheDocument();
        });
    });

    describe('static options', () => {
        it('reports the options picked from the list', async () => {
            const {onChange} = renderMultiSelect({options: STATIC_OPTIONS});

            expect(screen.getByText('Pick channels')).toBeInTheDocument();

            await userEvent.click(getTrigger());
            await userEvent.click(screen.getByRole('option', {name: 'Random'}));

            expect(onChange).toHaveBeenLastCalledWith(['random']);
        });

        it('shows the selected values and removes one on its close mark', async () => {
            const {onChange} = renderMultiSelect({options: STATIC_OPTIONS, value: ['general', 'random']});

            expect(screen.getByLabelText('General-selected')).toBeInTheDocument();
            expect(screen.getByLabelText('Random-selected')).toBeInTheDocument();

            await userEvent.click(screen.getAllByLabelText('remove-option')[0]);

            expect(onChange).toHaveBeenLastCalledWith(['random']);
        });

        it('selects every option at once', async () => {
            const {onChange} = renderMultiSelect({options: STATIC_OPTIONS});

            await userEvent.click(getTrigger());
            await userEvent.click(screen.getByRole('option', {name: 'Select All'}));

            expect(onChange).toHaveBeenLastCalledWith(['general', 'random']);
        });

        it('does not fetch options without an options data source', () => {
            renderMultiSelect({options: STATIC_OPTIONS});

            expect(lastQueryCall(hoisted.workflowNodeOptionsQuery)[1]).toBe(false);
            expect(lastQueryCall(hoisted.clusterElementOptionsQuery)[1]).toBe(false);
        });

        it('says no options are available and stays closed when there are none', () => {
            renderMultiSelect({options: []});

            expect(screen.getByText('No options available')).toBeInTheDocument();
            expect(getTrigger()).toBeDisabled();
        });
    });

    describe('options loaded from the server', () => {
        it('fetches the options of a workflow node and offers them', async () => {
            hoisted.workflowNodeOptionsQuery.mockReturnValue({
                data: [{label: 'Alerts', value: 'alerts'}, {label: 'Ops'}],
                isLoading: false,
            });

            const {onChange} = renderMultiSelect({
                lookupDependsOnPaths: [],
                lookupDependsOnValues: [],
                optionsDataSource: OPTIONS_DATA_SOURCE,
            });

            const [queryOptions, enabled] = lastQueryCall(hoisted.workflowNodeOptionsQuery);

            expect(enabled).toBe(true);
            expect(queryOptions).toEqual({
                loadDependencyValueKey: '',
                request: {
                    environmentId: 7,
                    id: 'workflow-1',
                    lookupDependsOnPaths: [],
                    propertyName: 'parameters.channels',
                    workflowNodeName: 'slack_1',
                },
            });
            expect(lastQueryCall(hoisted.clusterElementOptionsQuery)[1]).toBe(false);

            await userEvent.click(getTrigger());
            await userEvent.click(screen.getByRole('option', {name: 'Ops'}));

            expect(onChange).toHaveBeenLastCalledWith(['Ops']);
        });

        it('fetches the options of a cluster element through its cluster root', () => {
            useWorkflowEditorStore.setState({
                rootClusterElementNodeData: {workflowNodeName: 'aiAgent_1'} as NodeDataType,
            });

            setCurrentNode({clusterElementType: 'model', workflowNodeName: 'openAi_1'});

            renderMultiSelect({
                lookupDependsOnPaths: [],
                lookupDependsOnValues: [],
                optionsDataSource: OPTIONS_DATA_SOURCE,
            });

            const [queryOptions, enabled] = lastQueryCall(hoisted.clusterElementOptionsQuery);

            expect(enabled).toBe(true);
            expect(queryOptions.request).toEqual({
                clusterElementType: 'model',
                clusterElementWorkflowNodeName: 'openAi_1',
                environmentId: 7,
                id: 'workflow-1',
                lookupDependsOnPaths: [],
                propertyName: 'parameters.channels',
                workflowNodeName: 'aiAgent_1',
            });
            expect(lastQueryCall(hoisted.workflowNodeOptionsQuery)[1]).toBe(false);
        });

        it('prefers the cluster element options over the workflow node options', async () => {
            hoisted.clusterElementOptionsQuery.mockReturnValue({data: [{label: 'From cluster', value: 'c'}]});
            hoisted.workflowNodeOptionsQuery.mockReturnValue({data: [{label: 'From node', value: 'n'}]});

            renderMultiSelect({optionsDataSource: OPTIONS_DATA_SOURCE});

            await userEvent.click(getTrigger());

            expect(screen.getByRole('option', {name: 'From cluster'})).toBeInTheDocument();
            expect(screen.queryByRole('option', {name: 'From node'})).not.toBeInTheDocument();
        });

        it('keys the request by the values it depends on', () => {
            renderMultiSelect({
                lookupDependsOnPaths: ['parameters.workspace'],
                lookupDependsOnValues: ['acme'],
                optionsDataSource: {optionsLookupDependsOn: ['parameters.workspace']},
            });

            const [queryOptions, enabled] = lastQueryCall(hoisted.workflowNodeOptionsQuery);

            expect(enabled).toBe(true);
            expect(queryOptions.loadDependencyValueKey).toBe('acme');
            expect(screen.getByText('Pick channels')).toBeInTheDocument();
        });

        it('waits for a missing dependency and names it', () => {
            renderMultiSelect({
                lookupDependsOnPaths: ['parameters.workspace'],
                lookupDependsOnValues: [undefined],
                optionsDataSource: {optionsLookupDependsOn: ['parameters.workspace']},
            });

            expect(lastQueryCall(hoisted.workflowNodeOptionsQuery)[1]).toBe(false);
            expect(screen.getByText('parameters.workspace is not defined')).toBeInTheDocument();
        });

        it('waits for a connection the node requires', () => {
            setCurrentNode({connections: [{componentName: 'slack', key: 'slack'}] as NodeDataType['connections']});

            renderMultiSelect({
                lookupDependsOnPaths: [],
                lookupDependsOnValues: [],
                optionsDataSource: OPTIONS_DATA_SOURCE,
            });

            expect(lastQueryCall(hoisted.workflowNodeOptionsQuery)[1]).toBe(false);
            expect(screen.getByText('Connection missing...')).toBeInTheDocument();
        });

        it('fetches once the required connection is set', () => {
            setCurrentNode({
                connectionId: 3,
                connections: [{componentName: 'slack', key: 'slack'}] as NodeDataType['connections'],
            });

            renderMultiSelect({
                lookupDependsOnPaths: [],
                lookupDependsOnValues: [],
                optionsDataSource: OPTIONS_DATA_SOURCE,
            });

            expect(lastQueryCall(hoisted.workflowNodeOptionsQuery)[1]).toBe(true);
        });

        it('does not fetch while the operation is being changed', () => {
            useWorkflowNodeDetailsPanelStore.setState({operationChangeInProgress: true});

            renderMultiSelect({
                lookupDependsOnPaths: [],
                lookupDependsOnValues: [],
                optionsDataSource: OPTIONS_DATA_SOURCE,
            });

            expect(lastQueryCall(hoisted.workflowNodeOptionsQuery)[1]).toBe(false);
        });

        it('cannot be opened while the options load', () => {
            hoisted.workflowNodeOptionsQuery.mockReturnValue({data: undefined, isLoading: true});

            renderMultiSelect({
                lookupDependsOnPaths: [],
                lookupDependsOnValues: [],
                options: STATIC_OPTIONS,
                optionsDataSource: OPTIONS_DATA_SOURCE,
            });

            expect(getTrigger()).toBeDisabled();
        });
    });
});
