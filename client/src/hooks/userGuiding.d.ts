interface UserGuidingAppStateI {
    api?: {
        assistants?: {id?: number | string}[];
        resourceCenters?: {id?: number | string}[];
    };
}

interface UserGuidingSdkI {
    c(name: string): (...arguments_: unknown[]) => void;
    finishPreview(...arguments_: unknown[]): void;
    getAppState?(): UserGuidingAppStateI | undefined;
    hideChecklist(...arguments_: unknown[]): void;
    hideResourceCenter?(): void;
    identify(id: string | undefined, properties: Record<string, unknown>): void;
    launchChecklist(...arguments_: unknown[]): void;
    launchResourceCenter?(resourceCenterId: number | string): void;
    previewGuide(...arguments_: unknown[]): void;
    q: unknown[];
    track(eventName: string, properties?: Record<string, unknown>): void;
}

// eslint-disable-next-line @typescript-eslint/naming-convention
interface Window {
    userGuiding?: UserGuidingSdkI;
    userGuidingLayer?: unknown[];
}
