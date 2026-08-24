import {executeCommand} from '@/shared/command-bar/executeCommand';
import {type CommandContextI, type CommandI} from '@/shared/command-bar/types';
import {useCommandIntentStore} from '@/shared/command-bar/useCommandIntentStore';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const context: CommandContextI = {edition: 'EE', featureFlags: () => true, pathname: '/'};

const options = () => ({
    closePalette: vi.fn(),
    context,
    navigate: vi.fn(),
    onError: vi.fn(),
    recordRecent: vi.fn(),
});

describe('executeCommand', () => {
    beforeEach(() => {
        useCommandIntentStore.getState().reset();
    });

    it('closes the palette before running the first action', async () => {
        const order: string[] = [];
        const commandOptions = options();

        commandOptions.closePalette.mockImplementation(() => order.push('close'));
        commandOptions.navigate.mockImplementation(() => order.push('navigate'));

        const command: CommandI = {actions: [{to: '/a', type: 'navigate'}], id: 'a', title: 'A'};

        await executeCommand(command, commandOptions);

        expect(order).toEqual(['close', 'navigate']);
    });

    it('runs actions in order, awaiting each', async () => {
        const order: string[] = [];
        const commandOptions = options();

        const command: CommandI = {
            actions: [
                {
                    run: async () => {
                        await Promise.resolve();

                        order.push('first');
                    },
                    type: 'callback',
                },
                {run: () => void order.push('second'), type: 'callback'},
            ],
            id: 'a',
            title: 'A',
        };

        await executeCommand(command, commandOptions);

        expect(order).toEqual(['first', 'second']);
    });

    it('publishes an intent action', async () => {
        const command: CommandI = {
            actions: [{key: 'project.create', payload: {id: 7}, type: 'intent'}],
            id: 'a',
            title: 'A',
        };

        await executeCommand(command, options());

        expect(useCommandIntentStore.getState().intent).toEqual({key: 'project.create', payload: {id: 7}});
    });

    it('stops at the first failing action and reports it', async () => {
        const commandOptions = options();
        const never = vi.fn();

        const command: CommandI = {
            actions: [
                {
                    run: () => {
                        throw new Error('boom');
                    },
                    type: 'callback',
                },
                {run: never, type: 'callback'},
            ],
            id: 'a',
            title: 'A',
        };

        await executeCommand(command, commandOptions);

        expect(never).not.toHaveBeenCalled();
        expect(commandOptions.onError).toHaveBeenCalled();
    });

    it('does not record a failed command in recents', async () => {
        const commandOptions = options();

        const command: CommandI = {
            actions: [
                {
                    run: () => {
                        throw new Error('boom');
                    },
                    type: 'callback',
                },
            ],
            id: 'a',
            title: 'A',
        };

        await executeCommand(command, commandOptions);

        expect(commandOptions.recordRecent).not.toHaveBeenCalled();
    });

    it('records a successful command in recents', async () => {
        const commandOptions = options();

        const command: CommandI = {actions: [{to: '/a', type: 'navigate'}], id: 'a', title: 'A'};

        await executeCommand(command, commandOptions);

        expect(commandOptions.recordRecent).toHaveBeenCalledWith(command);
    });
});
