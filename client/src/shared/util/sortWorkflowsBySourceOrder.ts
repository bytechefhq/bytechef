const getSourcePosition = (source: string, label: string) => {
    if (!label) {
        return Number.MAX_SAFE_INTEGER;
    }

    const positions = [`"${label}"`, `'${label}'`]
        .map((quotedLabel) => source.indexOf(quotedLabel))
        .filter((position) => position >= 0);

    return positions.length > 0 ? Math.min(...positions) : Number.MAX_SAFE_INTEGER;
};

const sortWorkflowsBySourceOrder = <WorkflowType>(
    workflows: WorkflowType[],
    source: string | undefined,
    getLabel: (workflow: WorkflowType) => string
): WorkflowType[] => {
    const sourcePositions = new Map(
        workflows.map((workflow) => [workflow, getSourcePosition(source ?? '', getLabel(workflow))])
    );

    return [...workflows].sort((workflow, otherWorkflow) => {
        const positionDifference = sourcePositions.get(workflow)! - sourcePositions.get(otherWorkflow)!;

        return positionDifference !== 0
            ? positionDifference
            : getLabel(workflow).localeCompare(getLabel(otherWorkflow));
    });
};

export default sortWorkflowsBySourceOrder;
