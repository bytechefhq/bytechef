/* tslint:disable */
/* eslint-disable */
/**
 * The Automation Execution Internal API
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
import type { ExecutionErrorModel } from './ExecutionErrorModel';
import {
    ExecutionErrorModelFromJSON,
    ExecutionErrorModelFromJSONTyped,
    ExecutionErrorModelToJSON,
} from './ExecutionErrorModel';
import type { TaskExecutionModel } from './TaskExecutionModel';
import {
    TaskExecutionModelFromJSON,
    TaskExecutionModelFromJSONTyped,
    TaskExecutionModelToJSON,
} from './TaskExecutionModel';
import type { WebhookModel } from './WebhookModel';
import {
    WebhookModelFromJSON,
    WebhookModelFromJSONTyped,
    WebhookModelToJSON,
} from './WebhookModel';

/**
 * Represents an execution of a workflow.
 * @export
 * @interface JobModel
 */
export interface JobModel {
    /**
     * The created by.
     * @type {string}
     * @memberof JobModel
     */
    readonly createdBy?: string;
    /**
     * The created date.
     * @type {Date}
     * @memberof JobModel
     */
    readonly createdDate?: Date;
    /**
     * The index of the step on the job's workflow on which the job is working on right now.
     * @type {number}
     * @memberof JobModel
     */
    readonly currentTask?: number;
    /**
     * The time execution entered end status COMPLETED, STOPPED, FAILED
     * @type {Date}
     * @memberof JobModel
     */
    endDate?: Date;
    /**
     * 
     * @type {ExecutionErrorModel}
     * @memberof JobModel
     */
    error?: ExecutionErrorModel;
    /**
     * The id of a job.
     * @type {string}
     * @memberof JobModel
     */
    readonly id?: string;
    /**
     * The key-value map of the inputs passed to the job when it was created.
     * @type {{ [key: string]: any; }}
     * @memberof JobModel
     */
    readonly inputs?: { [key: string]: any; };
    /**
     * The job's human-readable name.
     * @type {string}
     * @memberof JobModel
     */
    readonly label?: string;
    /**
     * The last modified by.
     * @type {string}
     * @memberof JobModel
     */
    readonly lastModifiedBy?: string;
    /**
     * The last modified date.
     * @type {Date}
     * @memberof JobModel
     */
    readonly lastModifiedDate?: Date;
    /**
     * The key-value map of the outputs returned.
     * @type {{ [key: string]: any; }}
     * @memberof JobModel
     */
    readonly outputs?: { [key: string]: any; };
    /**
     * The id of the parent task that created this job. Required for sub-flows.
     * @type {number}
     * @memberof JobModel
     */
    readonly parentTaskExecutionId?: number;
    /**
     * The priority value.
     * @type {number}
     * @memberof JobModel
     */
    readonly priority: number;
    /**
     * The time of when the job began.
     * @type {Date}
     * @memberof JobModel
     */
    readonly startDate: Date;
    /**
     * The job's status.
     * @type {string}
     * @memberof JobModel
     */
    readonly status: JobModelStatusEnum;
    /**
     * 
     * @type {Array<TaskExecutionModel>}
     * @memberof JobModel
     */
    taskExecutions?: Array<TaskExecutionModel>;
    /**
     * The list of the webhooks configured.
     * @type {Array<WebhookModel>}
     * @memberof JobModel
     */
    readonly webhooks?: Array<WebhookModel>;
    /**
     * 
     * @type {string}
     * @memberof JobModel
     */
    readonly workflowId?: string;
}


/**
 * @export
 */
export const JobModelStatusEnum = {
    Created: 'CREATED',
    Started: 'STARTED',
    Stopped: 'STOPPED',
    Failed: 'FAILED',
    Completed: 'COMPLETED'
} as const;
export type JobModelStatusEnum = typeof JobModelStatusEnum[keyof typeof JobModelStatusEnum];


/**
 * Check if a given object implements the JobModel interface.
 */
export function instanceOfJobModel(value: object): boolean {
    if (!('priority' in value)) return false;
    if (!('startDate' in value)) return false;
    if (!('status' in value)) return false;
    return true;
}

export function JobModelFromJSON(json: any): JobModel {
    return JobModelFromJSONTyped(json, false);
}

export function JobModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): JobModel {
    if (json == null) {
        return json;
    }
    return {
        
        'createdBy': json['createdBy'] == null ? undefined : json['createdBy'],
        'createdDate': json['createdDate'] == null ? undefined : (new Date(json['createdDate'])),
        'currentTask': json['currentTask'] == null ? undefined : json['currentTask'],
        'endDate': json['endDate'] == null ? undefined : (new Date(json['endDate'])),
        'error': json['error'] == null ? undefined : ExecutionErrorModelFromJSON(json['error']),
        'id': json['id'] == null ? undefined : json['id'],
        'inputs': json['inputs'] == null ? undefined : json['inputs'],
        'label': json['label'] == null ? undefined : json['label'],
        'lastModifiedBy': json['lastModifiedBy'] == null ? undefined : json['lastModifiedBy'],
        'lastModifiedDate': json['lastModifiedDate'] == null ? undefined : (new Date(json['lastModifiedDate'])),
        'outputs': json['outputs'] == null ? undefined : json['outputs'],
        'parentTaskExecutionId': json['parentTaskExecutionId'] == null ? undefined : json['parentTaskExecutionId'],
        'priority': json['priority'],
        'startDate': (new Date(json['startDate'])),
        'status': json['status'],
        'taskExecutions': json['taskExecutions'] == null ? undefined : ((json['taskExecutions'] as Array<any>).map(TaskExecutionModelFromJSON)),
        'webhooks': json['webhooks'] == null ? undefined : ((json['webhooks'] as Array<any>).map(WebhookModelFromJSON)),
        'workflowId': json['workflowId'] == null ? undefined : json['workflowId'],
    };
}

export function JobModelToJSON(value?: Omit<JobModel, 'createdBy'|'createdDate'|'currentTask'|'id'|'inputs'|'label'|'lastModifiedBy'|'lastModifiedDate'|'outputs'|'parentTaskExecutionId'|'priority'|'startDate'|'status'|'webhooks'|'workflowId'> | null): any {
    if (value == null) {
        return value;
    }
    return {
        
        'endDate': value['endDate'] == null ? undefined : ((value['endDate']).toISOString()),
        'error': ExecutionErrorModelToJSON(value['error']),
        'taskExecutions': value['taskExecutions'] == null ? undefined : ((value['taskExecutions'] as Array<any>).map(TaskExecutionModelToJSON)),
    };
}

