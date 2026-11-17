const compareWorkflowLabels = (label?: string, otherLabel?: string) =>
    (label || '').localeCompare(otherLabel || '', undefined, {sensitivity: 'base'});

export default compareWorkflowLabels;
