# ByteChef Agent Instructions

This file provides context and guidelines for working with the ByteChef client codebase. It's automatically loaded by Cursor to maintain consistency across the team.

## Project Overview

ByteChef is a workflow automation platform. The client is a React application built with:

-   **Framework**: React with TypeScript
-   **Build Tool**: Vite
-   **State Management**: Zustand
-   **Data Fetching**: TanStack Query (React Query)
-   **Routing**: React Router DOM
-   **Forms**: React Hook Form + Zod
-   **UI Components**: Radix UI + shadcn/ui
-   **Styling**: Tailwind CSS
-   **Testing**: Vitest (unit/component) + Playwright (E2E)
-   **Internationalization**: Lingui
-   **Code Generation**: GraphQL Codegen for TypeScript + React Query hooks
-   **Workflow Editor**: Built with `@xyflow/react` for node-based workflow visualization
-   **Rich Text Inputs**: Built with TipTap (ProseMirror-based) for rich text editing

## Code Style & Formatting

### Formatting Rules

-   **Indentation**: 4 spaces (not tabs)
-   **Quotes**: Single quotes for strings
-   **Semicolons**: Always use semicolons
-   **Trailing Commas**: ES5 style (objects, arrays, function parameters)
-   **Bracket Spacing**: NO spaces inside curly braces: `{foo}` is correct, `{ foo }` is wrong
-   **Line Width**: Let Prettier handle it (default 80-100)
-   **Empty Lines Between Invocations**: Add empty lines between function/method invocations for better readability
-   **Single-Statement Functions**: Omit unnecessary curly braces in single-statement arrow functions

### Naming Conventions

-   **No Abbreviations**: Use full, descriptive names instead of abbreviations
    -   ❌ `e` for event → ✅ `event`
    -   ❌ `err` for error → ✅ `error`
    -   ❌ `res` for response → ✅ `response`
    -   ❌ `req` for request → ✅ `request`
    -   ❌ `val` for value → ✅ `value`
    -   ❌ `idx` for index → ✅ `index`
-   **Method Parameters**: Use descriptive names derived from context, not single letters
    -   ❌ `(a, b) => a + b` → ✅ `(firstNumber, secondNumber) => firstNumber + secondNumber`
    -   ❌ `(acc, item) => {...}` → ✅ `(accumulator, item) => {...}` or `(result, item) => {...}`
    -   ❌ `(e) => {...}` → ✅ `(event) => {...}`
    -   ❌ `(err) => {...}` → ✅ `(error) => {...}`
-   **Context-Derived Names**: Variable names should reflect their purpose and context
    -   In a reduce function: `accumulator` or `result` instead of `acc`
    -   In a map function: `item` or `element` instead of `x` or `e`
    -   In event handlers: `event` instead of `e`
    -   In error handlers: `error` instead of `err`

### TypeScript Conventions

-   **Strict Mode**: Always enabled
-   **Type Naming**:
    -   Interfaces: PascalCase with suffix `I` or `Props` (e.g., `AuthenticationI`, `ButtonProps`)
    -   Type Aliases: PascalCase with suffix `Type` (e.g., `ButtonPropsType`)
-   **File Naming**:
    -   Components: PascalCase (e.g., `Button.tsx`, `Login.tsx`)
    -   Utilities/Hooks: camelCase (e.g., `test-utils.tsx`, `useAnalytics.ts`)
    -   Tests: Same as source file with `.test.tsx` or `.spec.ts` suffix

### Import Organization

-   Group imports in this order (enforced by ESLint):
    1. External libraries (React, third-party)
    2. Internal absolute imports with `@/` alias
    3. Relative imports
-   Sort imports alphabetically within each group
-   Sort destructured imports alphabetically
-   Use `useShallow` from `zustand/react/shallow` when selecting multiple store values

### ESLint Rules

-   Custom ByteChef rules enforce:
    -   Empty line between JSX elements
    -   Import grouping and sorting
    -   No conditional object keys
    -   No duplicate imports
    -   No `.length` in JSX expressions
    -   Ref names must end with `Ref` suffix
    -   State variables must follow naming pattern
-   TypeScript naming conventions for interfaces and types
-   React hooks rules (exhaustive deps)
-   Tailwind CSS class ordering

## Component Patterns

### Component Structure

```typescript
// 1. Imports (grouped and sorted)
import {useState} from 'react';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';

// 2. Types/Interfaces
interface ComponentProps {
    // props
}

// 3. Component (functional component with forwardRef if needed)
const Component = React.forwardRef<HTMLDivElement, ComponentProps>(({prop1, prop2}, ref) => {
    // 4. Hooks
    const {value} = useAuthenticationStore(
        useShallow((state) => ({
            value: state.value,
        }))
    );

    // 5. State
    const [localState, setLocalState] = useState();

    // 6. Handlers
    const handleClick = () => {};

    // 7. Effects
    useEffect(() => {}, []);

    // 8. Render
    return <div ref={ref}>Content</div>;
});

Component.displayName = 'Component';

export default Component;
```

### Component Guidelines

-   Use functional components with hooks
-   Use `React.forwardRef` when component needs to forward refs
-   Always set `displayName` for forwardRef components
-   Use `twMerge()` utility for conditional class names
-   Prefer composition over configuration
-   Extract complex logic into custom hooks

### UI Components

-   Base UI components are in `src/components/ui/` (shadcn/ui)
-   Custom components in `src/components/`
-   Use Radix UI primitives for accessible components
-   Use `class-variance-authority` (cva) for variant-based styling
-   Components should accept `className` prop and merge with `twMerge()`

### Workflow Editor

-   Built with `@xyflow/react` for node-based workflow visualization
-   Location: `src/pages/platform/workflow-editor/`
-   Uses React Flow for drag-and-drop node editing
-   Nodes represent workflow tasks, edges represent connections
-   Custom node types and handles for workflow-specific functionality

### Rich Text Inputs

-   Built with TipTap (ProseMirror-based) for rich text editing
-   Used for property inputs that require rich text formatting
-   TipTap extensions: document, paragraph, text, mention, placeholder
-   Custom mention system for property references

## State Management

### Zustand Stores

-   Store location: `src/shared/stores/`
-   Pattern:

```typescript
import {createStore, useStore} from 'zustand';
import {devtools} from 'zustand/middleware';
import {ExtractState} from 'zustand/vanilla';

export interface StoreI {
    // state properties
    // action methods
}

export const store = createStore<StoreI>()(
    devtools(
        (set, get) => ({
            // initial state
            // actions using set() and get()
        }),
        {name: 'store-name'}
    )
);

export function useStore<U>(selector: (state: ExtractState<typeof store>) => U): U {
    return useStore(store, selector);
}
```

### Store Usage

-   Use `useShallow` when selecting multiple values to prevent unnecessary re-renders:

```typescript
const {value1, value2} = useStore(
    useShallow((state) => ({
        value1: state.value1,
        value2: state.value2,
    }))
);
```

-   Main stores:
    -   `useAuthenticationStore` - User authentication state
    -   `useEnvironmentStore` - Environment selection (Development/Staging/Production)
    -   `useApplicationInfoStore` - Application configuration and info
    -   `useFeatureFlagsStore` - Feature flag toggles

## Data Fetching

### GraphQL + React Query

-   GraphQL queries in `src/graphql/` organized by domain
-   Generated hooks in `src/shared/middleware/` (auto-generated, don't edit)
-   Use React Query hooks from generated code:

```typescript
import {useProjectsQuery} from '@/shared/middleware/automation/configuration';

const {data, isLoading, error} = useProjectsQuery();
```

### Mutations

-   Mutations in `src/shared/mutations/` organized by domain
-   Use React Query's `useMutation` with generated types:

```typescript
import {useCreateProjectMutation} from '@/shared/mutations/automation/projects.mutations';

const mutation = useCreateProjectMutation({
    onSuccess: (data) => {
        // handle success
    },
});

mutation.mutate({projectData});
```

### Query Keys

-   Query keys defined in `src/shared/queries/` organized by domain
-   Use consistent key structure for cache invalidation

## Form Handling

### React Hook Form + Zod

-   Define Zod schema first:

```typescript
const formSchema = z.object({
    email: z.string().email().min(5, 'Email is required'),
    password: z.string().min(8, 'Password must be at least 8 characters'),
});
```

-   Use with React Hook Form:

```typescript
import {useForm} from 'react-hook-form';
import {zodResolver} from '@hookform/resolvers/zod';

const form = useForm<z.infer<typeof formSchema>>({
    defaultValues: {
        email: '',
        password: '',
    },
    resolver: zodResolver(formSchema),
});

// In JSX
<Form {...form}>
    <FormField
        control={form.control}
        name="email"
        render={({field}) => (
            <FormItem>
                <FormLabel>Email</FormLabel>
                <FormControl>
                    <Input {...field} />
                </FormControl>
                <FormMessage />
            </FormItem>
        )}
    />
</Form>;
```

## File Organization

### Directory Structure

```
src/
├── components/        # Reusable UI components
│   ├── ui/            # shadcn/ui base components
│   └── [Component]/   # Component folders with .tsx, .test.tsx, .stories.tsx
├── pages/             # Page components (route-level)
│   ├── account/       # Account-related pages
│   ├── automation/    # Automation feature pages
│   └── platform/      # Platform feature pages
├── shared/            # Shared code across the app
│   ├── components/    # Shared components
│   ├── hooks/         # Custom React hooks
│   ├── stores/        # Zustand stores
│   ├── queries/       # React Query query keys
│   ├── mutations/     # React Query mutations
│   ├── middleware/    # Generated GraphQL types and hooks
│   ├── util/          # Utility functions
│   └── constants.tsx  # App-wide constants
├── graphql/           # GraphQL query/mutation files
├── hooks/             # App-level hooks
├── mocks/             # MSW mocks for testing
└── styles/            # Global styles

test/
└── playwright/        # Playwright E2E tests
    ├── pages/         # Pages split into module-like units
    ├── tests/         # Tests split into module-like units
    ├── utils/         # Test helper functions
    └── fixtures/      # Test fixtures
```

### Path Aliases

-   `@/` maps to `src/`
-   Always use `@/` for imports from `src/`
-   Example: `import {Button} from '@/components/ui/button';`

## Testing

### Unit/Component Tests (Vitest)

-   Location: Co-located with components (e.g., `Button.test.tsx` next to `Button.tsx`)
-   Use Testing Library: `@testing-library/react`, `@testing-library/user-event`
-   Test utilities: `src/shared/util/test-utils.tsx`
-   Setup file: `.vitest/setup.ts`
-   Use MSW for API mocking in tests
-   Pattern:

```typescript
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {expect, it} from 'vitest';

it('should render button', () => {
    render(<Button>Click me</Button>);
    expect(screen.getByRole('button', {name: /click me/i})).toBeInTheDocument();
});
```

### E2E Tests (Playwright)

-   Location: `tests/e2e/`
-   Use helpers from `tests/e2e/helpers/auth.ts` for authentication
-   Pattern:

```typescript
import {test, expect} from '@playwright/test';
import {login} from './helpers/auth';

test('should login', async ({page}) => {
    await login(page, 'user@example.com', 'password');
    await expect(page).toHaveURL('/');
});
```

## Common Utilities

### `twMerge()` - Class Name Utility

-   Location: `tailwind-merge` package
-   Merges Tailwind classes and resolves conflicts
-   Always use this instead of template literals for class names

```typescript
import {twMerge} from 'tailwind-merge';

<div className={twMerge('base-class', condition && 'conditional-class', className)} />;
```

### Constants

-   Location: `src/shared/constants.tsx`
-   Important constants:
    -   `DEVELOPMENT_ENVIRONMENT = 0`
    -   `STAGING_ENVIRONMENT = 1`
    -   `PRODUCTION_ENVIRONMENT = 2`
    -   `AUTHORITIES` - User roles
    -   `VALUE_PROPERTY_CONTROL_TYPES` - Form control types

## Routing

### Route Structure

-   Routes defined in `src/routes.tsx`
-   Public routes: `/login`, `/register`, `/password-reset`, etc.
-   Private routes require authentication via `PrivateRoute` component
-   Access control via `AccessControl` component for specific authorities
-   Embedded routes: `/embedded/*` for embedded workflow builder

### Navigation

-   Use React Router's `useNavigate()` hook
-   Use `Link` component for internal navigation
-   Handle authentication redirects in route loaders

## Internationalization (i18n)

### Lingui

-   Use `t` macro for translations: `import {t} from '@lingui/macro';`
-   Extract strings: `npm run lingui:extract`
-   Compile: `npm run lingui:compile`
-   Translation files: `src/locales/[lang]/messages.po`

## Development Workflow

### Scripts

-   `npm run dev` - Start dev server (Vite on 127.0.0.1:5173)
-   `npm run build` - Production build
-   `npm run test` - Run Vitest tests
-   `npm run test:e2e` - Run Playwright tests
-   `npm run lint` - Run ESLint
-   `npm run format` - Format with Prettier + fix ESLint
-   `npm run typecheck` - TypeScript type checking
-   `npm run codegen` - Generate GraphQL types and hooks

### Environment

-   Backend API: `localhost:9555` (proxied through Vite)
-   Dev server: `127.0.0.1:5173`
-   Environment variables: `.env.local` (not committed)
-   Feature flags: Set in `.env.local` with `VITE_FF_*` prefix

## Important Patterns

### Error Handling

-   Use Error Boundaries for component error handling
-   Use React Query's error states for data fetching errors
-   Show user-friendly error messages

### Loading States

-   Use React Query's `isLoading` for data fetching
-   Use `PageLoader` component for page-level loading
-   Use skeleton loaders for better UX

### Feature Flags

-   Check flags: `const ff_1234 = useFeatureFlagsStore()('ff-1234');`
-   Conditionally render based on flags
-   Flags defined in backend, accessed via `useApplicationInfoStore`

### Authentication

-   Check auth: `useAuthenticationStore((state) => state.authenticated)`
-   Login: `useAuthenticationStore((state) => state.login)(email, password, rememberMe)`
-   Public routes don't require auth
-   Private routes automatically redirect to `/login` if not authenticated

## Anti-Patterns to Avoid

1. ❌ Don't use spaces in curly braces: `{ foo }` → Use `{foo}`
2. ❌ Don't create new stores for simple local state → Use `useState`
3. ❌ Don't mutate Zustand state directly → Use `set()` function
4. ❌ Don't use template literals for class names → Use `twMerge()`
5. ❌ Don't import from `src/` directly → Use `@/` alias
6. ❌ Don't skip `useShallow` when selecting multiple store values
7. ❌ Don't write tests without using test utilities from `test-utils.tsx`
8. ❌ Don't forget to set `displayName` on forwardRef components
9. ❌ Don't use `.length` in JSX expressions → Extract to variable
10. ❌ Don't create conditional object keys → Use separate objects
11. ❌ Don't use abbreviations for variables/parameters: `e`, `err`, `res`, `req`, `val`, `idx` → Use full names: `event`, `error`, `response`, `request`, `value`, `index`
12. ❌ Don't use single-letter parameters in methods: `(a, b)`, `(acc, item)` → Use descriptive names: `(firstNumber, secondNumber)`, `(accumulator, item)`
13. ❌ Don't use em dashes (—) or en dashes (–) → Use regular hyphens (-) instead
14. ❌ Don't add comments, only allowed comments are just above a `useEffect` explaining their behaviour in human language

## Code Generation

### GraphQL Codegen

-   Config: `codegen.ts`
-   Generates TypeScript types and React Query hooks
-   Run `npm run codegen` after updating GraphQL files
-   Generated files in `src/shared/middleware/` (don't edit manually)

## Storybook

-   Component stories in `*.stories.tsx` files
-   Run: `npm run storybook`
-   Build: `npm run build-storybook`
-   Stories help document component usage and variants

## Additional Notes

-   The app supports both Automation and Embedded modes (feature flag controlled)
-   Monaco Editor is used for code editing (YAML, JSON, etc.)
-   Workflow editor is built with `@xyflow/react` for node-based workflow visualization and editing
-   Rich text inputs use TipTap (ProseMirror-based) for formatted text editing with mentions
-   PostHog is used for analytics (conditionally loaded)
-   CommandBar is used for help hub (conditionally loaded)
-   MSW (Mock Service Worker) is used for API mocking in tests
