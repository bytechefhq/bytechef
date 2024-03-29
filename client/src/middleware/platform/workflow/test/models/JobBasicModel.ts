/* tslint:disable */
/* eslint-disable */
/**
 * The Platform Workflow Test API
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
/**
 * Represents an execution of a workflow.
 * @export
 * @interface JobBasicModel
 */
export interface JobBasicModel {
    /**
     * The created by.
     * @type {string}
     * @memberof JobBasicModel
     */
    readonly createdBy?: string;
    /**
     * The created date.
     * @type {Date}
     * @memberof JobBasicModel
     */
    readonly createdDate?: Date;
    /**
     * The time execution entered end status COMPLETED, STOPPED, FAILED
     * @type {Date}
     * @memberof JobBasicModel
     */
    endDate?: Date;
    /**
     * The id of a job.
     * @type {string}
     * @memberof JobBasicModel
     */
    readonly id?: string;
    /**
     * The job's human-readable name.
     * @type {string}
     * @memberof JobBasicModel
     */
    readonly label?: string;
    /**
     * The last modified by.
     * @type {string}
     * @memberof JobBasicModel
     */
    readonly lastModifiedBy?: string;
    /**
     * The last modified date.
     * @type {Date}
     * @memberof JobBasicModel
     */
    readonly lastModifiedDate?: Date;
    /**
     * The priority value.
     * @type {number}
     * @memberof JobBasicModel
     */
    readonly priority: number;
    /**
     * The time of when the job began.
     * @type {Date}
     * @memberof JobBasicModel
     */
    readonly startDate: Date;
    /**
     * The job's status.
     * @type {string}
     * @memberof JobBasicModel
     */
    readonly status: JobBasicModelStatusEnum;
    /**
     * 
     * @type {string}
     * @memberof JobBasicModel
     */
    readonly workflowId?: string;
}


/**
 * @export
 */
export const JobBasicModelStatusEnum = {
    Created: 'CREATED',
    Started: 'STARTED',
    Stopped: 'STOPPED',
    Failed: 'FAILED',
    Completed: 'COMPLETED'
} as const;
export type JobBasicModelStatusEnum = typeof JobBasicModelStatusEnum[keyof typeof JobBasicModelStatusEnum];


/**
 * Check if a given object implements the JobBasicModel interface.
 */
export function instanceOfJobBasicModel(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "priority" in value;
    isInstance = isInstance && "startDate" in value;
    isInstance = isInstance && "status" in value;

    return isInstance;
}

export function JobBasicModelFromJSON(json: any): JobBasicModel {
    return JobBasicModelFromJSONTyped(json, false);
}

export function JobBasicModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): JobBasicModel {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'createdBy': !exists(json, 'createdBy') ? undefined : json['createdBy'],
        'createdDate': !exists(json, 'createdDate') ? undefined : (new Date(json['createdDate'])),
        'endDate': !exists(json, 'endDate') ? undefined : (new Date(json['endDate'])),
        'id': !exists(json, 'id') ? undefined : json['id'],
        'label': !exists(json, 'label') ? undefined : json['label'],
        'lastModifiedBy': !exists(json, 'lastModifiedBy') ? undefined : json['lastModifiedBy'],
        'lastModifiedDate': !exists(json, 'lastModifiedDate') ? undefined : (new Date(json['lastModifiedDate'])),
        'priority': json['priority'],
        'startDate': (new Date(json['startDate'])),
        'status': json['status'],
        'workflowId': !exists(json, 'workflowId') ? undefined : json['workflowId'],
    };
}

export function JobBasicModelToJSON(value?: JobBasicModel | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'endDate': value.endDate === undefined ? undefined : (value.endDate.toISOString()),
    };
}

