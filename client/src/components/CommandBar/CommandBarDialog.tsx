import Button from '@/components/Button/Button';
import {Dialog, DialogClose, DialogContent, DialogMain, DialogTitle} from '@/components/Dialog';
import {Command, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList} from '@/components/ui/command';
import {executeCommand} from '@/shared/command-bar/executeCommand';
import {type CommandChildrenI, type CommandI} from '@/shared/command-bar/types';
import {useCommandBarStore} from '@/shared/command-bar/useCommandBarStore';
import {useCommandContext} from '@/shared/command-bar/useCommandContext';
import {useCommandRecentsStore} from '@/shared/command-bar/useCommandRecentsStore';
import {collectCommands, useCommandSourceRegistry} from '@/shared/command-bar/useCommandSourceRegistry';
import {useResolvedChildren} from '@/shared/command-bar/useResolvedChildren';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {ArrowRightIcon, XIcon} from 'lucide-react';
import {type KeyboardEvent as ReactKeyboardEvent, useCallback, useMemo} from 'react';
import {useNavigate} from 'react-router-dom';
import {toast} from 'sonner';

const UNGROUPED = 'Commands';

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

    const handleEscapeKeyDown = useCallback(
        (event: KeyboardEvent) => {
            if (stack.length > 0) {
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
            <DialogContent aria-describedby={undefined} onEscapeKeyDown={handleEscapeKeyDown}>
                <DialogMain className="relative overflow-hidden">
                    <DialogTitle className="sr-only">Command Palette</DialogTitle>

                    <Command className="**:data-[slot=command-input-wrapper]:h-12 **:data-[slot=command-input-wrapper]:pr-10 [&_[cmdk-group-heading]]:px-2 [&_[cmdk-group-heading]]:font-medium [&_[cmdk-group-heading]]:text-muted-foreground [&_[cmdk-group]]:px-2 [&_[cmdk-group]:not([hidden])_~[cmdk-group]]:pt-0 [&_[cmdk-input-wrapper]_svg]:h-5 [&_[cmdk-input-wrapper]_svg]:w-5 [&_[cmdk-input]]:h-12 [&_[cmdk-item]]:px-2 [&_[cmdk-item]]:py-3 [&_[cmdk-item]_svg]:h-5 [&_[cmdk-item]_svg]:w-5">
                        <CommandInput
                            className="border-none ring-0"
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

                    <DialogClose asChild>
                        <Button
                            aria-label="Close"
                            className="absolute top-3 right-3"
                            icon={<XIcon />}
                            size="iconXs"
                            variant="ghost"
                        />
                    </DialogClose>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default CommandBarDialog;
