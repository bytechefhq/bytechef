import {type LucideIcon} from 'lucide-react';
import {type NavigateFunction} from 'react-router-dom';

export type CommandActionType =
    | {run: (runContext: CommandRunContextI) => Promise<void> | void; type: 'callback'}
    | {key: string; payload?: unknown; type: 'intent'}
    | {to: string; type: 'navigate'};

export interface CommandContextI {
    edition: string | undefined;
    featureFlags: (featureFlag: string) => boolean;
    pathname: string;
}

export interface CommandRunContextI {
    command: CommandI;
    context: CommandContextI;
    navigate: NavigateFunction;
}

export interface CommandChildrenI {
    minQueryLength?: number;
    placeholder: string;
    resolve: (query: string, signal: AbortSignal) => Promise<CommandI[]>;
}

export interface CommandI {
    actions?: CommandActionType[];
    children?: CommandChildrenI;
    group?: string;
    icon?: LucideIcon;
    id: string;
    keywords?: string[];
    subtitle?: string;
    title: string;
    when?: (context: CommandContextI) => boolean;
}

export interface CommandSourceI {
    getCommands: (context: CommandContextI) => CommandI[];
    id: string;
}

export interface RecentCommandI {
    actions: CommandActionType[];
    id: string;
    title: string;
}
