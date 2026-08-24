import {type CommandContextI, type CommandI} from '@/shared/command-bar/types';
import {collectCommands, useCommandSourceRegistry} from '@/shared/command-bar/useCommandSourceRegistry';
import {useRegisterCommands} from '@/shared/command-bar/useRegisterCommands';
import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

const context: CommandContextI = {
    edition: undefined,
    featureFlags: () => true,
    pathname: '/',
};

const commands: CommandI[] = [{actions: [{to: '/a', type: 'navigate'}], id: 'a', title: 'A'}];

describe('useRegisterCommands', () => {
    beforeEach(() => {
        useCommandSourceRegistry.getState().reset();
    });

    it('registers on mount', () => {
        renderHook(() => useRegisterCommands(commands, []));

        expect(useCommandSourceRegistry.getState().sources).toHaveLength(1);
    });

    it('unregisters on unmount', () => {
        const {unmount} = renderHook(() => useRegisterCommands(commands, []));

        unmount();

        expect(useCommandSourceRegistry.getState().sources).toHaveLength(0);
    });

    it('does not accumulate sources across re-renders', () => {
        const {rerender} = renderHook(() => useRegisterCommands(commands, []));

        rerender();
        rerender();

        expect(useCommandSourceRegistry.getState().sources).toHaveLength(1);
    });

    it('replaces the registered source when the dependency value changes', () => {
        const {rerender} = renderHook(
            ({dependencyValue}: {dependencyValue: string}) =>
                useRegisterCommands(
                    [
                        {
                            actions: [{to: `/${dependencyValue}`, type: 'navigate'}],
                            id: dependencyValue,
                            title: dependencyValue,
                        },
                    ],
                    [dependencyValue]
                ),
            {initialProps: {dependencyValue: 'first'}}
        );

        rerender({dependencyValue: 'second'});
        rerender({dependencyValue: 'third'});

        expect(useCommandSourceRegistry.getState().sources).toHaveLength(1);

        const collectedCommands = collectCommands(useCommandSourceRegistry.getState().sources, context);

        expect(collectedCommands.map((collectedCommand) => collectedCommand.id)).toEqual(['third']);
    });
});
