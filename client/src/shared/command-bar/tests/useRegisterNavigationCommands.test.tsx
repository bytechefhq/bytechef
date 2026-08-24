import {type CommandContextI} from '@/shared/command-bar/types';
import {collectCommands, useCommandSourceRegistry} from '@/shared/command-bar/useCommandSourceRegistry';
import {useRegisterNavigationCommands} from '@/shared/command-bar/useRegisterNavigationCommands';
import {renderHook} from '@testing-library/react';
import {FolderIcon} from 'lucide-react';
import {beforeEach, describe, expect, it} from 'vitest';

const context: CommandContextI = {edition: 'EE', featureFlags: () => true, pathname: '/automation/projects'};

describe('useRegisterNavigationCommands', () => {
    beforeEach(() => {
        useCommandSourceRegistry.getState().reset();
    });

    it('registers one navigate command per item, in the Navigation group', () => {
        renderHook(() =>
            useRegisterNavigationCommands([{href: '/automation/projects', icon: FolderIcon, name: 'Projects'}])
        );

        const commands = collectCommands(useCommandSourceRegistry.getState().sources, context);

        expect(commands).toEqual([
            {
                actions: [{to: '/automation/projects', type: 'navigate'}],
                group: 'Navigation',
                icon: FolderIcon,
                id: 'navigation./automation/projects',
                title: 'Go to Projects',
            },
        ]);
    });

    it('registers nothing when the filtered navigation is empty', () => {
        renderHook(() => useRegisterNavigationCommands([]));

        expect(collectCommands(useCommandSourceRegistry.getState().sources, context)).toEqual([]);
    });
});
