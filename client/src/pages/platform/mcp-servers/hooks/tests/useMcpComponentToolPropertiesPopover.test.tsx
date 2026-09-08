import {McpTool} from '@/shared/middleware/graphql';
import {act, renderHook, waitFor} from '@testing-library/react';
import {type Mock, beforeEach, describe, expect, it, vi} from 'vitest';

import useMcpComponentToolPropertiesPopover from '../useMcpComponentToolPropertiesPopover';

/**
 * An empty field is stored as null so the backend clears the parameter, which means the form is seeded from
 * nulls the next time the tool is opened. A controlled input handed null is uncontrolled as far as React is
 * concerned, so the two directions have to mirror each other.
 */

const hoisted = vi.hoisted(() => ({
    mutate: vi.fn(),
    properties: [] as Array<Record<string, unknown>>,
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useClusterElementDefinitionQuery: () => ({
        data: {clusterElementDefinition: {properties: hoisted.properties}},
        isLoading: false,
    }),
    useUpdateMcpToolMutation: () => ({mutate: hoisted.mutate}),
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: () => ({invalidateQueries: vi.fn()}),
}));

const renderPopoverHook = (parameters: Record<string, unknown>) => {
    const mcpTool = {
        id: '1',
        mcpComponentId: '1',
        name: 'post',
        parameters,
        version: 1,
    } as unknown as McpTool;

    return renderHook(() => useMcpComponentToolPropertiesPopover('httpClient', 1, mcpTool, vi.fn()));
};

describe('useMcpComponentToolPropertiesPopover', () => {
    beforeEach(() => {
        (hoisted.mutate as unknown as Mock).mockReset();

        hoisted.properties = [
            {controlType: 'TEXT_AREA', name: 'toolDescription', type: 'STRING'},
            {controlType: 'TEXT', name: 'uri', type: 'STRING'},
            {controlType: 'OBJECT_BUILDER', name: 'body', type: 'OBJECT'},
        ];
    });

    it('seeds a cleared field as an empty string rather than null', async () => {
        const {result} = renderPopoverHook({toolDescription: null, uri: 'https://example.com'});

        await waitFor(() => expect(result.current.form.getValues('toolDescription')).toBe(''));

        expect(result.current.form.getValues('uri')).toBe('https://example.com');
    });

    it('seeds nested cleared fields as empty strings', async () => {
        const {result} = renderPopoverHook({body: {bodyContent: null, bodyContentType: 'JSON'}});

        await waitFor(() => expect(result.current.form.getValues('body.bodyContent')).toBe(''));

        expect(result.current.form.getValues('body.bodyContentType')).toBe('JSON');
    });

    it('stores a cleared field as null', async () => {
        const {result} = renderPopoverHook({uri: 'https://example.com'});

        await waitFor(() => expect(result.current.properties).toHaveLength(3));

        act(() => result.current.handleFormSubmit({toolDescription: '', uri: 'https://example.com'}));

        expect(hoisted.mutate).toHaveBeenCalledWith(
            expect.objectContaining({
                input: expect.objectContaining({
                    parameters: {toolDescription: null, uri: 'https://example.com'},
                }),
            })
        );
    });

    it('stores nested cleared fields as null', async () => {
        const {result} = renderPopoverHook({});

        await waitFor(() => expect(result.current.properties).toHaveLength(3));

        act(() => result.current.handleFormSubmit({body: {bodyContent: '', bodyContentType: 'JSON'}}));

        expect(hoisted.mutate).toHaveBeenCalledWith(
            expect.objectContaining({
                input: expect.objectContaining({
                    parameters: {body: {bodyContent: null, bodyContentType: 'JSON'}},
                }),
            })
        );
    });

    it('round-trips a cleared field back to an empty string', async () => {
        const {result} = renderPopoverHook({});

        await waitFor(() => expect(result.current.properties).toHaveLength(3));

        act(() => result.current.handleFormSubmit({toolDescription: ''}));

        const stored = (hoisted.mutate as unknown as Mock).mock.calls[0][0].input.parameters;

        const {result: reopened} = renderPopoverHook(stored);

        await waitFor(() => expect(reopened.current.form.getValues('toolDescription')).toBe(''));
    });
});
