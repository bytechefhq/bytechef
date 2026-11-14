import type {CompletionContextType} from './completionTypes';

const completionContextsByModelUri = new Map<string, CompletionContextType>();

export function registerCompletionContext(modelUri: string, context: CompletionContextType): void {
    completionContextsByModelUri.set(modelUri, context);
}

export function unregisterCompletionContext(modelUri: string): void {
    completionContextsByModelUri.delete(modelUri);
}

export function getCompletionContext(modelUri: string): CompletionContextType | undefined {
    return completionContextsByModelUri.get(modelUri);
}
