interface UserGuidingSdkI {
    c(name: string): (...arguments_: unknown[]) => void;
    finishPreview(...arguments_: unknown[]): void;
    hideChecklist(...arguments_: unknown[]): void;
    identify(id: string | undefined, properties: Record<string, unknown>): void;
    launchChecklist(...arguments_: unknown[]): void;
    previewGuide(...arguments_: unknown[]): void;
    q: unknown[];
    track(eventName: string, properties?: Record<string, unknown>): void;
}

interface Window {
    userGuiding?: UserGuidingSdkI;
    userGuidingLayer?: unknown[];
}
