/* tslint:disable */
/* eslint-disable */
/**
 * The Embedded Configuration Internal API
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: 1
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

import { mapValues } from '../runtime';
import type { ComponentConnection } from './ComponentConnection';
import {
    ComponentConnectionFromJSON,
    ComponentConnectionFromJSONTyped,
    ComponentConnectionToJSON,
    ComponentConnectionToJSONTyped,
} from './ComponentConnection';

/**
 * Represents a definition of a workflow task.
 * @export
 * @interface WorkflowTask
 */
export interface WorkflowTask {
    /**
     * 
     * @type {{ [key: string]: any; }}
     * @memberof WorkflowTask
     */
    readonly clusterElements?: { [key: string]: any; };
    /**
     * 
     * @type {Array<ComponentConnection>}
     * @memberof WorkflowTask
     */
    readonly connections?: Array<ComponentConnection>;
    /**
     * The description of the task.
     * @type {string}
     * @memberof WorkflowTask
     */
    description?: string;
    /**
     * The (optional) list of tasks that are to be executed after execution of a task -- regardless of whether it had failed or not.
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
     * Key-value map of metadata.
     * @type {{ [key: string]: any; }}
     * @memberof WorkflowTask
     */
    metadata?: { [key: string]: any; };
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
     * @type {{ [key: string]: any; }}
     * @memberof WorkflowTask
     */
    parameters?: { [key: string]: any; };
    /**
     * The (optional) list of tasks that are to be executed after the successful execution of a task.
     * @type {Array<WorkflowTask>}
     * @memberof WorkflowTask
     */
    post?: Array<WorkflowTask>;
    /**
     * The (optional) list of tasks that are to be executed prior to a task.
     * @type {Array<WorkflowTask>}
     * @memberof WorkflowTask
     */
    pre?: Array<WorkflowTask>;
    /**
     * The timeout expression which describes when a task should be deemed as timed-out.
     * @type {string}
     * @memberof WorkflowTask
     */
    timeout?: string;
    /**
     * The type of the task.
     * @type {string}
     * @memberof WorkflowTask
     */
    type: string;
}

/**
 * Check if a given object implements the WorkflowTask interface.
 */
export function instanceOfWorkflowTask(value: object): value is WorkflowTask {
    if (!('name' in value) || value['name'] === undefined) return false;
    if (!('type' in value) || value['type'] === undefined) return false;
    return true;
}

export function WorkflowTaskFromJSON(json: any): WorkflowTask {
    return WorkflowTaskFromJSONTyped(json, false);
}

export function WorkflowTaskFromJSONTyped(json: any, ignoreDiscriminator: boolean): WorkflowTask {
    if (json == null) {
        return json;
    }
    return {
        
        'clusterElements': json['clusterElements'] == null ? undefined : json['clusterElements'],
        'connections': json['connections'] == null ? undefined : ((json['connections'] as Array<any>).map(ComponentConnectionFromJSON)),
        'description': json['description'] == null ? undefined : json['description'],
        'finalize': json['finalize'] == null ? undefined : ((json['finalize'] as Array<any>).map(WorkflowTaskFromJSON)),
        'label': json['label'] == null ? undefined : json['label'],
        'metadata': json['metadata'] == null ? undefined : json['metadata'],
        'name': json['name'],
        'node': json['node'] == null ? undefined : json['node'],
        'parameters': json['parameters'] == null ? undefined : json['parameters'],
        'post': json['post'] == null ? undefined : ((json['post'] as Array<any>).map(WorkflowTaskFromJSON)),
        'pre': json['pre'] == null ? undefined : ((json['pre'] as Array<any>).map(WorkflowTaskFromJSON)),
        'timeout': json['timeout'] == null ? undefined : json['timeout'],
        'type': json['type'],
    };
}

export function WorkflowTaskToJSON(json: any): WorkflowTask {
    return WorkflowTaskToJSONTyped(json, false);
}

export function WorkflowTaskToJSONTyped(value?: Omit<WorkflowTask, 'clusterElements'|'connections'> | null, ignoreDiscriminator: boolean = false): any {
    if (value == null) {
        return value;
    }

    return {
        
        'description': value['description'],
        'finalize': value['finalize'] == null ? undefined : ((value['finalize'] as Array<any>).map(WorkflowTaskToJSON)),
        'label': value['label'],
        'metadata': value['metadata'],
        'name': value['name'],
        'node': value['node'],
        'parameters': value['parameters'],
        'post': value['post'] == null ? undefined : ((value['post'] as Array<any>).map(WorkflowTaskToJSON)),
        'pre': value['pre'] == null ? undefined : ((value['pre'] as Array<any>).map(WorkflowTaskToJSON)),
        'timeout': value['timeout'],
        'type': value['type'],
    };
}

