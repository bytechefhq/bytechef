/* tslint:disable */
/* eslint-disable */
/**
 * Embedded Execution Internal API
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
import type { ComponentDefinitionBasicModel } from './ComponentDefinitionBasicModel';
import {
    ComponentDefinitionBasicModelFromJSON,
    ComponentDefinitionBasicModelFromJSONTyped,
    ComponentDefinitionBasicModelToJSON,
} from './ComponentDefinitionBasicModel';
import type { ExecutionErrorModel } from './ExecutionErrorModel';
import {
    ExecutionErrorModelFromJSON,
    ExecutionErrorModelFromJSONTyped,
    ExecutionErrorModelToJSON,
} from './ExecutionErrorModel';
import type { WorkflowTriggerModel } from './WorkflowTriggerModel';
import {
    WorkflowTriggerModelFromJSON,
    WorkflowTriggerModelFromJSONTyped,
    WorkflowTriggerModelToJSON,
} from './WorkflowTriggerModel';

/**
 * Adds execution semantics to a trigger.
 * @export
 * @interface TriggerExecutionModel
 */
export interface TriggerExecutionModel {
    /**
     * 
     * @type {boolean}
     * @memberof TriggerExecutionModel
     */
    batch?: boolean;
    /**
     * 
     * @type {ComponentDefinitionBasicModel}
     * @memberof TriggerExecutionModel
     */
    component?: ComponentDefinitionBasicModel;
    /**
     * The created by.
     * @type {string}
     * @memberof TriggerExecutionModel
     */
    readonly createdBy?: string;
    /**
     * The created date.
     * @type {Date}
     * @memberof TriggerExecutionModel
     */
    readonly createdDate?: Date;
    /**
     * The time when a task instance ended (CANCELLED, FAILED, COMPLETED).
     * @type {Date}
     * @memberof TriggerExecutionModel
     */
    readonly endDate?: Date;
    /**
     * 
     * @type {ExecutionErrorModel}
     * @memberof TriggerExecutionModel
     */
    error?: ExecutionErrorModel;
    /**
     * The total time in ms for a task to execute (excluding wait time of the task in transit). i.e. actual execution time on a worker node.
     * @type {number}
     * @memberof TriggerExecutionModel
     */
    readonly executionTime?: number;
    /**
     * The id of a task execution.
     * @type {string}
     * @memberof TriggerExecutionModel
     */
    readonly id?: string;
    /**
     * The input parameters for a task.
     * @type {{ [key: string]: any; }}
     * @memberof TriggerExecutionModel
     */
    readonly input?: { [key: string]: any; };
    /**
     * The last modified by.
     * @type {string}
     * @memberof TriggerExecutionModel
     */
    readonly lastModifiedBy?: string;
    /**
     * The last modified date.
     * @type {Date}
     * @memberof TriggerExecutionModel
     */
    readonly lastModifiedDate?: Date;
    /**
     * The maximum number of times that a task may retry.
     * @type {number}
     * @memberof TriggerExecutionModel
     */
    readonly maxRetries?: number;
    /**
     * The result output generated by the trigger handler which executed a trigger.
     * @type {object}
     * @memberof TriggerExecutionModel
     */
    readonly output?: object;
    /**
     * The priority value.
     * @type {number}
     * @memberof TriggerExecutionModel
     */
    readonly priority: number;
    /**
     * The number of times that a task has been retried.
     * @type {number}
     * @memberof TriggerExecutionModel
     */
    readonly retryAttempts?: number;
    /**
     * The delay to introduce between each retry. Values are to be specified using the ISO-8601 format (excluding the PT prefix). e.g. 10s (ten seconds), 1m (one minute) etc.
     * @type {string}
     * @memberof TriggerExecutionModel
     */
    readonly retryDelay?: string;
    /**
     * The factor to use in order to calculate the actual delay time between each successive retry -- multiplying by the value of the retryDelay.
     * @type {number}
     * @memberof TriggerExecutionModel
     */
    readonly retryDelayFactor?: number;
    /**
     * The calculated retry delay. i.e. delay * retryAttempts * retryDelayFactor.
     * @type {number}
     * @memberof TriggerExecutionModel
     */
    readonly retryDelayMillis?: number;
    /**
     * The time when a task instance was started.
     * @type {Date}
     * @memberof TriggerExecutionModel
     */
    readonly startDate: Date;
    /**
     * The current status of a task.
     * @type {string}
     * @memberof TriggerExecutionModel
     */
    readonly status: TriggerExecutionModelStatusEnum;
    /**
     * 
     * @type {WorkflowTriggerModel}
     * @memberof TriggerExecutionModel
     */
    workflowTrigger?: WorkflowTriggerModel;
    /**
     * The type of the task.
     * @type {string}
     * @memberof TriggerExecutionModel
     */
    readonly type?: string;
}


/**
 * @export
 */
export const TriggerExecutionModelStatusEnum = {
    Created: 'CREATED',
    Started: 'STARTED',
    Failed: 'FAILED',
    Cancelled: 'CANCELLED',
    Completed: 'COMPLETED'
} as const;
export type TriggerExecutionModelStatusEnum = typeof TriggerExecutionModelStatusEnum[keyof typeof TriggerExecutionModelStatusEnum];


/**
 * Check if a given object implements the TriggerExecutionModel interface.
 */
export function instanceOfTriggerExecutionModel(value: object): boolean {
    if (!('priority' in value)) return false;
    if (!('startDate' in value)) return false;
    if (!('status' in value)) return false;
    return true;
}

export function TriggerExecutionModelFromJSON(json: any): TriggerExecutionModel {
    return TriggerExecutionModelFromJSONTyped(json, false);
}

export function TriggerExecutionModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): TriggerExecutionModel {
    if (json == null) {
        return json;
    }
    return {
        
        'batch': json['batch'] == null ? undefined : json['batch'],
        'component': json['component'] == null ? undefined : ComponentDefinitionBasicModelFromJSON(json['component']),
        'createdBy': json['createdBy'] == null ? undefined : json['createdBy'],
        'createdDate': json['createdDate'] == null ? undefined : (new Date(json['createdDate'])),
        'endDate': json['endDate'] == null ? undefined : (new Date(json['endDate'])),
        'error': json['error'] == null ? undefined : ExecutionErrorModelFromJSON(json['error']),
        'executionTime': json['executionTime'] == null ? undefined : json['executionTime'],
        'id': json['id'] == null ? undefined : json['id'],
        'input': json['input'] == null ? undefined : json['input'],
        'lastModifiedBy': json['lastModifiedBy'] == null ? undefined : json['lastModifiedBy'],
        'lastModifiedDate': json['lastModifiedDate'] == null ? undefined : (new Date(json['lastModifiedDate'])),
        'maxRetries': json['maxRetries'] == null ? undefined : json['maxRetries'],
        'output': json['output'] == null ? undefined : json['output'],
        'priority': json['priority'],
        'retryAttempts': json['retryAttempts'] == null ? undefined : json['retryAttempts'],
        'retryDelay': json['retryDelay'] == null ? undefined : json['retryDelay'],
        'retryDelayFactor': json['retryDelayFactor'] == null ? undefined : json['retryDelayFactor'],
        'retryDelayMillis': json['retryDelayMillis'] == null ? undefined : json['retryDelayMillis'],
        'startDate': (new Date(json['startDate'])),
        'status': json['status'],
        'workflowTrigger': json['workflowTrigger'] == null ? undefined : WorkflowTriggerModelFromJSON(json['workflowTrigger']),
        'type': json['type'] == null ? undefined : json['type'],
    };
}

export function TriggerExecutionModelToJSON(value?: Omit<TriggerExecutionModel, 'createdBy'|'createdDate'|'endDate'|'executionTime'|'id'|'input'|'lastModifiedBy'|'lastModifiedDate'|'maxRetries'|'output'|'priority'|'retryAttempts'|'retryDelay'|'retryDelayFactor'|'retryDelayMillis'|'startDate'|'status'|'type'> | null): any {
    if (value == null) {
        return value;
    }
    return {
        
        'batch': value['batch'],
        'component': ComponentDefinitionBasicModelToJSON(value['component']),
        'error': ExecutionErrorModelToJSON(value['error']),
        'workflowTrigger': WorkflowTriggerModelToJSON(value['workflowTrigger']),
    };
}

