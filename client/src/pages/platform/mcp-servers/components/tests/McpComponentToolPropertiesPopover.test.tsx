import {McpTool} from '@/shared/middleware/graphql';
import {PropertyAllType} from '@/shared/types';
import {render, screen} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import McpComponentToolPropertiesPopover from '../McpComponentToolPropertiesPopover';

const hoisted = vi.hoisted(() => ({
    displayConditions: undefined as Record<string, boolean> | undefined,
    isEvaluating: false,
    isLoading: false,
    properties: [] as Array<PropertyAllType>,
}));

vi.mock('@/components/ui/popover', () => ({
    PopoverContent: ({children}: {children: ReactNode}) => <div>{children}</div>,
}));

vi.mock('@/pages/platform/workflow-editor/components/properties/Properties', () => ({
    default: () => <div data-testid="properties" />,
}));

vi.mock('@/components/ui/tooltip', async () => {
    const actual = await vi.importActual<Record<string, unknown>>('@/components/ui/tooltip');

    return actual;
});

vi.mock('@/pages/platform/mcp-servers/hooks/useMcpComponentToolPropertiesPopover', async () => {
    const {useForm} = await import('react-hook-form');

    const useMcpComponentToolPropertiesPopover = () => {
        const form = useForm({defaultValues: {toolName: ''}});

        return {
            control: form.control,
            form,
            formState: form.formState,
            handleFormSubmit: vi.fn(),
            handleSubmit: form.handleSubmit,
            isLoading: hoisted.isLoading,
            properties: hoisted.properties,
        };
    };

    return {default: useMcpComponentToolPropertiesPopover};
});

vi.mock('@/pages/platform/mcp-servers/hooks/useMcpToolFormDisplayConditions', () => ({
    default: () => ({displayConditions: hoisted.displayConditions, isEvaluating: hoisted.isEvaluating}),
}));

const mcpTool = {id: '1', mcpComponentId: '1', name: 'post', title: 'POST', version: 1} as McpTool;

const renderPopover = () =>
    render(
        <McpComponentToolPropertiesPopover
            componentName="httpClient"
            componentVersion={1}
            mcpTool={mcpTool}
            onClose={vi.fn()}
        />
    );

describe('McpComponentToolPropertiesPopover', () => {
    beforeEach(() => {
        hoisted.displayConditions = undefined;
        hoisted.isEvaluating = false;
        hoisted.isLoading = false;
        hoisted.properties = [
            {controlType: 'TEXT', name: 'toolName', required: true, type: 'STRING'},
        ] as Array<PropertyAllType>;
    });

    // Property inputs carry the native `required` attribute. Without noValidate the browser blocks the submit
    // with a transient bubble before react-hook-form runs, so Save looked like a button that did nothing and
    // the edited values were never persisted.
    it('opts the form out of native validation so react-hook-form can report required fields', () => {
        const {container} = renderPopover();

        const form = container.querySelector('form');

        expect(form).not.toBeNull();
        expect(form).toHaveAttribute('novalidate');
    });

    // Unevaluated conditions show every conditional property, so rendering before the first verdict flashed the
    // whole set of mutually exclusive properties on open.
    it('holds the properties back until the display conditions have been evaluated once', () => {
        hoisted.isEvaluating = true;

        renderPopover();

        expect(screen.getByText('Loading properties...')).toBeInTheDocument();
        expect(screen.queryByTestId('properties')).not.toBeInTheDocument();
    });

    it('renders the properties once the display conditions have been evaluated', () => {
        hoisted.displayConditions = {"body.bodyContentType == 'JSON'": true};

        renderPopover();

        expect(screen.getByTestId('properties')).toBeInTheDocument();
        expect(screen.queryByText('Loading properties...')).not.toBeInTheDocument();
    });

    it('reports a tool that has no configurable properties', () => {
        hoisted.properties = [];

        renderPopover();

        expect(screen.getByText('No configurable properties for this tool.')).toBeInTheDocument();
    });
});
