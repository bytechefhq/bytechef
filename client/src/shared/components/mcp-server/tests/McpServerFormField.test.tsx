import {McpServer} from '@/shared/middleware/graphql';
import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {FormProvider, useForm} from 'react-hook-form';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import McpServerFormField from '../McpServerFormField';

// FormLabel and FormMessage read the form context, so the field can only render inside a provider.
const FormHarness = ({mcpServer}: {mcpServer?: McpServer}) => {
    const form = useForm({defaultValues: {mcpServerId: 'server-1'}});

    return (
        <FormProvider {...form}>
            <McpServerFormField control={form.control} mcpServer={mcpServer} name="mcpServerId" />
        </FormProvider>
    );
};

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('McpServerFormField', () => {
    it('should label the field', () => {
        render(<FormHarness />);

        expect(screen.getByText('MCP Server')).toBeInTheDocument();
    });

    it('should prompt for a server when none is given', () => {
        render(<FormHarness />);

        const input = screen.getByPlaceholderText('Select MCP Server');

        expect(input).toBeEnabled();
        expect(input).toHaveValue('server-1');
    });

    // Opened from a server's own page the server is already decided, so the field shows it and refuses edits.
    it('should lock itself to the given server', () => {
        render(<FormHarness mcpServer={{id: '1', name: 'My Server'} as McpServer} />);

        const input = screen.getByDisplayValue('My Server');

        expect(input).toBeDisabled();
    });
});
