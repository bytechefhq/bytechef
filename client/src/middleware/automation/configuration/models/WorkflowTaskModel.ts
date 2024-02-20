/* tslint:disable */
/* eslint-disable */
/**
 * The Automation Configuration API
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: 1
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

import { exists, mapValues } from '../runtime';
import type { DataStreamComponentModel } from './DataStreamComponentModel';
import {
    DataStreamComponentModelFromJSON,
    DataStreamComponentModelFromJSONTyped,
    DataStreamComponentModelToJSON,
} from './DataStreamComponentModel';
import type { WorkflowConnectionModel } from './WorkflowConnectionModel';
import {
    WorkflowConnectionModelFromJSON,
    WorkflowConnectionModelFromJSONTyped,
    WorkflowConnectionModelToJSON,
} from './WorkflowConnectionModel';

/**
 * Represents a definition of a workflow task.
 * @export
 * @interface WorkflowTaskModel
 */
export interface WorkflowTaskModel {
    /**
     * 
     * @type {Array<WorkflowConnectionModel>}
     * @memberof WorkflowTaskModel
     */
    readonly connections?: Array<WorkflowConnectionModel>;
    /**
     * 
     * @type {DataStreamComponentModel}
     * @memberof WorkflowTaskModel
     */
    destination?: DataStreamComponentModel;
    /**
     * The (optional) list of tasks that are to be executed after execution of a task -- regardless of whether it had failed or not.
     * @type {Array<WorkflowTaskModel>}
     * @memberof WorkflowTaskModel
     */
    finalize?: Array<WorkflowTaskModel>;
    /**
     * The human-readable description of the task.
     * @type {string}
     * @memberof WorkflowTaskModel
     */
    label?: string;
    /**
     * The identifier name of the task. Task names are used for assigning the output of one task so it can be later used by subsequent tasks.
     * @type {string}
     * @memberof WorkflowTaskModel
     */
    name: string;
    /**
     * Defines the name of the type of the node that the task execution will be routed to. For instance, if the node value is "encoder", then the task will be routed to the "encoder" queue which is presumably subscribed to by worker nodes of "encoder" type.
     * @type {string}
     * @memberof WorkflowTaskModel
     */
    node?: string;
    /**
     * Key-value map of task parameters.
     * @type {{ [key: string]: object; }}
     * @memberof WorkflowTaskModel
     */
    parameters?: { [key: string]: object; };
    /**
     * The (optional) list of tasks that are to be executed after the successful execution of a task.
     * @type {Array<WorkflowTaskModel>}
     * @memberof WorkflowTaskModel
     */
    post?: Array<WorkflowTaskModel>;
    /**
     * The (optional) list of tasks that are to be executed prior to a task.
     * @type {Array<WorkflowTaskModel>}
     * @memberof WorkflowTaskModel
     */
    pre?: Array<WorkflowTaskModel>;
    /**
     * 
     * @type {DataStreamComponentModel}
     * @memberof WorkflowTaskModel
     */
    source?: DataStreamComponentModel;
    /**
     * The timeout expression which describes when a task should be deemed as timed-out.
     * @type {string}
     * @memberof WorkflowTaskModel
     */
    timeout?: string;
    /**
     * The type of a task.
     * @type {string}
     * @memberof WorkflowTaskModel
     */
    type: string;
}

/**
 * Check if a given object implements the WorkflowTaskModel interface.
 */
export function instanceOfWorkflowTaskModel(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "name" in value;
    isInstance = isInstance && "type" in value;

    return isInstance;
}

export function WorkflowTaskModelFromJSON(json: any): WorkflowTaskModel {
    return WorkflowTaskModelFromJSONTyped(json, false);
}

export function WorkflowTaskModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): WorkflowTaskModel {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'connections': !exists(json, 'connections') ? undefined : ((json['connections'] as Array<any>).map(WorkflowConnectionModelFromJSON)),
        'destination': !exists(json, 'destination') ? undefined : DataStreamComponentModelFromJSON(json['destination']),
        'finalize': !exists(json, 'finalize') ? undefined : ((json['finalize'] as Array<any>).map(WorkflowTaskModelFromJSON)),
        'label': !exists(json, 'label') ? undefined : json['label'],
        'name': json['name'],
        'node': !exists(json, 'node') ? undefined : json['node'],
        'parameters': !exists(json, 'parameters') ? undefined : json['parameters'],
        'post': !exists(json, 'post') ? undefined : ((json['post'] as Array<any>).map(WorkflowTaskModelFromJSON)),
        'pre': !exists(json, 'pre') ? undefined : ((json['pre'] as Array<any>).map(WorkflowTaskModelFromJSON)),
        'source': !exists(json, 'source') ? undefined : DataStreamComponentModelFromJSON(json['source']),
        'timeout': !exists(json, 'timeout') ? undefined : json['timeout'],
        'type': json['type'],
    };
}

export function WorkflowTaskModelToJSON(value?: WorkflowTaskModel | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'destination': DataStreamComponentModelToJSON(value.destination),
        'finalize': value.finalize === undefined ? undefined : ((value.finalize as Array<any>).map(WorkflowTaskModelToJSON)),
        'label': value.label,
        'name': value.name,
        'node': value.node,
        'parameters': value.parameters,
        'post': value.post === undefined ? undefined : ((value.post as Array<any>).map(WorkflowTaskModelToJSON)),
        'pre': value.pre === undefined ? undefined : ((value.pre as Array<any>).map(WorkflowTaskModelToJSON)),
        'source': DataStreamComponentModelToJSON(value.source),
        'timeout': value.timeout,
        'type': value.type,
    };
}

