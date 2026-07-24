# Property test coverage analysis

Snapshot of Playwright coverage for the workflow-editor Property feature, cross-referenced with
[bytechefhq/bytechef#3932](https://github.com/bytechefhq/bytechef/issues/3932) and its sub-issues.

Date: 2026-07-23

## Existing specs

All live in `client/test/playwright/tests/properties/` (~2050 lines total).

| Spec                                | Node exercised         | Sub-issue     |
| ----------------------------------- | ---------------------- | ------------- |
| `propertyPersistence.spec.ts`       | `var_1` (`var/v1/set`) | #3933 closed  |
| `propertyValidation.spec.ts`        | `propertyTesting_1`    | #3935 closed  |
| `objectProperty.spec.ts`            | `var_1`                | #4149 closed  |
| `arrayProperty.spec.ts`             | `var_1`                | #4150 closed  |
| `propertyDisplayConditions.spec.ts` | `propertyTesting_1`    | #3937 / #4151 |

Open sub-issues:

- **#3937** display conditions — covered by `propertyDisplayConditions.spec.ts`, except nested conditions
- **#4151** displayCondition properties — same spec; the `[index]` replacement case is still uncovered
- **#4152** dynamic properties
- **#3938** E2E integration flows — low value, existing specs already cover reload/reopen persistence

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
14 of them are never referenced by any spec:

```
arrayDefaultValues           objectDefaultValues            optionsMultiselect
arrayNoDefaultValues         objectNoDefaultValues          optionsNoMultiselect
arrayPredefinedProperties    objectNoPredefinedProperties   optionsLookupDependsOn
arrayNoPredefinedProperties  objectPredefinedProperties     setForOptionsLookup
displayCondition             dynamicPropertiesLookup
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

- `PropertyComboBox` with `optionsLookupDependsOn` (`Property.tsx:824`)
- `PropertyMultiSelect` (`Property.tsx:888`)
- static-options `PropertySelect` (`Property.tsx:795`)
- `DYNAMIC_PROPERTIES` (`Property.tsx:913`) — #4152
- displayCondition skeleton and hide branches (`Property.tsx:171-183`) — #3937 / #4151
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
2. **`propertyDynamicProperties.spec.ts`** — #4152. `dynamicPropertiesLookup` → `dynamicProperty` is
   wired against the real backend, no mocks needed.
3. **`propertyOptions.spec.ts`** — not tracked by any sub-issue. Covers combobox, multiselect, and
   options lookup dependencies. Large untracked gap.
4. **`propertyDefaults.spec.ts`** — default-value and predefined-properties array/object variants.
5. **Cluster-element / tools-mode spec** — the `control` half. Needs an AI-agent workflow fixture;
   the biggest gap but also the most expensive to set up.

#3938 can be closed as covered.
