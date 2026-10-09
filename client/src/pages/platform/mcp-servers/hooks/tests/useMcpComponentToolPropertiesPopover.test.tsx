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
    embeddedMutate: vi.fn(),
    embeddedMutationOptions: undefined as {onSuccess: () => void} | undefined,
    invalidateQueries: vi.fn(),
    mutate: vi.fn(),
    mutationOptions: undefined as {onSuccess: () => void} | undefined,
    properties: [] as Array<Record<string, unknown>>,
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useClusterElementDefinitionQuery: () => ({
        data: {clusterElementDefinition: {properties: hoisted.properties}},
        isLoading: false,
    }),
    useUpdateEmbeddedMcpToolMutation: (options: {onSuccess: () => void}) => {
        hoisted.embeddedMutationOptions = options;

        return {mutate: hoisted.embeddedMutate};
    },
    useUpdateMcpToolMutation: (options: {onSuccess: () => void}) => {
        hoisted.mutationOptions = options;

        return {mutate: hoisted.mutate};
    },
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: () => ({invalidateQueries: hoisted.invalidateQueries}),
}));

const renderPopoverHook = (parameters: Record<string, unknown>, embedded?: boolean, onClose = vi.fn()) => {
    const mcpTool = {
        id: '1',
        mcpComponentId: '1',
        name: 'post',
        parameters,
        version: 1,
    } as unknown as McpTool;

    return renderHook(() => useMcpComponentToolPropertiesPopover('httpClient', 1, mcpTool, onClose, embedded));
};

describe('useMcpComponentToolPropertiesPopover', () => {
    beforeEach(() => {
        (hoisted.embeddedMutate as unknown as Mock).mockReset();
        (hoisted.mutate as unknown as Mock).mockReset();
        hoisted.invalidateQueries.mockReset();

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

    it('saves an embedded tool through the embedded mutation', async () => {
        const {result} = renderPopoverHook({uri: 'https://example.com'}, true);

        await waitFor(() => expect(result.current.properties).toHaveLength(3));

        act(() => result.current.handleFormSubmit({uri: 'https://example.org'}));

        expect(hoisted.embeddedMutate).toHaveBeenCalledWith({
            id: '1',
            input: {mcpComponentId: '1', name: 'post', parameters: {uri: 'https://example.org'}, version: 1},
        });
        expect(hoisted.mutate).not.toHaveBeenCalled();
    });

    it('round-trips a cleared field back to an empty string', async () => {
        const {result} = renderPopoverHook({});

        await waitFor(() => expect(result.current.properties).toHaveLength(3));

        act(() => result.current.handleFormSubmit({toolDescription: ''}));

        const stored = (hoisted.mutate as unknown as Mock).mock.calls[0][0].input.parameters;

        const {result: reopened} = renderPopoverHook(stored);

        await waitFor(() => expect(reopened.current.form.getValues('toolDescription')).toBe(''));
    });

    it('refreshes the automation components and closes the popover after an automation tool is saved', () => {
        const onClose = vi.fn();

        renderPopoverHook({}, false, onClose);

        act(() => hoisted.mutationOptions!.onSuccess());

        expect(hoisted.invalidateQueries).toHaveBeenCalledWith({queryKey: ['mcpComponentsByServerId']});
        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('refreshes the embedded components and closes the popover after an embedded tool is saved', () => {
        const onClose = vi.fn();

        renderPopoverHook({}, true, onClose);

        act(() => hoisted.embeddedMutationOptions!.onSuccess());

        expect(hoisted.invalidateQueries).toHaveBeenCalledWith({queryKey: ['embeddedMcpComponentsByServerId']});
        expect(hoisted.invalidateQueries).not.toHaveBeenCalledWith({queryKey: ['mcpComponentsByServerId']});
        expect(onClose).toHaveBeenCalledTimes(1);
    });
});
