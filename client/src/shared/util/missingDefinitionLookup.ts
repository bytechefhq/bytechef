const DEFINITION_LOOKUP_URL_PATTERN =
    /\/component-definitions\/[^/]+\/versions\/[^/?]+(\/(action-definitions|trigger-definitions|cluster-element-definition)\/[^/?]+)?(\?|$)/;

export function isMissingDefinitionLookup(url: string, status: number): boolean {
    return status === 400 && DEFINITION_LOOKUP_URL_PATTERN.test(url);
}
