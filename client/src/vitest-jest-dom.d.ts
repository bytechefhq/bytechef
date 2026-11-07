import type {TestingLibraryMatchers} from '@testing-library/jest-dom/matchers';

declare module 'vitest' {
    // eslint-disable-next-line @typescript-eslint/naming-convention, @typescript-eslint/no-empty-object-type
    interface Matchers<
        R extends void | Promise<void> = void | Promise<void>,
        T = unknown,
    > extends TestingLibraryMatchers<T, R> {}
}
