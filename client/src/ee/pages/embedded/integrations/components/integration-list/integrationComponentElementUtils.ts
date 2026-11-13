export interface IntegrationComponentElementI {
    description?: string;
    id: string;
    title: string;
}

const isUpperCaseLetter = (character: string) =>
    character === character.toUpperCase() && character !== character.toLowerCase();

/**
 * Mirrors the server's default MCP tool naming (AbstractToolFacade.getToolName): the upper-cased component name, an
 * underscore, then the element name with every camelCase hump split by an underscore and upper-cased, e.g. gmail +
 * sendEmail -> GMAIL_SEND_EMAIL.
 */
export const getToolName = (componentName: string, clusterElementName: string): string => {
    let toolName = `${componentName.toUpperCase()}_${clusterElementName.charAt(0).toUpperCase()}`;

    for (const character of clusterElementName.slice(1)) {
        toolName += isUpperCaseLetter(character) ? `_${character}` : character.toUpperCase();
    }

    return toolName;
};
