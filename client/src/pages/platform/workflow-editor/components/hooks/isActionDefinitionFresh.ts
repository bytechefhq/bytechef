type ActionDefinitionFreshnessCandidateType = {
    componentName?: string;
    componentVersion?: number;
    name?: string;
};

type ComponentDefinitionReferenceType = {
    name?: string;
    version?: number;
};

// Two components can expose actions that share an operation name (e.g. OpenRouter/ask and
// OpenAI/ask). Matching only on name causes the panel to reuse the previous component's
// definition when the user switches nodes, so the guard must also check componentName and
// componentVersion before short-circuiting the fetch.
export default function isActionDefinitionFresh(
    currentActionDefinition: ActionDefinitionFreshnessCandidateType | undefined,
    currentComponentDefinition: ComponentDefinitionReferenceType | undefined,
    currentOperationName: string
): boolean {
    return (
        currentActionDefinition?.name === currentOperationName &&
        currentActionDefinition?.componentName === currentComponentDefinition?.name &&
        currentActionDefinition?.componentVersion === currentComponentDefinition?.version
    );
}
