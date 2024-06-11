/// <reference types="vite/client" />

interface ImportMetaEnvI {
    readonly VITE_FF_EMBEDDED_TYPE_ENABLED: string;
}

interface ImportMetaI {
    readonly env: ImportMetaEnvI;
}
