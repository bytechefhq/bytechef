import {act, renderHook} from '@testing-library/react';
import {useForm} from 'react-hook-form';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useMcpComponentToolForms from '../useMcpComponentToolForms';

const hoisted = vi.hoisted(() => ({
    failedSeedKeys: [] as string[],
    seededValuesByTool: {} as Record<string, Record<string, string>>,
    toastWarning: vi.fn(),
    useMcpToolSeeding: vi.fn(),
}));

vi.mock('sonner', () => ({toast: {warning: hoisted.toastWarning}}));

vi.mock('@/pages/platform/mcp-servers/hooks/useMcpToolSeeding', () => ({
    default: (params: unknown) => {
        hoisted.useMcpToolSeeding(params);

        return {
            aiValuesByTool: {},
            getFailedSeedKeys: () => hoisted.failedSeedKeys,
            waitForSeeding: async () => hoisted.seededValuesByTool,
        };
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
        hoisted.failedSeedKeys = [];
        hoisted.seededValuesByTool = {};
        hoisted.toastWarning.mockReset();
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

    it('warns when the settings of a new tool could not be loaded', async () => {
        hoisted.failedSeedKeys = ['listChannels', 'sendMessage', 'notSelected'];

        const {result} = renderHook(() => useMcpComponentToolForms(baseProps));

        await act(async () => {
            await result.current.buildToolsPayload([{name: 'sendMessage'}, {name: 'listChannels'}]);
        });

        expect(hoisted.toastWarning).toHaveBeenCalledTimes(1);
        expect(hoisted.toastWarning).toHaveBeenCalledWith(
            "Couldn't load the settings of listChannels",
            expect.objectContaining({description: expect.stringContaining('without AI values')})
        );
    });

    it('does not warn when every new tool was seeded', async () => {
        const {result} = renderHook(() => useMcpComponentToolForms(baseProps));

        await act(async () => {
            await result.current.buildToolsPayload([{name: 'sendMessage'}, {name: 'listChannels'}]);
        });

        expect(hoisted.toastWarning).not.toHaveBeenCalled();
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
