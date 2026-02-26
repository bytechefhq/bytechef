import {PropertyAllType} from '@/shared/types';
import {render, screen} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import PropertyDynamicProperties from './PropertyDynamicProperties';

const hoisted = vi.hoisted(() => ({
    mockClusterElementQuery: vi.fn(),
    mockDynamicPropertiesQuery: vi.fn(),
    mockNodeDetailsPanelStore: vi.fn(),
    mockWorkflowEditorStore: vi.fn(),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: () => 1,
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowDataStore', () => ({
    default: () => ({actionNames: [], id: 'workflow-1', nodeNames: []}),
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore', () => ({
    default: (...args: unknown[]) => hoisted.mockNodeDetailsPanelStore(...args),
}));

vi.mock('../../../stores/useWorkflowEditorStore', () => ({
    default: () => hoisted.mockWorkflowEditorStore(),
}));

vi.mock('@/shared/queries/platform/workflowNodeDynamicProperties.queries', () => ({
    useGetClusterElementDynamicPropertiesQuery: hoisted.mockClusterElementQuery,
    useGetWorkflowNodeDynamicPropertiesQuery: hoisted.mockDynamicPropertiesQuery,
}));

vi.mock('../Property', () => ({
    default: ({parameterValue, property}: {parameterValue: unknown; property: PropertyAllType}) => (
        <div data-testid={`property-${property.name}`} data-value={String(parameterValue)}>
            {property.name}
        </div>
    ),
}));

const sampleProperties = [
    {defaultValue: 'val1', name: 'field1', type: 'STRING'},
    {name: 'field2', type: 'INTEGER'},
] as PropertyAllType[];

const defaultProps = {
    enabled: true,
    lookupDependsOnPaths: ['connectionId'],
    lookupDependsOnValues: [1],
    name: 'dynamicProp',
    parameterValue: {field1: 'saved1', field2: 42},
    path: 'parameters',
};

beforeEach(() => {
    hoisted.mockDynamicPropertiesQuery.mockReturnValue({data: undefined, isLoading: false});
    hoisted.mockClusterElementQuery.mockReturnValue({data: undefined, isLoading: false});

    hoisted.mockNodeDetailsPanelStore.mockReturnValue({
        currentNode: {
            clusterElementType: undefined,
            name: 'httpClient_1',
            workflowNodeName: 'httpClient_1',
        },
        operationChangeInProgress: false,
    });

    hoisted.mockWorkflowEditorStore.mockReturnValue({
        rootClusterElementNodeData: undefined,
    });
});

afterEach(() => {
    vi.clearAllMocks();
});

describe('PropertyDynamicProperties', () => {
    describe('rendering sub-properties', () => {
        it('should render a Property for each sub-property returned by the query', () => {
            hoisted.mockDynamicPropertiesQuery.mockReturnValue({data: sampleProperties, isLoading: false});

            render(<PropertyDynamicProperties {...defaultProps} />);

            expect(screen.getByTestId('property-field1')).toBeInTheDocument();
            expect(screen.getByTestId('property-field2')).toBeInTheDocument();
        });

        it('should render nothing when sub-properties array is empty', () => {
            hoisted.mockDynamicPropertiesQuery.mockReturnValue({data: [], isLoading: false});

            const {container} = render(<PropertyDynamicProperties {...defaultProps} />);

            expect(container.querySelector('ul')).toBeInTheDocument();
            expect(screen.queryByTestId('property-field1')).not.toBeInTheDocument();
        });

        it('should use property.defaultValue when defined', () => {
            hoisted.mockDynamicPropertiesQuery.mockReturnValue({data: sampleProperties, isLoading: false});

            render(<PropertyDynamicProperties {...defaultProps} />);

            expect(screen.getByTestId('property-field1')).toHaveAttribute('data-value', 'val1');
        });

        it('should fall back to parameterValue object when property.defaultValue is undefined', () => {
            hoisted.mockDynamicPropertiesQuery.mockReturnValue({data: sampleProperties, isLoading: false});

            render(<PropertyDynamicProperties {...defaultProps} />);

            expect(screen.getByTestId('property-field2')).toHaveAttribute('data-value', '42');
        });
    });

    describe('parameterValue guard (primitive protection)', () => {
        it('should not use primitive parameterValue for bracket access', () => {
            hoisted.mockDynamicPropertiesQuery.mockReturnValue({data: sampleProperties, isLoading: false});

            render(<PropertyDynamicProperties {...defaultProps} parameterValue="a string" />);

            expect(screen.getByTestId('property-field2')).toHaveAttribute('data-value', '');
        });

        it('should not use array parameterValue for bracket access', () => {
            hoisted.mockDynamicPropertiesQuery.mockReturnValue({data: sampleProperties, isLoading: false});

            render(<PropertyDynamicProperties {...defaultProps} parameterValue={[1, 2, 3]} />);

            expect(screen.getByTestId('property-field2')).toHaveAttribute('data-value', '');
        });
    });

    describe('Fix 3 — skeleton guard when subProperties already exist', () => {
        it('should show skeletons when loading with no existing sub-properties', () => {
            hoisted.mockDynamicPropertiesQuery.mockReturnValue({data: undefined, isLoading: true});

            const {container} = render(<PropertyDynamicProperties {...defaultProps} />);

            const skeletons = container.querySelectorAll('[class*="animate-pulse"]');

            expect(skeletons.length).toBeGreaterThan(0);
        });

        it('should NOT show skeletons when loading but sub-properties already loaded', () => {
            hoisted.mockDynamicPropertiesQuery.mockReturnValue({data: sampleProperties, isLoading: true});

            render(<PropertyDynamicProperties {...defaultProps} />);

            expect(screen.getByTestId('property-field1')).toBeInTheDocument();
            expect(screen.getByTestId('property-field2')).toBeInTheDocument();
        });

        it('should NOT show skeletons when query is disabled', () => {
            hoisted.mockDynamicPropertiesQuery.mockReturnValue({data: undefined, isLoading: false});

            const {container} = render(<PropertyDynamicProperties {...defaultProps} enabled={false} />);

            const skeletons = container.querySelectorAll('[class*="animate-pulse"]');

            expect(skeletons.length).toBe(0);
        });
    });

    describe('query enabling logic', () => {
        it('should disable query when lookupDependsOnValues contains null', () => {
            hoisted.mockDynamicPropertiesQuery.mockReturnValue({data: undefined, isLoading: false});

            render(
                <PropertyDynamicProperties
                    {...defaultProps}
                    lookupDependsOnPaths={['connectionId']}
                    lookupDependsOnValues={[null]}
                />
            );

            const queryCall = hoisted.mockDynamicPropertiesQuery.mock.calls[0];

            expect(queryCall[1]).toBe(false);
        });

        it('should enable query when all dependencies are defined', () => {
            hoisted.mockDynamicPropertiesQuery.mockReturnValue({data: undefined, isLoading: true});

            render(<PropertyDynamicProperties {...defaultProps} />);

            const queryCall = hoisted.mockDynamicPropertiesQuery.mock.calls[0];

            expect(queryCall[1]).toBe(true);
        });

        it('should enable query when no lookupDependsOnPaths are specified', () => {
            hoisted.mockDynamicPropertiesQuery.mockReturnValue({data: undefined, isLoading: true});

            render(
                <PropertyDynamicProperties
                    {...defaultProps}
                    lookupDependsOnPaths={undefined}
                    lookupDependsOnValues={undefined}
                />
            );

            const queryCall = hoisted.mockDynamicPropertiesQuery.mock.calls[0];

            expect(queryCall[1]).toBe(true);
        });
    });

    describe('cluster element query', () => {
        beforeEach(() => {
            hoisted.mockNodeDetailsPanelStore.mockReturnValue({
                currentNode: {
                    clusterElementType: 'processor',
                    name: 'fieldMapper_1',
                    workflowNodeName: 'fieldMapper_1',
                },
                operationChangeInProgress: false,
            });

            hoisted.mockWorkflowEditorStore.mockReturnValue({
                rootClusterElementNodeData: {
                    workflowNodeName: 'dataStream_1',
                },
            });
        });

        it('should pass lookupDependsOnPaths in cluster element query request', () => {
            hoisted.mockClusterElementQuery.mockReturnValue({data: sampleProperties, isLoading: false});

            const lookupDependsOnPaths = ['useJsonSchema'];

            render(
                <PropertyDynamicProperties
                    {...defaultProps}
                    lookupDependsOnPaths={lookupDependsOnPaths}
                    lookupDependsOnValues={[true]}
                />
            );

            const queryCall = hoisted.mockClusterElementQuery.mock.calls[0];
            const queryOptions = queryCall[0];

            expect(queryOptions.request.lookupDependsOnPaths).toEqual(lookupDependsOnPaths);
        });

        it('should include cluster element type and workflow node names in request', () => {
            hoisted.mockClusterElementQuery.mockReturnValue({data: sampleProperties, isLoading: false});

            render(<PropertyDynamicProperties {...defaultProps} />);

            const queryCall = hoisted.mockClusterElementQuery.mock.calls[0];
            const queryOptions = queryCall[0];

            expect(queryOptions.request.clusterElementType).toBe('processor');
            expect(queryOptions.request.clusterElementWorkflowNodeName).toBe('fieldMapper_1');
            expect(queryOptions.request.workflowNodeName).toBe('dataStream_1');
        });

        it('should use cluster element query when clusterElementType is set', () => {
            hoisted.mockClusterElementQuery.mockReturnValue({data: sampleProperties, isLoading: false});

            render(<PropertyDynamicProperties {...defaultProps} />);

            const clusterQueryCall = hoisted.mockClusterElementQuery.mock.calls[0];

            expect(clusterQueryCall[1]).toBe(true);

            const regularQueryCall = hoisted.mockDynamicPropertiesQuery.mock.calls[0];

            expect(regularQueryCall[1]).toBe(false);
        });

        it('should render cluster element properties', () => {
            hoisted.mockClusterElementQuery.mockReturnValue({data: sampleProperties, isLoading: false});

            render(<PropertyDynamicProperties {...defaultProps} />);

            expect(screen.getByTestId('property-field1')).toBeInTheDocument();
            expect(screen.getByTestId('property-field2')).toBeInTheDocument();
        });
    });
});
