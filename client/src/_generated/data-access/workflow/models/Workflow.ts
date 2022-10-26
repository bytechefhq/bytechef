/* tslint:disable */
/* eslint-disable */
/**
 * OpenAPI definition
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: v1
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

import { exists, mapValues } from '../runtime';
import type { ExecutionError } from './ExecutionError';
import {
    ExecutionErrorFromJSON,
    ExecutionErrorFromJSONTyped,
    ExecutionErrorToJSON,
} from './ExecutionError';
import type { WorkflowTask } from './WorkflowTask';
import {
    WorkflowTaskFromJSON,
    WorkflowTaskFromJSONTyped,
    WorkflowTaskToJSON,
} from './WorkflowTask';

/**
 * 
 * @export
 * @interface Workflow
 */
export interface Workflow {
    /**
     * 
     * @type {string}
     * @memberof Workflow
     */
    content?: string;
    /**
     * 
     * @type {string}
     * @memberof Workflow
     */
    readonly createdBy?: string;
    /**
     * 
     * @type {Date}
     * @memberof Workflow
     */
    readonly createdDate?: Date;
    /**
     * 
     * @type {ExecutionError}
     * @memberof Workflow
     */
    error?: ExecutionError;
    /**
     * 
     * @type {string}
     * @memberof Workflow
     */
    format?: WorkflowFormatEnum;
    /**
     * 
     * @type {string}
     * @memberof Workflow
     */
    readonly id?: string;
    /**
     * 
     * @type {Array<{ [key: string]: object; }>}
     * @memberof Workflow
     */
    inputs?: Array<{ [key: string]: object; }>;
    /**
     * 
     * @type {string}
     * @memberof Workflow
     */
    label?: string;
    /**
     * 
     * @type {string}
     * @memberof Workflow
     */
    readonly lastModifiedBy?: string;
    /**
     * 
     * @type {Date}
     * @memberof Workflow
     */
    readonly lastModifiedDate?: Date;
    /**
     * 
     * @type {Array<{ [key: string]: object; }>}
     * @memberof Workflow
     */
    outputs?: Array<{ [key: string]: object; }>;
    /**
     * 
     * @type {Array<WorkflowTask>}
     * @memberof Workflow
     */
    tasks?: Array<WorkflowTask>;
    /**
     * 
     * @type {number}
     * @memberof Workflow
     */
    retry?: number;
}


/**
 * @export
 */
export const WorkflowFormatEnum = {
    Json: 'JSON',
    Yml: 'YML',
    Yaml: 'YAML'
} as const;
export type WorkflowFormatEnum = typeof WorkflowFormatEnum[keyof typeof WorkflowFormatEnum];


/**
 * Check if a given object implements the Workflow interface.
 */
export function instanceOfWorkflow(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function WorkflowFromJSON(json: any): Workflow {
    return WorkflowFromJSONTyped(json, false);
}

export function WorkflowFromJSONTyped(json: any, ignoreDiscriminator: boolean): Workflow {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'content': !exists(json, 'content') ? undefined : json['content'],
        'createdBy': !exists(json, 'createdBy') ? undefined : json['createdBy'],
        'createdDate': !exists(json, 'createdDate') ? undefined : (new Date(json['createdDate'])),
        'error': !exists(json, 'error') ? undefined : ExecutionErrorFromJSON(json['error']),
        'format': !exists(json, 'format') ? undefined : json['format'],
        'id': !exists(json, 'id') ? undefined : json['id'],
        'inputs': !exists(json, 'inputs') ? undefined : json['inputs'],
        'label': !exists(json, 'label') ? undefined : json['label'],
        'lastModifiedBy': !exists(json, 'lastModifiedBy') ? undefined : json['lastModifiedBy'],
        'lastModifiedDate': !exists(json, 'lastModifiedDate') ? undefined : (new Date(json['lastModifiedDate'])),
        'outputs': !exists(json, 'outputs') ? undefined : json['outputs'],
        'tasks': !exists(json, 'tasks') ? undefined : ((json['tasks'] as Array<any>).map(WorkflowTaskFromJSON)),
        'retry': !exists(json, 'retry') ? undefined : json['retry'],
    };
}

export function WorkflowToJSON(value?: Workflow | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'content': value.content,
        'error': ExecutionErrorToJSON(value.error),
        'format': value.format,
        'inputs': value.inputs,
        'label': value.label,
        'outputs': value.outputs,
        'tasks': value.tasks === undefined ? undefined : ((value.tasks as Array<any>).map(WorkflowTaskToJSON)),
        'retry': value.retry,
    };
}

