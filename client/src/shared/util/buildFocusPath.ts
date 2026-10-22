import {FocusPathElement} from '@/shared/middleware/automation/workflow/execution';

export function buildFocusPath(focusPath: FocusPathElement[]): string[] {
    const accordionValues: string[] = [];

    focusPath.forEach((element, index) => {
        if (element.iterationIndex != null && index > 0) {
            accordionValues.push(`${focusPath[index - 1].taskExecutionId}-iteration-${element.iterationIndex}`);
        }

        accordionValues.push(element.taskExecutionId);
    });

    return accordionValues;
}
