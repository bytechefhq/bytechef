import {type CommandI} from '@/shared/command-bar/types';
import {useRegisterCommands} from '@/shared/command-bar/useRegisterCommands';
import {type NavigationItemI} from '@/shared/navigation/navigationItems';
import {useMemo} from 'react';

export function useRegisterNavigationCommands(navigationItems: NavigationItemI[]): void {
    const commands = useMemo<CommandI[]>(
        () =>
            navigationItems.map((navigationItem) => ({
                actions: [{to: navigationItem.href, type: 'navigate'}],
                group: 'Navigation',
                icon: navigationItem.icon,
                id: `navigation.${navigationItem.href}`,
                title: `Go to ${navigationItem.name}`,
            })),
        [navigationItems]
    );

    useRegisterCommands(commands, [commands]);
}
