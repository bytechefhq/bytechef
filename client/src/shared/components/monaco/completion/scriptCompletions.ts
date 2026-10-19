import {
    type ActionCallKeySlotType,
    type ActionCallValueSlotType,
    findActionCallKeySlot,
    findActionCallValueSlot,
} from './actionCallScanner';
import {parseMemberPath} from './memberPath';
import {findEnclosingFunctionName, findPerformScope} from './performScanner';
import {getPropertyCompletionItems, getPropertyValueItems} from './propertyCompletions';
import {buildActionCallSnippet, buildFunctionSnippet} from './snippets';
import {
    type EnclosingTaskType,
    findComponentConnectionPropertyNames,
    findEnclosingAction,
    findEnclosingTask,
    findTaskByPerformName,
} from './sourceScanner';

import type {Option, Property} from '@/shared/middleware/platform/configuration';

import type {
    CodeWorkflowCompletionContextI,
    ConnectionDeclarationType,
    CustomComponentCompletionContextI,
    OperationPropertyRequestType,
    PerformScopeType,
    ScriptCompletionContextI,
    ScriptLanguageType,
} from './completionTypes';

export type CompletionItemDraftType = {
    detail?: string;
    documentation?: string;
    filterText?: string;
    insertAsSnippet: boolean;
    insertText: string;
    kind: 'function' | 'module' | 'method' | 'property' | 'value' | 'variable';
    label: string;
    replaceLength?: number;
    retrigger?: boolean;
    sortText?: string;
};

export interface ScriptCompletionRequestI {
    context: ScriptCompletionContextI | CustomComponentCompletionContextI | CodeWorkflowCompletionContextI;
    cursorOffset: number;
    source: string;
}

type ContextType = ScriptCompletionRequestI['context'];

const SCRIPT_CONTEXT_MEMBERS = ['component'];
const CUSTOM_COMPONENT_CONTEXT_MEMBERS = ['http', 'log'];
const CODE_WORKFLOW_CONTEXT_MEMBERS = ['component', 'connection', 'input', 'log', 'parameters'];
const ZERO_ARGUMENT_CONTEXT_MEMBERS = new Set(['connection', 'input', 'parameters']);

function propertyItem(name: string): CompletionItemDraftType {
    return {insertAsSnippet: false, insertText: name, kind: 'property', label: name};
}

function propertyItems(names: string[]): CompletionItemDraftType[] {
    return names.map(propertyItem);
}

function contextMembers(context: ContextType): string[] {
    if (context.kind === 'script') {
        return SCRIPT_CONTEXT_MEMBERS;
    }

    return context.kind === 'customComponent' ? CUSTOM_COMPONENT_CONTEXT_MEMBERS : CODE_WORKFLOW_CONTEXT_MEMBERS;
}

function contextMemberItems(context: ContextType): CompletionItemDraftType[] {
    return contextMembers(context).map((member) => {
        if (member === 'component') {
            return {insertAsSnippet: false, insertText: member, kind: 'module', label: member};
        }

        const insertText =
            context.kind === 'codeWorkflow' && ZERO_ARGUMENT_CONTEXT_MEMBERS.has(member)
                ? `${member}()$0`
                : buildFunctionSnippet(member);

        return {insertAsSnippet: true, insertText, kind: 'method', label: member};
    });
}

function findTask(request: ScriptCompletionRequestI): EnclosingTaskType | undefined {
    const {context, cursorOffset, source} = request;
    const task = findEnclosingTask(source, cursorOffset, context.language);

    if (task) {
        return task;
    }

    const functionName = findEnclosingFunctionName(source, cursorOffset, context.language);

    return functionName ? findTaskByPerformName(source, functionName, context.language) : undefined;
}

function declaredConnections(request: ScriptCompletionRequestI): ConnectionDeclarationType[] {
    if (request.context.kind === 'script') {
        return request.context.connections;
    }

    return request.context.kind === 'codeWorkflow' ? (findTask(request)?.connections ?? []) : [];
}

async function componentItems(request: ScriptCompletionRequestI): Promise<CompletionItemDraftType[]> {
    const {context} = request;

    if (context.kind === 'customComponent') {
        return [];
    }

    const connectedNames = new Set(declaredConnections(request).map((connection) => connection.componentName));
    const items: CompletionItemDraftType[] = context.components.components.map((component) => {
        const connected = connectedNames.has(component.name);

        return {
            ...(connected ? {detail: 'connected'} : {}),
            insertAsSnippet: false,
            insertText: component.name,
            kind: 'module',
            label: component.title ?? component.name,
            sortText: `${connected ? '0' : '1'}_${component.name}`,
        };
    });

    return [
        ...items.filter((item) => item.detail === 'connected'),
        ...items.filter((item) => item.detail !== 'connected'),
    ];
}

async function actionItems(
    request: ScriptCompletionRequestI,
    componentName: string
): Promise<CompletionItemDraftType[]> {
    const {context} = request;

    if (context.kind === 'customComponent') {
        return [];
    }

    const definition = await context.components.getDefinition(componentName);

    if (!definition) {
        return [];
    }

    const matchingConnectionNames = declaredConnections(request)
        .filter((connection) => connection.componentName === componentName)
        .map((connection) => connection.name);

    return (definition.actions ?? []).map((action) => {
        const acceptsClusterElements =
            context.kind === 'codeWorkflow' && Boolean(definition.actionClusterElementTypes?.[action.name]?.length);

        return {
            detail: action.title,
            documentation: action.description,
            insertAsSnippet: true,
            insertText: buildActionCallSnippet(
                action.name,
                context.language,
                matchingConnectionNames,
                acceptsClusterElements
            ),
            kind: 'method',
            label: action.name,
        };
    });
}

function inputItems(
    request: ScriptCompletionRequestI,
    segments: string[],
    scope: PerformScopeType
): CompletionItemDraftType[] {
    const {context, cursorOffset, source} = request;

    if (context.kind === 'script') {
        let value: unknown = context.input;

        for (const segment of segments.slice(0, -1)) {
            value = isPlainObject(value) ? value[segment] : undefined;
        }

        return isPlainObject(value) ? propertyItems(Object.keys(value)) : [];
    }

    if (context.kind === 'customComponent' && segments.length === 1 && scope.inputName) {
        const action = findEnclosingAction(source, cursorOffset, context.language);

        return propertyItems(action?.propertyNames ?? []);
    }

    return [];
}

function isPlainObject(value: unknown): value is Record<string, unknown> {
    return typeof value === 'object' && value !== null && !Array.isArray(value);
}

async function connectionPropertyItems(
    context: ScriptCompletionContextI,
    connectionName: string
): Promise<CompletionItemDraftType[]> {
    const declaration = context.connections.find((connection) => connection.name === connectionName);

    if (!declaration) {
        return [];
    }

    const connection = await context.components.getConnectionDefinition(
        declaration.componentName,
        declaration.componentVersion
    );
    const names = new Set<string>();

    for (const property of connection?.properties ?? []) {
        if (property.name) {
            names.add(property.name);
        }
    }

    for (const authorization of connection?.authorizations ?? []) {
        for (const property of authorization.properties ?? []) {
            if (property.name) {
                names.add(property.name);
            }
        }
    }

    return propertyItems([...names]);
}

async function connectionItems(
    request: ScriptCompletionRequestI,
    segments: string[]
): Promise<CompletionItemDraftType[]> {
    const {context, source} = request;

    if (segments.length === 1) {
        if (context.kind === 'script') {
            return propertyItems(context.connections.map((connection) => connection.name));
        }

        return context.kind === 'customComponent'
            ? propertyItems(findComponentConnectionPropertyNames(source, context.language))
            : [];
    }

    return segments.length === 2 && context.kind === 'script' ? connectionPropertyItems(context, segments[0]) : [];
}

function bareRootItems(scope: PerformScopeType): CompletionItemDraftType[] {
    return [scope.inputName, scope.connectionName, scope.contextName]
        .filter((name): name is string => Boolean(name))
        .map((name) => ({insertAsSnippet: false, insertText: name, kind: 'variable', label: name}));
}

type PropertyLoaderType<T> = (
    propertyName: string,
    lookupDependsOnPaths: string[],
    parameters: Record<string, unknown>
) => Promise<T[]>;

type ActionCallTargetType = {
    loadDisplayConditions: (parameters: Record<string, unknown>) => Promise<Record<string, boolean>>;
    loadDynamicProperties?: PropertyLoaderType<Property>;
    loadOptions?: PropertyLoaderType<Option>;
    properties: Property[];
};

async function resolveActionCall(
    request: ScriptCompletionRequestI,
    calleeText: string
): Promise<ActionCallTargetType | undefined> {
    const {context, cursorOffset, source} = request;

    if (context.kind === 'customComponent') {
        return undefined;
    }

    const calleePath = parseMemberPath(calleeText);

    if (!calleePath || calleePath.segments.length !== 3 || calleePath.segments[0] !== 'component') {
        return undefined;
    }

    const scope = findPerformScope(source, cursorOffset, context.language, calleePath.root);

    if (!scope || calleePath.root !== scope.contextName) {
        return undefined;
    }

    const [, componentName, actionName] = calleePath.segments;
    const declaredConnection = declaredConnections(request).find(
        (connection) => connection.componentName === componentName
    );
    const declaredVersion = declaredConnection?.componentVersion;

    const properties = await context.components.getOperationProperties(
        componentName,
        declaredVersion,
        actionName,
        'action'
    );
    const {loadDynamicProperties, loadOptions} = context;
    const workflowNodeName = context.kind === 'script' ? context.workflowNodeName : findTask(request)?.name;
    const clusterElementNode = context.kind === 'script' ? context.clusterElementNode : undefined;

    const toOperationRequest =
        workflowNodeName && declaredVersion !== undefined
            ? (
                  propertyName: string,
                  lookupDependsOnPaths: string[],
                  parameters: Record<string, unknown>
              ): OperationPropertyRequestType => ({
                  clusterElementNode,
                  componentName,
                  componentVersion: declaredVersion,
                  connectionKey: declaredConnection?.name,
                  lookupDependsOnPaths,
                  operationKind: 'action',
                  operationName: actionName,
                  parameters,
                  propertyName,
                  workflowNodeName,
              })
            : undefined;

    return {
        loadDisplayConditions: (parameters) =>
            context.components.getDisplayConditions(componentName, declaredVersion, actionName, 'action', parameters),
        loadDynamicProperties:
            loadDynamicProperties && toOperationRequest
                ? (...requestArguments) => loadDynamicProperties(toOperationRequest(...requestArguments))
                : undefined,
        loadOptions:
            loadOptions && toOperationRequest
                ? (...requestArguments) => loadOptions(toOperationRequest(...requestArguments))
                : undefined,
        properties,
    };
}

async function actionPropertyItems(
    request: ScriptCompletionRequestI,
    keySlot: ActionCallKeySlotType
): Promise<CompletionItemDraftType[] | undefined> {
    const target = await resolveActionCall(request, keySlot.calleeText);

    if (!target) {
        return undefined;
    }

    return getPropertyCompletionItems({
        ...target,
        existingKeys: keySlot.existingKeys,
        parameters: keySlot.rootValues,
        presentKeys: keySlot.rootKeys,
        propertyPath: keySlot.propertyPath,
    });
}

function escapeQuoted(value: string, quote: string): string {
    return value.replaceAll('\\', '\\\\').replaceAll(quote, `\\${quote}`);
}

function scriptValueText(value: unknown, language: ScriptLanguageType, quote?: string): string | undefined {
    if (quote) {
        return typeof value === 'string' ? escapeQuoted(value, quote) : undefined;
    }

    if (typeof value === 'string') {
        return `'${escapeQuoted(value, "'")}'`;
    }

    if (typeof value === 'boolean' && language === 'python') {
        return value ? 'True' : 'False';
    }

    return String(value);
}

async function actionValueItems(
    request: ScriptCompletionRequestI,
    valueSlot: ActionCallValueSlotType
): Promise<CompletionItemDraftType[] | undefined> {
    const target = await resolveActionCall(request, valueSlot.calleeText);

    if (!target) {
        return undefined;
    }

    return getPropertyValueItems({
        ...target,
        parameters: valueSlot.rootValues,
        presentKeys: valueSlot.rootKeys,
        propertyPath: valueSlot.propertyPath,
        replaceLength: valueSlot.prefix.length,
        toInsertText: (value) => scriptValueText(value, request.context.language, valueSlot.quote),
    });
}

export async function getScriptCompletions(request: ScriptCompletionRequestI): Promise<CompletionItemDraftType[]> {
    const {context, cursorOffset, source} = request;
    const keySlot = findActionCallKeySlot(source, cursorOffset, context.language);

    if (keySlot) {
        const propertyItems = await actionPropertyItems(request, keySlot);

        if (propertyItems) {
            return propertyItems;
        }
    }

    const valueSlot = findActionCallValueSlot(source, cursorOffset, context.language);

    if (valueSlot) {
        const valueItems = await actionValueItems(request, valueSlot);

        if (valueItems) {
            return valueItems;
        }
    }
    const path = parseMemberPath(source.slice(0, cursorOffset));

    if (!path) {
        return [];
    }

    const {callSegments, root, segments} = path;
    const scope = findPerformScope(source, cursorOffset, context.language, segments.length > 0 ? root : undefined);

    if (!scope) {
        return [];
    }

    if (segments.length === 0) {
        return context.language === 'javascript' ? [] : bareRootItems(scope);
    }

    if (root === scope.contextName) {
        if (segments.length === 1) {
            return contextMemberItems(context);
        }

        if (segments[0] === 'component') {
            if (segments.length === 2) {
                return componentItems(request);
            }

            return segments.length === 3 ? actionItems(request, segments[1]) : [];
        }

        if (segments.length === 2 && segments[0] === 'input' && callSegments[0] && context.kind === 'codeWorkflow') {
            const task = findTask(request);

            return propertyItems([...(task?.inputNames ?? []), ...(task?.earlierTaskNames ?? [])]);
        }

        return [];
    }

    if (root === scope.inputName) {
        return inputItems(request, segments, scope);
    }

    if (root === scope.connectionName) {
        return connectionItems(request, segments);
    }

    return [];
}
