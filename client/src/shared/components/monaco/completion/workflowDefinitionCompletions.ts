import {filterFunctionDefinitions} from '@/pages/platform/workflow-editor/components/properties/components/property-mentions-input/functionSuggestionUtils';
import getOutputSchemaFromWorkflowNodeOutput from '@/pages/platform/workflow-editor/utils/getOutputSchemaFromWorkflowNodeOutput';

import {getPropertyCompletionItems, getPropertyValueItems} from './propertyCompletions';
import {type CompletionItemDraftType, getScriptCompletions} from './scriptCompletions';
import {buildFunctionSnippet} from './snippets';
import {scanWorkflowDocument} from './workflowDocumentScanner';

import type {Option, Property} from '@/shared/middleware/platform/configuration';
import type {PropertyAllType} from '@/shared/types';

import type {
    OperationPropertyRequestType,
    ScriptCompletionContextI,
    ScriptLanguageType,
    WorkflowDefinitionCompletionContextI,
} from './completionTypes';
import type {WorkflowDocumentPositionType} from './workflowDocumentScanner';

export interface WorkflowDefinitionCompletionRequestI {
    context: WorkflowDefinitionCompletionContextI;
    cursorOffset: number;
    source: string;
}

const SCRIPT_TYPE_PREFIX = 'script/v1/';
const SCRIPT_LANGUAGES: Record<string, ScriptLanguageType> = {javascript: 'javascript', python: 'python'};
const TYPE_WITH_VERSION_PATTERN = /^(\w+)\/v(\d+)\//;
const NODE_REFERENCE_PATTERN = /\$\{([\w.-]*)$/;
const TRAILING_WORD_PATTERN = /[A-Za-z]\w*$/;
const OPERATION_TYPE_PATTERN = /^(\w+)\/v(\d+)\/(\w+)$/;

function isArrayElementType(path: string[]): boolean {
    return (path[0] === 'tasks' || path[0] === 'triggers') && /^\d+$/.test(path[path.length - 2] ?? '');
}

function last(path: string[]): string | undefined {
    return path[path.length - 1];
}

async function getInlineScriptCompletions(
    context: WorkflowDefinitionCompletionContextI,
    position: WorkflowDocumentPositionType,
    value: string
): Promise<CompletionItemDraftType[]> {
    const language = SCRIPT_LANGUAGES[(position.enclosingTaskType ?? '').slice(SCRIPT_TYPE_PREFIX.length)];

    if (!language) {
        return [];
    }

    const scriptContext: ScriptCompletionContextI = {
        components: context.components,
        connections: position.enclosingTaskConnections,
        input: Object.fromEntries(position.enclosingTaskInputKeys.map((key) => [key, null])),
        kind: 'script',
        language,
    };

    return getScriptCompletions({context: scriptContext, cursorOffset: value.length, source: value});
}

function getComponentTypeCompletions(context: WorkflowDefinitionCompletionContextI): CompletionItemDraftType[] {
    return context.components.components.map((component) => {
        const insertText = `${component.name}/v${component.version}/`;

        return {
            detail: component.title,
            insertAsSnippet: false,
            insertText,
            kind: 'module',
            label: insertText,
            retrigger: true,
        };
    });
}

async function getActionTypeCompletions(
    context: WorkflowDefinitionCompletionContextI,
    section: string,
    value: string
): Promise<CompletionItemDraftType[]> {
    const match = TYPE_WITH_VERSION_PATTERN.exec(value);

    if (!match) {
        return [];
    }

    const [, componentName, version] = match;
    const definition = await context.components.getDefinition(componentName, Number(version));
    const operations = (section === 'triggers' ? definition?.triggers : definition?.actions) ?? [];

    return operations.map((operation) => ({
        detail: operation.title,
        filterText: operation.name,
        insertAsSnippet: false,
        insertText: operation.name,
        kind: 'method',
        label: operation.name,
    }));
}

async function getOutputPropertyCompletions(
    context: WorkflowDefinitionCompletionContextI,
    segments: string[]
): Promise<CompletionItemDraftType[]> {
    const [nodeName, ...propertyPath] = segments.slice(0, -1);
    const outputs = await context.getNodeOutputs();
    const output = outputs.find((candidate) => candidate.workflowNodeName === nodeName);

    let schema: PropertyAllType | undefined = getOutputSchemaFromWorkflowNodeOutput(output);

    for (const segment of propertyPath) {
        schema = schema?.properties?.find((property) => property.name === segment);
    }

    return (schema?.properties ?? [])
        .filter((property) => property.name)
        .map((property) => ({
            detail: property.type,
            insertAsSnippet: false,
            insertText: property.name as string,
            kind: 'property',
            label: property.name as string,
        }));
}

function getFunctionCompletions(
    context: WorkflowDefinitionCompletionContextI,
    word: string | undefined
): CompletionItemDraftType[] {
    if (!word) {
        return [];
    }

    return filterFunctionDefinitions(context.functions, word).map((definition) => ({
        detail: definition.description,
        filterText: definition.name,
        insertAsSnippet: true,
        insertText: buildFunctionSnippet(definition.name),
        kind: 'function',
        label: `${definition.name}(${definition.parameters.map((parameter) => parameter.name).join(', ')})`,
    }));
}

async function getExpressionCompletions(
    context: WorkflowDefinitionCompletionContextI,
    position: WorkflowDocumentPositionType,
    value: string
): Promise<CompletionItemDraftType[]> {
    const items: CompletionItemDraftType[] = [];
    const reference = NODE_REFERENCE_PATTERN.exec(value);

    if (reference) {
        const segments = reference[1].split('.');

        if (segments.length > 1) {
            return getOutputPropertyCompletions(context, segments);
        }

        items.push(
            ...position.earlierNodeNames.map((name): CompletionItemDraftType => ({
                insertAsSnippet: false,
                insertText: name,
                kind: 'variable',
                label: name,
            }))
        );

        items.push(...getFunctionCompletions(context, segments[0] || undefined));

        return items;
    }

    if (value.startsWith('=')) {
        items.push(...getFunctionCompletions(context, TRAILING_WORD_PATTERN.exec(value)?.[0]));
    }

    return items;
}

type ParameterTargetType = {
    loadDisplayConditions: (parameters: Record<string, unknown>) => Promise<Record<string, boolean>>;
    loadDynamicProperties?: PropertyLoaderType<Property>;
    loadOptions?: PropertyLoaderType<Option>;
    parameters: Record<string, unknown>;
    presentKeys: string[];
    properties: Property[];
    propertyPath: string[];
};

type PropertyLoaderType<T> = (
    propertyName: string,
    lookupDependsOnPaths: string[],
    parameters: Record<string, unknown>
) => Promise<T[]>;

async function resolveParameterTarget(
    context: WorkflowDefinitionCompletionContextI,
    position: WorkflowDocumentPositionType,
    containerPath: string[]
): Promise<ParameterTargetType | undefined> {
    const section = containerPath[0];
    const parametersIndex = containerPath.lastIndexOf('parameters');

    if (
        (section !== 'tasks' && section !== 'triggers') ||
        parametersIndex < 2 ||
        !/^\d+$/.test(containerPath[parametersIndex - 1])
    ) {
        return undefined;
    }

    const nodePath = containerPath.slice(0, parametersIndex);
    const typeMatch = OPERATION_TYPE_PATTERN.exec(position.typeOf(nodePath) ?? '');

    if (!typeMatch) {
        return undefined;
    }

    const [, componentName, version, operationName] = typeMatch;
    const componentVersion = Number(version);
    const operationKind = section === 'triggers' && parametersIndex === 2 ? 'trigger' : 'action';
    const parametersPath = containerPath.slice(0, parametersIndex + 1);
    const workflowNodeName = position.nameOf(nodePath);
    const {loadDynamicProperties, loadOptions} = context;

    const properties = await context.components.getOperationProperties(
        componentName,
        componentVersion,
        operationName,
        operationKind
    );

    const toOperationRequest = workflowNodeName
        ? (
              propertyName: string,
              lookupDependsOnPaths: string[],
              parameters: Record<string, unknown>
          ): OperationPropertyRequestType => ({
              componentName,
              componentVersion,
              lookupDependsOnPaths,
              operationKind,
              operationName,
              parameters,
              propertyName,
              workflowNodeName,
          })
        : undefined;

    return {
        loadDisplayConditions: (parameters) =>
            context.components.getDisplayConditions(
                componentName,
                componentVersion,
                operationName,
                operationKind,
                parameters
            ),
        loadDynamicProperties:
            loadDynamicProperties && toOperationRequest
                ? (...requestArguments) => loadDynamicProperties(toOperationRequest(...requestArguments))
                : undefined,
        loadOptions:
            loadOptions && toOperationRequest
                ? (...requestArguments) => loadOptions(toOperationRequest(...requestArguments))
                : undefined,
        parameters: position.literalValuesOf(parametersPath),
        presentKeys: position.keysOf(parametersPath),
        properties,
        propertyPath: containerPath.slice(parametersIndex + 1),
    };
}

async function getParameterKeyCompletions(
    context: WorkflowDefinitionCompletionContextI,
    position: WorkflowDocumentPositionType
): Promise<CompletionItemDraftType[]> {
    const {containerPath, existingKeys} = position.keySlot!;
    const target = await resolveParameterTarget(context, position, containerPath);

    return target ? getPropertyCompletionItems({...target, existingKeys}) : [];
}

const YAML_PLAIN_STRING = /^[A-Za-z_/.@][\w ./@-]*$/;
const YAML_RESERVED_PLAIN = /^(true|false|null|~|yes|no|on|off|-?\d+(\.\d+)?)$/i;

function documentValueText(value: unknown, format: 'json' | 'yaml', quote?: string): string | undefined {
    if (format === 'json' || quote === '"') {
        return typeof value === 'string' ? JSON.stringify(value).slice(1, -1) : undefined;
    }

    if (quote === "'") {
        return typeof value === 'string' ? value.replaceAll("'", "''") : undefined;
    }

    if (typeof value === 'string') {
        return YAML_PLAIN_STRING.test(value) && !YAML_RESERVED_PLAIN.test(value) ? value : JSON.stringify(value);
    }

    return String(value);
}

async function getParameterValueCompletions(
    context: WorkflowDefinitionCompletionContextI,
    position: WorkflowDocumentPositionType,
    cursorOffset: number
): Promise<CompletionItemDraftType[]> {
    const {path, stringQuote, stringStart} = position;
    const key = last(path);

    if (key === undefined || stringStart === undefined) {
        return [];
    }

    const target = await resolveParameterTarget(context, position, path.slice(0, -1));

    if (!target) {
        return [];
    }

    return getPropertyValueItems({
        ...target,
        propertyPath: [...target.propertyPath, key],
        replaceLength: cursorOffset - stringStart,
        toInsertText: (value) => documentValueText(value, context.format, stringQuote),
    });
}

export async function getWorkflowDefinitionCompletions(
    request: WorkflowDefinitionCompletionRequestI
): Promise<CompletionItemDraftType[]> {
    const {context, cursorOffset, source} = request;
    const position = scanWorkflowDocument(source, cursorOffset, context.format);

    if (position?.keySlot) {
        return getParameterKeyCompletions(context, position);
    }

    if (!position?.inString) {
        return [];
    }

    const value = position.stringValueBeforeCursor ?? '';
    const {path} = position;
    const key = last(path);

    if (
        key === 'script' &&
        path[path.length - 2] === 'parameters' &&
        position.enclosingTaskType?.startsWith(SCRIPT_TYPE_PREFIX)
    ) {
        return getInlineScriptCompletions(context, position, value);
    }

    if (key === 'type' && isArrayElementType(path)) {
        return value.includes('/')
            ? getActionTypeCompletions(context, path[0], value)
            : getComponentTypeCompletions(context);
    }

    if (!value.includes('${') && !value.startsWith('=')) {
        return getParameterValueCompletions(context, position, cursorOffset);
    }

    return getExpressionCompletions(context, position, value);
}
