/**
 * Builds the cache key under which a component-defined input's resolved options are stored in (and read from) the
 * `workflowInputOptions` map. The key mirrors the component-reference identity tuple
 * (`componentName`, `componentVersion`, `groupName`, the property/member name, and the resolved dependency values)
 * so that two distinct component inputs — or two group members in different components — that happen to share a
 * property/member name do not collide on the same slot. The write side that populates `workflowInputOptions` MUST
 * key with this same function.
 */
export const optionsCacheKey = (
    componentName: string,
    componentVersion: number,
    groupName: string,
    propertyName: string,
    dependencyValues: Record<string, unknown>
): string =>
    `${componentName}:${componentVersion}:${groupName}:${propertyName}:${JSON.stringify(dependencyValues ?? {})}`;
