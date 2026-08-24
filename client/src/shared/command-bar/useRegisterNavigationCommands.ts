import {type CommandI} from '@/shared/command-bar/types';
import {useRegisterCommands} from '@/shared/command-bar/useRegisterCommands';
import {type NavigationItemI} from '@/shared/navigation/navigationItems';
import {useMemo} from 'react';

/**
 * Uses the hook door rather than a bootstrap source because which entries are visible depends on the edition, three
 * feature flags, the AI configuration and the current environment -- roughly forty lines of filtering that already
 * live in App.tsx. Passing the filtered list in keeps that logic with exactly one implementation.
 */
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
