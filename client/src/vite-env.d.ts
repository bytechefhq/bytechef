/// <reference types="vite/client" />
/// <reference types="@testing-library/jest-dom/vitest" />

interface ImportMetaEnvI {
    readonly VITE_FF_EMBEDDED_TYPE_ENABLED: string;
}

interface ImportMetaI {
    readonly env: ImportMetaEnvI;
}
