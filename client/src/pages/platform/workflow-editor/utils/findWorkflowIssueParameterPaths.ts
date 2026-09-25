import {WorkflowIssueI} from '../stores/useWorkflowIssuesStore';
import {isWorkflowTask} from './flattenDefinitionTasks';

const EXPRESSION_PATTERN = /\$\{([^}]*)\}/g;
const PARAMETER_PATH_ROOT_PATTERN = /^[^.[]+/;

function isNestedTaskList(value: unknown): boolean {
    if (!Array.isArray(value) || value.length === 0) {
        return false;
    }

    const firstItem = value[0];

    return (
        isWorkflowTask(firstItem) || (Array.isArray(firstItem) && firstItem.length > 0 && isWorkflowTask(firstItem[0]))
    );
}

function collectParameterPaths(
    value: unknown,
    parameterPath: string,
    matchesExpression: (expressionBody: string) => boolean,
    parameterPaths: Array<string>
): void {
    if (typeof value === 'string') {
        for (const match of value.matchAll(EXPRESSION_PATTERN)) {
            if (matchesExpression(match[1].trim())) {
                parameterPaths.push(parameterPath);

                return;
            }
        }

        return;
    }

    if (isWorkflowTask(value) || isNestedTaskList(value)) {
        return;
    }

    if (Array.isArray(value)) {
        value.forEach((item, index) =>
            collectParameterPaths(item, `${parameterPath}[${index}]`, matchesExpression, parameterPaths)
        );

        return;
    }

    if (value && typeof value === 'object') {
        for (const [key, nestedValue] of Object.entries(value)) {
            collectParameterPaths(
                nestedValue,
                parameterPath ? `${parameterPath}.${key}` : key,
                matchesExpression,
                parameterPaths
            );
        }
    }
}

function getExpressionMatcher({
    propertyPath,
    referencedNodeName,
}: Pick<WorkflowIssueI, 'propertyPath' | 'referencedNodeName'>): ((expressionBody: string) => boolean) | undefined {
    if (propertyPath) {
        return (expressionBody) => expressionBody === propertyPath;
    }

    if (referencedNodeName) {
        const escapedNodeName = referencedNodeName.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
        const referencePattern = new RegExp(`(^|[^\\w])${escapedNodeName}([^\\w]|$)`);

        return (expressionBody) => referencePattern.test(expressionBody);
    }

    return undefined;
}

export function getParameterPathRoot(parameterPath: string): string {
    return PARAMETER_PATH_ROOT_PATTERN.exec(parameterPath)?.[0] ?? parameterPath;
}

export default function findWorkflowIssueParameterPaths(
    issue: Pick<WorkflowIssueI, 'propertyPath' | 'referencedNodeName'>,
    parameters: Record<string, unknown> | undefined
): Array<string> {
    const matchesExpression = getExpressionMatcher(issue);

    if (!parameters || !matchesExpression) {
        return [];
    }

    const parameterPaths: Array<string> = [];

    collectParameterPaths(parameters, '', matchesExpression, parameterPaths);

    if (parameterPaths.length === 0 && issue.propertyPath && getParameterPathRoot(issue.propertyPath) in parameters) {
        return [issue.propertyPath];
    }

    return parameterPaths;
}

export function getIssueParameterNames(
    issues: Array<Pick<WorkflowIssueI, 'propertyPath' | 'referencedNodeName'>>,
    parameters: Record<string, unknown> | undefined
): Set<string> {
    const parameterNames = new Set<string>();

    for (const issue of issues) {
        if (issue.propertyPath) {
            parameterNames.add(getParameterPathRoot(issue.propertyPath));
        }

        for (const parameterPath of findWorkflowIssueParameterPaths(issue, parameters)) {
            parameterNames.add(getParameterPathRoot(parameterPath));
        }
    }

    return parameterNames;
}
