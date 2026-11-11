import {act, renderHook} from '@testing-library/react';
import {useForm} from 'react-hook-form';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useMcpComponentToolForms from '../useMcpComponentToolForms';

const hoisted = vi.hoisted(() => ({
    seededValuesByTool: {} as Record<string, Record<string, string>>,
    useMcpToolSeeding: vi.fn(),
}));

vi.mock('@/pages/platform/mcp-servers/hooks/useMcpToolSeeding', () => ({
    default: (params: unknown) => {
        hoisted.useMcpToolSeeding(params);

        return {aiValuesByTool: {}, waitForSeeding: async () => hoisted.seededValuesByTool};
    },
}));

const existingTools = [{name: 'sendMessage', parameters: {channel: 'general', toolName: 'send'}}, null];

type HookPropsType = Parameters<typeof useMcpComponentToolForms>[0];

const baseProps: HookPropsType = {
    componentName: 'slack',
    componentVersion: 1,
    connectionId: 'connection-1',
    connectionRequired: true,
    existingTools,
    selectedTools: [{name: 'sendMessage'}, {name: 'listChannels'}],
};

describe('useMcpComponentToolForms', () => {
    beforeEach(() => {
        hoisted.seededValuesByTool = {};
        hoisted.useMcpToolSeeding.mockReset();
    });

    it('seeds only the tools that are not saved yet', () => {
        renderHook(() => useMcpComponentToolForms(baseProps));

        expect(hoisted.useMcpToolSeeding).toHaveBeenLastCalledWith(
            expect.objectContaining({
                componentName: 'slack',
                componentVersion: 1,
                connectionId: 'connection-1',
                connectionRequired: true,
                newToolNames: ['listChannels'],
            })
        );
    });

    it('keeps the saved parameters map and new tool names stable across renders', () => {
        const {rerender, result} = renderHook((props: HookPropsType) => useMcpComponentToolForms(props), {
            initialProps: baseProps,
        });

        const firstSavedParameters = result.current.savedParametersByToolName;
        const firstNewToolNames = hoisted.useMcpToolSeeding.mock.lastCall![0].newToolNames;

        rerender(baseProps);

        expect(result.current.savedParametersByToolName).toBe(firstSavedParameters);
        expect(hoisted.useMcpToolSeeding.mock.lastCall![0].newToolNames).toBe(firstNewToolNames);
    });

    it('sends the seeded values for a new tool and the saved parameters for an existing one', async () => {
        hoisted.seededValuesByTool = {listChannels: {'filter.name': '=fromAi(name)'}};

        const {result} = renderHook(() => useMcpComponentToolForms(baseProps));

        let payload: Awaited<ReturnType<typeof result.current.buildToolsPayload>> = [];

        await act(async () => {
            payload = await result.current.buildToolsPayload([{name: 'sendMessage'}, {name: 'listChannels'}]);
        });

        expect(payload).toEqual([
            {name: 'sendMessage', parameters: {channel: 'general', toolName: 'send'}},
            {name: 'listChannels', parameters: {filter: {name: '=fromAi(name)'}}},
        ]);
    });

    it('adds the changed form values of a registered tool on top of its saved parameters', async () => {
        const {result: formResult} = renderHook(() =>
            useForm<Record<string, unknown>>({defaultValues: {channel: 'general', text: ''}})
        );

        const {result} = renderHook(() => useMcpComponentToolForms(baseProps));

        act(() => {
            result.current.registerToolForm('sendMessage', {
                form: formResult.current,
                initialValues: {channel: 'general', text: ''},
            });
        });

        act(() => formResult.current.setValue('text', 'Hello', {shouldDirty: true}));

        let payload: Awaited<ReturnType<typeof result.current.buildToolsPayload>> = [];

        await act(async () => {
            payload = await result.current.buildToolsPayload([{name: 'sendMessage'}]);
        });

        expect(payload).toEqual([
            {name: 'sendMessage', parameters: {channel: 'general', text: 'Hello', toolName: 'send'}},
        ]);
    });

    it('ignores the form of a tool that was unregistered', async () => {
        const {result: formResult} = renderHook(() => useForm<Record<string, unknown>>({defaultValues: {text: ''}}));

        const {result} = renderHook(() => useMcpComponentToolForms(baseProps));

        act(() => {
            result.current.registerToolForm('sendMessage', {form: formResult.current, initialValues: {text: ''}});
        });

        act(() => formResult.current.setValue('text', 'Hello', {shouldDirty: true}));

        act(() => {
            result.current.registerToolForm('sendMessage', null);
        });

        let payload: Awaited<ReturnType<typeof result.current.buildToolsPayload>> = [];

        await act(async () => {
            payload = await result.current.buildToolsPayload([{name: 'sendMessage'}]);
        });

        expect(payload).toEqual([{name: 'sendMessage', parameters: {channel: 'general', toolName: 'send'}}]);
    });
});
