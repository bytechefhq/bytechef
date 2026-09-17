# Component Wrapper Blueprint

How to build a ByteChef design-system component as a thin wrapper around a shadcn/ui
primitive. Reference exemplars, already merged, live at:

- [Button](../../client/src/components/Button/Button.tsx)
- [Badge](../../client/src/components/Badge/Badge.tsx)
- [Switch](../../client/src/components/Switch/Switch.tsx)

Follow this document when generating a new wrapper. Every rule here is derived from those
three files plus [CLAUDE.md](../../CLAUDE.md); when in doubt, open the closest exemplar and
match it.

---

## 1. What a wrapper is (and is not)

The base primitives in `client/src/components/ui/*` are stock shadcn: they use `cva`,
generic shadcn tokens (`bg-primary`, `text-primary-foreground`), `cn()`, and `data-slot`
attributes. **Do not edit them.**

A wrapper sits in `client/src/components/<Name>/<Name>.tsx` and does three jobs:

1. **Re-skins** — fully discards shadcn's variant classes and re-maps every variant/size to
   ByteChef semantic tokens (`bg-surface-*`, `text-content-*`, `border-stroke-*`).
2. **Ergonomic API** — replaces loose props with a house-shaped API: named `variant` /
   `styleType` / `size` string unions, plus `label` / `icon` / `children` combinations
   modeled as **discriminated unions** so illegal combinations fail at compile time.
3. **Stable surface** — the wrapper is what the rest of the app imports. The app never
   imports from `components/ui/*` directly.

Layering: `radix / native element` → `components/ui/<name>` (stock shadcn) → `components/<Name>/<Name>` (this wrapper) → app.

---

## 2. Canonical file structure

Each wrapper is a directory of three co-located files. All three are mandatory.

```
client/src/components/<Name>/
  <Name>.tsx          # the wrapper
  <Name>.stories.tsx  # Storybook (@storybook/react-vite), autodocs
  <Name>.test.tsx     # vitest + @testing-library, incl. @ts-expect-error type tests
```

---

## 3. Hard rules (non-negotiable)

These come from the exemplars and `CLAUDE.md`. Breaking any of them fails review or lint.

| # | Rule |
|---|------|
| 1 | Import the base as `Shadcn<Name>`: `import {Switch as ShadcnSwitch} from '@/components/ui/switch'`. |
| 2 | Merge classes with `twMerge` from `tailwind-merge`. **Never** `cn()` — `cn` is for the `ui/*` base layer only. |
| 3 | Refs are **props**, not `forwardRef`. Accept `ref` via `React.ComponentPropsWithRef<...>` and spread it through. (See §5 — this is a deliberate divergence from the older Button/Switch exemplars.) |
| 4 | Set `<Name>.displayName = '<Name>'`. |
| 5 | `export default <Name>;` — and also `export type {<Name>Props};` (named type export). |
| 6 | Re-map **all** variants to semantic tokens. Do not pass shadcn's `variant`/`size` through. |
| 7 | Model `label` / `icon` / `children` / `aria-label` combinations as a discriminated union of interfaces (see §4). |
| 8 | Keep a single `basicStyles` string for shadcn resets (`shadow-none`, `[&_svg]:size-*`, etc.). |
| 9 | Class-map records (`variants`, sizes) are typed `Record<SomeUnionType, string>`. When keys break alphabetical order, add `// eslint-disable-next-line sort-keys` on the offending line — do not reorder to satisfy the token design. |
| 10 | Descriptive names everywhere — no `e`, `el`, `v`. Loop/arrow params included (CLAUDE.md). |
| 11 | Lucide icons imported with the `Icon` suffix (`CheckIcon`, not `Check`). |
| 12 | Object keys in stories/tests sorted ascending (ESLint `sort-keys`, not auto-fixable). |
| 13 | Prop type name ends in `Props` or `I`; the wrapper's exported union is `<Name>Props`. |

---

## 4. The discriminated-union prop pattern

This is the core ergonomic move. A shared base interface holds the styling props; concrete
member interfaces express the legal content shapes and use `never` to forbid the illegal
ones. Type them so that `@ts-expect-error` tests in §8 hold.

```tsx
// Base: strip the shadcn props you are replacing, keep ref via ComponentPropsWithRef.
interface BaseBadgeProps extends Omit<React.ComponentPropsWithRef<typeof ShadcnBadge>, 'variant'> {
    className?: string;
    styleType?: StyleType;
    weight?: WeightType;
}

// Members: each legal content shape; `never` forbids the rest.
interface TextBadgeProps extends BaseBadgeProps {
    'aria-label'?: never;   // text is its own accessible name
    children?: never;
    icon?: never;
    label: string;
}

interface IconBadgeProps extends BaseBadgeProps {
    'aria-label': string;   // icon-only REQUIRES an accessible name
    children?: never;
    icon: React.ReactElement;
    label?: never;
}
// ...IconTextBadgeProps, CustomBadgeProps (children) ...

type BadgePropsType = TextBadgeProps | IconBadgeProps | IconTextBadgeProps | CustomBadgeProps;
```

Guidelines:

- **Every component** with more than one content shape (text vs icon vs custom children)
  gets a union. A leaf with a single shape (e.g. a plain wrapper that only re-skins) can use
  a single interface.
- Icon-only variants **must** require `'aria-label': string`; text variants forbid it
  (`'aria-label'?: never`) because the text is the accessible name.
- `label` and `children` are mutually exclusive; when both are legal in different members,
  `label` wins at render time (`label ?? children`).

---

## 5. Ref handling — React 19 ref-as-prop

New wrappers use React 19's ref-as-prop. **Do not use `React.forwardRef`.** All three
exemplars now follow this pattern — `ref` arrives as a normal prop and flows through `{...props}`.

- Extend `React.ComponentPropsWithRef<typeof Shadcn<Name>>` (this types `ref` correctly).
- The base shadcn components forward `...props` onto their root, so `ref` flows through by
  spreading. No explicit `ref` destructure is needed unless you must attach it to a specific
  inner element.
- `displayName` is still required (dev tools / lint).

```tsx
function Switch({className, variant = 'default', ...props}: SwitchPropsType) {
    return <ShadcnSwitch className={twMerge(basicStyles, variants[variant], className)} {...props} />;
}

Switch.displayName = 'Switch';
```

---

## 6. Annotated wrapper template

Copy this skeleton and fill in the component's real variants, sizes, and tokens. `<Name>` /
`<name>` are placeholders.

```tsx
import {<Name> as Shadcn<Name>} from '@/components/ui/<name>';
import React from 'react';
import {twMerge} from 'tailwind-merge';

// ---- Prop types -----------------------------------------------------------

interface Base<Name>Props extends Omit<React.ComponentPropsWithRef<typeof Shadcn<Name>>, 'variant' | 'size'> {
    className?: string;
    size?: SizeType;
    variant?: VariantType;
}

// If the component has distinct content shapes, split into a discriminated union here.
// Otherwise a single interface is fine:
type <Name>PropsType = Base<Name>Props;

// ---- Design vocabulary (house names, not shadcn names) --------------------

type VariantType = 'default' | 'secondary' | 'destructive' /* | ... */;
type SizeType = 'default' | 'sm' | 'lg' /* | ... */;

// ---- Class maps (semantic tokens only) ------------------------------------

// Shadcn resets shared by every instance.
const basicStyles = 'shadow-none hover:shadow-none [&_svg]:size-4';

const sizes: Record<SizeType, string> = {
    lg: 'h-10 px-8 py-2',
    // eslint-disable-next-line sort-keys
    default: 'h-9 px-4 py-2',
    sm: 'h-8 px-3 py-2 text-xs',
};

const variants: Record<VariantType, string> = {
    default:
        'bg-surface-brand-primary hover:bg-surface-brand-primary-hover active:bg-surface-brand-primary-active text-content-onsurface-primary',
    secondary:
        'bg-surface-neutral-secondary hover:bg-surface-neutral-secondary-hover text-content-neutral-primary',
    // eslint-disable-next-line sort-keys
    destructive:
        'bg-surface-destructive-primary hover:bg-surface-destructive-primary-hover text-content-onsurface-primary',
};

// ---- Component ------------------------------------------------------------

function <Name>({className, size = 'default', variant = 'default', ...props}: <Name>PropsType) {
    return (
        <Shadcn<Name>
            className={twMerge(basicStyles, sizes[size], variants[variant], className)}
            {...props}
        />
    );
}

<Name>.displayName = '<Name>';

export default <Name>;
export type {<Name>PropsType as <Name>Props};
```

Rendering `label`/`icon` (when the component has content, like Button/Badge):

```tsx
return (
    <Shadcn<Name> className={twMerge(basicStyles, ...)} {...props}>
        {icon}

        {label ?? children}
    </Shadcn<Name>>
);
```

For composite layouts (label + description + alignment, like Switch's `box`/`start`/`end`),
factor the text block into a small local sub-component (`TextBlock`) rather than inlining
branches. Keep it in the same file.

---

## 7. Semantic design tokens

Never hardcode colors or use shadcn's generic tokens (`bg-primary`, `text-foreground`).
Always use ByteChef semantic tokens. They auto-adapt to light/dark.

**Source of truth:**
- CSS variables (light + dark values): [client/src/styles/index.css](../../client/src/styles/index.css)
- Tailwind class mapping: [client/tailwind.config.js](../../client/tailwind.config.js)

**Token families** (pattern: `<utility>-<group>-<role>-<rank>[-state]`):

| Prefix | Use | Examples |
|--------|-----|----------|
| `bg-surface-*` | backgrounds/fills | `bg-surface-brand-primary`, `bg-surface-neutral-secondary`, `bg-surface-destructive-primary` |
| `text-content-*` | text/icon color | `text-content-onsurface-primary`, `text-content-neutral-primary`, `text-content-brand-primary` |
| `border-stroke-*` | borders/rings | `border-stroke-neutral-secondary`, `border-stroke-brand-primary` |

**States** are suffixes on the same token, used with Tailwind pseudo-prefixes:
`hover:bg-surface-brand-primary-hover`, `active:bg-surface-brand-primary-active`,
`focus-visible:ring-stroke-brand-focus`.

Semantic groups: `brand`, `neutral`, `success`, `warning`, `destructive`, plus `onsurface`
(content that sits on a filled surface). Before inventing a class, grep
[index.css](../../client/src/styles/index.css) to confirm the token exists.

---

## 8. Test template (`<Name>.test.tsx`)

vitest + Testing Library. Import `render`/`screen` from the project's `test-utils` (wraps a
React Query client) — **not** from `@testing-library/react` directly.

Cover, in this order:
1. Default render (content present, default tokens applied).
2. Each `variant`/`styleType` maps to the expected token classes (`toHaveClass`).
3. Each `size` maps to expected sizing classes.
4. `icon` renders (`container.querySelector('svg')`, `toHaveClass('lucide-*')`).
5. `className` merges through `twMerge` (custom class present alongside defaults).
6. A `describe('TypeScript tests')` block with `@ts-expect-error` for every illegal prop
   combination the discriminated union forbids.

```tsx
import {render, screen} from '@/shared/util/test-utils';
import {CheckIcon} from 'lucide-react';
import {describe, expect, it} from 'vitest';

import <Name> from './<Name>';

it('should render default <name> with expected tokens', () => {
    render(<<Name> label="Hello" />);

    expect(screen.getByText('Hello')).toHaveClass('bg-surface-brand-primary text-content-onsurface-primary');
});

describe('<Name> variants', () => {
    it('should apply destructive tokens when variant is destructive', () => {
        render(<<Name> label="Hello" variant="destructive" />);

        expect(screen.getByText('Hello')).toHaveClass('bg-surface-destructive-primary');
    });
});

describe('TypeScript tests', () => {
    it('should require aria-label for icon-only', () => {
        // @ts-expect-error - aria-label required for icon-only
        render(<<Name> icon={<CheckIcon />} />);
    });

    it('should reject label + children together', () => {
        render(
            // @ts-expect-error - label and children are mutually exclusive
            <<Name> label="Hello"><span>x</span></<Name>>
        );
    });

    it('should merge className via twMerge', () => {
        render(<<Name> className="ring-1" label="Hello" />);

        expect(screen.getByText('Hello')).toHaveClass('ring-1');
    });
});
```

Notes:
- Assert specific token classes, not layout minutiae — mirror `Badge.test.tsx`.
- `@ts-expect-error` lines are real tests: if the type stops forbidding the combo, the build
  fails. Keep one per illegal shape.
- Test method/helper names are camelCase, no underscores (Checkstyle-equivalent lint).

---

## 9. Stories template (`<Name>.stories.tsx`)

Storybook via `@storybook/react-vite`, `tags: ['autodocs']`. Model on
[Badge.stories.tsx](../../client/src/components/Badge/Badge.stories.tsx).

```tsx
import {Meta, StoryObj} from '@storybook/react-vite';
import {CheckIcon, CircleIcon, XIcon} from 'lucide-react';

import <Name> from './<Name>';

const icons = {
    CheckIcon: <CheckIcon />,
    CircleIcon: <CircleIcon />,
    XIcon: <XIcon />,
} as const;

const meta = {
    title: 'Components/<Name>',
    // eslint-disable-next-line sort-keys
    component: <Name>,
    parameters: {
        controls: {include: ['variant', 'size', 'icon', 'label', 'children']},
        docs: {description: {component: 'One-paragraph description of the component and its props.'}},
        layout: 'centered',
    },
    // eslint-disable-next-line sort-keys
    argTypes: {
        icon: {
            control: {type: 'select'},
            description: 'Icon as a ReactElement.',
            mapping: {'no-icon': undefined, ...icons},
            options: ['no-icon', ...Object.keys(icons)],
            table: {type: {summary: 'ReactElement'}},
        },
        variant: {
            control: {type: 'select'},
            description: 'Visual style.',
            options: ['default', 'secondary', 'destructive'],
            table: {type: {summary: 'string'}},
        },
    },
    tags: ['autodocs'],
} satisfies Meta<typeof <Name>>;

export default meta;
// eslint-disable-next-line @typescript-eslint/naming-convention
type Story = StoryObj<typeof <Name>>;

export const Default<Name>: Story = {
    args: {label: 'Label', variant: 'default'},
    render: (args: React.ComponentProps<typeof <Name>>) => <<Name> {...args} />,
};

// One grid story per axis: all variants, all sizes, with-icons, use-cases.
export const <Name>Variants: Story = {
    render: () => (
        <div className="flex flex-wrap items-center gap-4">
            <<Name> label="Default" variant="default" />
            <<Name> label="Secondary" variant="secondary" />
            <<Name> label="Destructive" variant="destructive" />
        </div>
    ),
};
```

Story conventions:
- Map icons/labels/custom-content into `as const` objects and expose via `argTypes` `mapping`
  + `options` (`'no-icon'` sentinel → `undefined`).
- One "showcase grid" story per prop axis (all variants, all sizes, icons, real use-cases).
- `meta.component` and `type Story` need the eslint-disable comments shown above.

---

## 10. Per-component checklist

Before considering a wrapper done:

- [ ] `<Name>.tsx`, `<Name>.stories.tsx`, `<Name>.test.tsx` all present in `components/<Name>/`.
- [ ] Base imported as `Shadcn<Name>`; nothing else imports from `ui/<name>`.
- [ ] `twMerge` used; no `cn()`.
- [ ] ref-as-prop via `ComponentPropsWithRef`; no `forwardRef`; `displayName` set.
- [ ] `export default` + `export type {<Name>Props}`.
- [ ] All variants/sizes re-mapped to semantic tokens verified against [index.css](../../client/src/styles/index.css).
- [ ] Discriminated union for content shapes; icon-only requires `aria-label`.
- [ ] `// eslint-disable-next-line sort-keys` on class-map lines that break alpha order.
- [ ] Tests cover defaults, every variant/size, icon render, className merge, and
      `@ts-expect-error` for each illegal prop combo.
- [ ] Stories have autodocs, argTypes, and one grid per axis.
- [ ] `cd client && npm run check` passes (lint + typecheck + tests).

---

## 11. Exemplars (copy freely)

All three exemplars — Button, Badge, Switch — now fully conform to this blueprint: `Shadcn*`
import alias, `twMerge`, `basicStyles`, `Record<Union,string>` class maps, discriminated
unions, `aria-label` on icon-only, React 19 ref-as-prop (§5), `export default` + named
`export type {<Name>Props}`, and co-located stories+tests. Copy any of them as a starting
point; no known divergences remain.

[Input](../../client/src/components/Input/Input.tsx) is a fourth reference — a wrapper that
does **not** use a discriminated union (single optional-adornment interface). Study it for the
gotchas in §12.

---

## 12. Gotchas (learned from the Input pilot)

Real snags that surfaced building `Input`. Check these before shipping any wrapper.

1. **Don't shadow a native attribute with a house prop.** The wrapped element may already own
   the name you want. `<input>` has a native numeric `size` attribute, so reusing `size` for
   the house scale breaks any caller that spreads a wide `{...props}` object (its `size:
   number` no longer matches `size: SizeType`). Rename the house prop (`inputSize`) and let the
   native attribute pass through. Same risk applies to `type`, `color`, `width`, `height`, etc.,
   depending on the base element. `button`/`div`/radix `Switch` have no such collision, which is
   why Button/Badge/Switch can use plain `size`/`variant`.

2. **Only use discriminated unions for genuinely exclusive content shapes.** A `never`-based
   union (`icon?: never` vs `icon: ReactElement`) cannot narrow a wide spread object: a caller
   doing `<Input {...bigProps} />` fails to match any member and TS reports a confusing "missing
   property" error. Badge/Button need the union because label / icon-only / children are
   mutually exclusive and icon-only requires `aria-label`. An **optional adornment** (an
   optional `icon` with an `iconPosition` that only styles when present) is not exclusive — put
   it on a single interface. Matches the blueprint's own "single shape → single interface" rule
   (§4).

3. **Single-interface props type must be named `<Name>Props`, not `<Name>PropsType`.** The
   `@typescript-eslint/naming-convention` rule requires an `interface` to end in `Props`/`I`.
   The `type <Name>PropsType = A | B` alias used by the union exemplars is exempt because it's a
   *type alias*, not an interface — so `export type {<Name>PropsType as <Name>Props}` only works
   for unions. For a single interface, name it `<Name>Props` directly and `export type
   {<Name>Props}`.

4. **Preserve existing named exports when a wrapper already has callers.** `Input` had ~85
   `import {Input}` sites. Adding `export default Input` for blueprint conformance while keeping
   `export {Input}` avoids a mass import migration. New wrappers only need the default export.
