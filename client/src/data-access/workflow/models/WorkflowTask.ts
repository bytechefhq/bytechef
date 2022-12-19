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
/**
 * Represents a definition of the task.
 * @export
 * @interface WorkflowTask
 */
export interface WorkflowTask {
    /**
     * The (optional) list of tasks that are to be executed after execution of this task -- regardless of whether it had failed or not.
     * @type {Array<WorkflowTask>}
     * @memberof WorkflowTask
     */
    finalize?: Array<WorkflowTask>;
    /**
     * The human-readable description of the task.
     * @type {string}
     * @memberof WorkflowTask
     */
    label?: string;
    /**
     * The identifier name of the task. Task names are used for assigning the output of one task so it can be later used by subsequent tasks.
     * @type {string}
     * @memberof WorkflowTask
     */
    name: string;
    /**
     * Defines the name of the type of the node that the task execution will be routed to. For instance, if the node value is "encoder", then the task will be routed to the "encoder" queue which is presumably subscribed to by worker nodes of "encoder" type.
     * @type {string}
     * @memberof WorkflowTask
     */
    node?: string;
    /**
     * Key-value map of task parameters.
     * @type {{ [key: string]: object; }}
     * @memberof WorkflowTask
     */
    parameters?: { [key: string]: object; };
    /**
     * The (optional) list of tasks that are to be executed after the succesful execution of this task.
     * @type {Array<WorkflowTask>}
     * @memberof WorkflowTask
     */
    post?: Array<WorkflowTask>;
    /**
     * The (optional) list of tasks that are to be executed prior to this task.
     * @type {Array<WorkflowTask>}
     * @memberof WorkflowTask
     */
    pre?: Array<WorkflowTask>;
    /**
     * The timeout expression which describes when this task should be deemed as timed-out.
     * @type {string}
     * @memberof WorkflowTask
     */
    timeout?: string;
    /**
     * Type of the task.
     * @type {string}
     * @memberof WorkflowTask
     */
    type?: string;
}

/**
 * Check if a given object implements the WorkflowTask interface.
 */
export function instanceOfWorkflowTask(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "name" in value;

    return isInstance;
}

export function WorkflowTaskFromJSON(json: any): WorkflowTask {
    return WorkflowTaskFromJSONTyped(json, false);
}

export function WorkflowTaskFromJSONTyped(json: any, ignoreDiscriminator: boolean): WorkflowTask {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'finalize': !exists(json, 'finalize') ? undefined : ((json['finalize'] as Array<any>).map(WorkflowTaskFromJSON)),
        'label': !exists(json, 'label') ? undefined : json['label'],
        'name': json['name'],
        'node': !exists(json, 'node') ? undefined : json['node'],
        'parameters': !exists(json, 'parameters') ? undefined : json['parameters'],
        'post': !exists(json, 'post') ? undefined : ((json['post'] as Array<any>).map(WorkflowTaskFromJSON)),
        'pre': !exists(json, 'pre') ? undefined : ((json['pre'] as Array<any>).map(WorkflowTaskFromJSON)),
        'timeout': !exists(json, 'timeout') ? undefined : json['timeout'],
        'type': !exists(json, 'type') ? undefined : json['type'],
    };
}

export function WorkflowTaskToJSON(value?: WorkflowTask | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'finalize': value.finalize === undefined ? undefined : ((value.finalize as Array<any>).map(WorkflowTaskToJSON)),
        'label': value.label,
        'name': value.name,
        'node': value.node,
        'parameters': value.parameters,
        'post': value.post === undefined ? undefined : ((value.post as Array<any>).map(WorkflowTaskToJSON)),
        'pre': value.pre === undefined ? undefined : ((value.pre as Array<any>).map(WorkflowTaskToJSON)),
        'timeout': value.timeout,
        'type': value.type,
    };
}

