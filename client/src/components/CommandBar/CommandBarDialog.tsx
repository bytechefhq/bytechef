import {Command, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList} from '@/components/ui/command';
import {Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {executeCommand} from '@/shared/command-bar/executeCommand';
import {type CommandChildrenI, type CommandI} from '@/shared/command-bar/types';
import {useCommandBarStore} from '@/shared/command-bar/useCommandBarStore';
import {useCommandContext} from '@/shared/command-bar/useCommandContext';
import {useCommandRecentsStore} from '@/shared/command-bar/useCommandRecentsStore';
import {collectCommands, useCommandSourceRegistry} from '@/shared/command-bar/useCommandSourceRegistry';
import {useResolvedChildren} from '@/shared/command-bar/useResolvedChildren';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {ArrowRightIcon} from 'lucide-react';
import {type KeyboardEvent as ReactKeyboardEvent, useCallback, useMemo} from 'react';
import {useNavigate} from 'react-router-dom';
import {toast} from 'sonner';

const UNGROUPED = 'Commands';

// Group order must not depend on the order sources happened to register in: navigation comes from a React hook and
// resources from the bootstrap module, so insertion order is not stable. Anything unlisted sorts before Navigation.
const GROUP_ORDER = [
    'Workflows',
    'Projects',
    'Connections',
    'Data Tables',
    'Deployments',
    'API Platform',
    'Knowledge Base',
    'Files',
    UNGROUPED,
    'Navigation',
];

function compareGroups(first: string, second: string): number {
    const firstIndex = GROUP_ORDER.indexOf(first);
    const secondIndex = GROUP_ORDER.indexOf(second);

    return (
        (firstIndex === -1 ? GROUP_ORDER.indexOf(UNGROUPED) : firstIndex) -
        (secondIndex === -1 ? GROUP_ORDER.indexOf(UNGROUPED) : secondIndex)
    );
}

const CommandBarDialog = () => {
    const navigate = useNavigate();

    const open = useCommandBarStore((state) => state.open);
    const popCommand = useCommandBarStore((state) => state.popCommand);
    const pushCommand = useCommandBarStore((state) => state.pushCommand);
    const query = useCommandBarStore((state) => state.query);
    const setOpen = useCommandBarStore((state) => state.setOpen);
    const setQuery = useCommandBarStore((state) => state.setQuery);
    const stack = useCommandBarStore((state) => state.stack);

    const sources = useCommandSourceRegistry((state) => state.sources);
    const addRecent = useCommandRecentsStore((state) => state.addRecent);
    const recentsByUserId = useCommandRecentsStore((state) => state.recentsByUserId);
    const userId = useAuthenticationStore((state) => state.account?.id);

    const context = useCommandContext();

    const activeCommand = stack.at(-1);

    // The hook must be called unconditionally to keep hook order stable across renders, so at the root level (no
    // active command) it receives this stable no-op stand-in instead of being skipped.
    const noChildren = useMemo<CommandChildrenI>(
        () => ({minQueryLength: Number.MAX_SAFE_INTEGER, placeholder: '', resolve: async () => []}),
        []
    );

    const {children: resolvedChildren, isBelowMinimum} = useResolvedChildren(
        activeCommand?.children ?? noChildren,
        query
    );

    const commands = useMemo(() => collectCommands(sources, context), [context, sources]);

    const recentCommands = useMemo<CommandI[]>(() => {
        if (!userId) {
            return [];
        }

        const commandsById = new Map(commands.map((command) => [command.id, command]));

        // The icon is not persisted -- a LucideIcon is a component, not JSON -- so it is re-resolved from the live
        // registry and falls back to the generic arrow when the command is no longer registered.
        //
        // `keywords` matters as much as `title` here: cmdk scores an item against its `value` + `keywords`, and
        // recents are rendered with a `recent-${id}` value (see renderCommandItem below) to avoid colliding with
        // the live registry entry's own value. Without the recent's title in `keywords`, typing anything scores
        // that value at 0 and the recent silently disappears from the filtered list the moment the user types.
        //
        // `group` is deliberately omitted -- the Recent block below renders under its own hardcoded heading and
        // bypasses groupedCommands entirely, so a `group` field here would never be read.
        return (recentsByUserId[String(userId)] ?? []).map((recent) => ({
            actions: recent.actions,
            icon: commandsById.get(recent.id)?.icon,
            id: recent.id,
            keywords: [recent.title],
            title: recent.title,
        }));
    }, [commands, recentsByUserId, userId]);

    const groupedCommands = useMemo(() => {
        const groups = new Map<string, CommandI[]>();

        for (const command of commands) {
            const group = command.group || UNGROUPED;

            groups.set(group, [...(groups.get(group) ?? []), command]);
        }

        return [...groups.entries()].sort(([first], [second]) => compareGroups(first, second));
    }, [commands]);

    const handleSelect = useCallback(
        (command: CommandI) => {
            if (command.children) {
                pushCommand(command);

                return;
            }

            void executeCommand(command, {
                closePalette: () => setOpen(false),
                context,
                navigate,
                onError: (error) =>
                    toast.error(`"${command.title}" failed`, {
                        description: error instanceof Error ? error.message : String(error),
                    }),
                recordRecent: (executed) => userId && addRecent(String(userId), executed),
            });
        },
        [addRecent, context, navigate, pushCommand, setOpen, userId]
    );

    const handleKeyDown = useCallback(
        (event: ReactKeyboardEvent<HTMLInputElement>) => {
            if (event.key === 'Backspace' && query === '' && stack.length > 0) {
                event.preventDefault();

                popCommand();
            }
        },
        [popCommand, query, stack.length]
    );

    // Radix's DialogContent fires onOpenChange(false) for Escape, outside-click, AND the close button alike -- with
    // no way to tell them apart from inside that callback. Only Escape should pop a nested sub-mode back to the
    // root command list; outside-click and the close button must always close the whole palette. Distinguishing
    // them needs the Content-level onEscapeKeyDown callback below (Escape only, and preventable independently of
    // onOpenChange), so onOpenChange itself stays a plain, unconditional setOpen -- it never pops.
    const handleEscapeKeyDown = useCallback(
        (event: KeyboardEvent) => {
            if (stack.length > 0) {
                // Stops Radix's own dismiss-on-Escape (which would otherwise call onOpenChange(false) and close the
                // whole palette) so popping the sub-mode is the only thing that happens.
                event.preventDefault();

                popCommand();
            }
        },
        [popCommand, stack.length]
    );

    const renderCommandItem = useCallback(
        (command: CommandI, value?: string) => {
            const Icon = command.icon || ArrowRightIcon;

            return (
                <CommandItem
                    key={command.id}
                    keywords={command.keywords}
                    onSelect={() => handleSelect(command)}
                    value={value || command.title}
                >
                    <Icon className="mr-2 size-4" />

                    <span>{command.title}</span>

                    {command.subtitle && <span className="ml-2 text-xs text-muted-foreground">{command.subtitle}</span>}
                </CommandItem>
            );
        },
        [handleSelect]
    );

    return (
        <Dialog onOpenChange={setOpen} open={open}>
            <DialogHeader className="sr-only">
                <DialogTitle>Command Palette</DialogTitle>

                <DialogDescription>Search for a command to run...</DialogDescription>
            </DialogHeader>

            {/*
                CommandDialog (client/src/components/ui/command.tsx) is not used here: it spreads its props onto
                Dialog's Root, not onto DialogContent, so a Content-level Radix callback like onEscapeKeyDown --
                needed to tell Escape apart from outside-click/close-button below -- cannot reach it through that
                wrapper. Composing Dialog + DialogContent + Command directly (all already exported for reuse) gets
                access to it without changing the shared wrapper itself; the classes/structure below mirror what
                CommandDialog renders internally.
            */}

            <DialogContent className="overflow-hidden p-0" onEscapeKeyDown={handleEscapeKeyDown} showCloseButton>
                <Command className="**:data-[slot=command-input-wrapper]:h-12 [&_[cmdk-group-heading]]:px-2 [&_[cmdk-group-heading]]:font-medium [&_[cmdk-group-heading]]:text-muted-foreground [&_[cmdk-group]]:px-2 [&_[cmdk-group]:not([hidden])_~[cmdk-group]]:pt-0 [&_[cmdk-input-wrapper]_svg]:h-5 [&_[cmdk-input-wrapper]_svg]:w-5 [&_[cmdk-input]]:h-12 [&_[cmdk-item]]:px-2 [&_[cmdk-item]]:py-3 [&_[cmdk-item]_svg]:h-5 [&_[cmdk-item]_svg]:w-5">
                    <CommandInput
                        className="my-2"
                        onKeyDown={handleKeyDown}
                        onValueChange={setQuery}
                        placeholder={
                            activeCommand ? activeCommand.children!.placeholder : 'Type a command or search...'
                        }
                        value={query}
                    />

                    <CommandList>
                        {activeCommand ? (
                            isBelowMinimum ? (
                                // Every shipped source sets minQueryLength: 0, so this branch is currently
                                // unreachable in practice -- kept for any future source that sets a higher minimum,
                                // and deliberately worded differently from the no-results branch below so the two
                                // states read as distinct.
                                <CommandEmpty>Type at least 2 characters to search...</CommandEmpty>
                            ) : resolvedChildren.length === 0 ? (
                                <CommandEmpty>No results found.</CommandEmpty>
                            ) : (
                                <CommandGroup heading={activeCommand.title}>
                                    {resolvedChildren.map((command) => renderCommandItem(command))}
                                </CommandGroup>
                            )
                        ) : (
                            <>
                                <CommandEmpty>No results found.</CommandEmpty>

                                {recentCommands.length > 0 && (
                                    <CommandGroup heading="Recent">
                                        {recentCommands.map((command) =>
                                            renderCommandItem(command, `recent-${command.id}`)
                                        )}
                                    </CommandGroup>
                                )}

                                {groupedCommands.map(([group, groupCommands]) => (
                                    <CommandGroup heading={group} key={group}>
                                        {groupCommands.map((command) => renderCommandItem(command))}
                                    </CommandGroup>
                                ))}
                            </>
                        )}
                    </CommandList>
                </Command>
            </DialogContent>
        </Dialog>
    );
};

export default CommandBarDialog;
