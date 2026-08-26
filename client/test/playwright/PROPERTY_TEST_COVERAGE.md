# Property test coverage analysis

Snapshot of Playwright coverage for the workflow-editor Property feature, cross-referenced with
[bytechefhq/bytechef#3932](https://github.com/bytechefhq/bytechef/issues/3932) and its sub-issues.

Date: 2026-07-29

## Existing specs

All live in `client/test/playwright/tests/properties/` (~2950 lines, 106 tests total).

| Spec                                | Node exercised         | Tests | Sub-issue            |
| ----------------------------------- | ---------------------- | ----- | -------------------- |
| `propertyPersistence.spec.ts`       | `var_1` (`var/v1/set`) | 12    | #3933 closed         |
| `propertyValidation.spec.ts`        | `propertyTesting_1`    | 25    | #3935 closed         |
| `objectProperty.spec.ts`            | `var_1`                | 13    | #4149 closed         |
| `arrayProperty.spec.ts`             | `var_1`                | 17    | #4150 closed         |
| `propertyDisplayConditions.spec.ts` | `propertyTesting_1`    | 8     | #3937 / #4151 closed |
| `propertyDynamicProperties.spec.ts` | `propertyTesting_1`    | 9     | #4152                |
| `propertyOptions.spec.ts`           | `propertyTesting_1`    | 22    | untracked            |

Open sub-issues:

- **#4152** dynamic properties — covered by `propertyDynamicProperties.spec.ts`; close once merged
- **#3938** E2E integration flows — low value, existing specs already cover reload/reopen persistence

Closed but partially covered:

- **#3937** display conditions — nested conditions still uncovered
- **#4151** displayCondition properties — the `[index]` replacement case is still uncovered

### Still missing for #3937 / #4151

`PropertyTestingAction` has no array item or object subproperty carrying a `displayCondition`, so these
cannot be written yet:

- display conditions inside nested objects
- display conditions inside array items, including the `[index]` → `[0]` / `[1]` condition-key
  replacement in `useProperty.ts:310-330`
- `ARRAY` / `OBJECT` properties with a display condition, which deliberately skip the skeleton branch
  (`useProperty.ts:1689-1697`)

Adding an array-of-objects property whose subproperty uses `displayCondition("someArray[index].flag == true")`
to the test component would unblock all three.

## Test component is underused

`server/libs/modules/components/property-testing/.../PropertyTestingAction.java` defines 31 properties.
8 of them are never referenced by any spec:

```
arrayDefaultValues           objectDefaultValues
arrayNoDefaultValues         objectNoDefaultValues
arrayPredefinedProperties    objectNoPredefinedProperties
arrayNoPredefinedProperties  objectPredefinedProperties
```

The object and array specs only exercise `var_1`'s dynamic (user-added) properties. The
predefined-properties and default-value variants of arrays and objects are untested.

## Property.tsx branch gaps

`Property.tsx` splits into two halves. Every Playwright test currently hits only the `!control` half.

### `control` half — zero E2E coverage

The react-hook-form branch used by cluster elements / AI-agent tools mode. Roughly 200 lines covering
`controlledDynamicMode`, the fromAi toggle, the "Automatically defined by the model" overlay, the
expression prefix, `FormControlledArrayItems` and `FormControlledObjectEntries`, and the tools-mode
input-type switch. Only Vitest slice tests exist for its pieces. This is the largest hole.

### `!control` half — untested branches

- static-options `PropertySelect` (`Property.tsx:795`) — only the BOOLEAN variant is exercised, via the
  `bool` property in `propertyDisplayConditions.spec.ts`
- `PropertyMentionsInput` (`Property.tsx:211`) — data pills, `=` expression mode, formula mode
- input-type-switch button, TIME clear button, description tooltip, `RequiredMark`
- `CODE_EDITOR`, `JSON_SCHEMA_BUILDER`, `TEXT_AREA`, `NULL` control types
- `FILE_ENTRY` — a single render assertion in `objectProperty.spec.ts`, no edit or persist coverage

There is also no `Property.test.tsx`; the component is only covered indirectly through hook-slice
Vitest files under `properties/hooks/tests/`.

## Blocker for the remaining #3935 boxes

Three unchecked boxes on #3935 cannot be written against the current test component:

- required-field validation
- email format validation
- URL format validation

`PropertyTestingAction` declares no `.required(true)` property and no `controlType` overrides. Server-side
additions are needed first: a required property, `EMAIL`, `URL`, `TEXT_AREA`, `NULL`, and a code-editor
property.

## Suggested order of work

1. ~~**`propertyDisplayConditions.spec.ts`** — merges #3937 and #4151.~~ Done, 8 tests.
2. ~~**`propertyDynamicProperties.spec.ts`** — #4152.~~ Done, 9 tests.
3. ~~**`propertyOptions.spec.ts`** — combobox, multiselect, options lookup dependencies.~~ Done,
   22 tests.
4. **`propertyDefaults.spec.ts`** — default-value and predefined-properties array/object variants.
5. **Cluster-element / tools-mode spec** — the `control` half. Needs an AI-agent workflow fixture;
   the biggest gap but also the most expensive to set up.

#3938 can be closed as covered.

## Behaviour uncovered by `propertyOptions.spec.ts`

- Clearing every multiselect option removes the parameter from the workflow definition rather than
  storing an empty array. The spec asserts the current behaviour (`toBeUndefined()`).
- The per-badge remove and "+ N more" clear buttons in `MultiSelect.tsx` were inert: the icons were
  direct `svg` children of `Badge`, whose base style sets `[&>svg]:pointer-events-none`, which beat the
  `[&_svg]:pointer-events-auto` on the trigger button. Both icons are now wrapped in a clickable `span`.
