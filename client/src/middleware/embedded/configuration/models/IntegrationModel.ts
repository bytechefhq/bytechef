/* tslint:disable */
/* eslint-disable */
/**
 * The Embedded Configuration API
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
import type { CategoryModel } from './CategoryModel';
import {
    CategoryModelFromJSON,
    CategoryModelFromJSONTyped,
    CategoryModelToJSON,
} from './CategoryModel';
import type { TagModel } from './TagModel';
import {
    TagModelFromJSON,
    TagModelFromJSONTyped,
    TagModelToJSON,
} from './TagModel';

/**
 * A group of workflows that make one logical integration.
 * @export
 * @interface IntegrationModel
 */
export interface IntegrationModel {
    /**
     * 
     * @type {CategoryModel}
     * @memberof IntegrationModel
     */
    category?: CategoryModel;
    /**
     * The created by.
     * @type {string}
     * @memberof IntegrationModel
     */
    readonly createdBy?: string;
    /**
     * The created date.
     * @type {Date}
     * @memberof IntegrationModel
     */
    readonly createdDate?: Date;
    /**
     * The name of the integration's component.
     * @type {string}
     * @memberof IntegrationModel
     */
    componentName?: string;
    /**
     * The version of the integration's component.
     * @type {number}
     * @memberof IntegrationModel
     */
    componentVersion?: number;
    /**
     * The id of an integration.
     * @type {number}
     * @memberof IntegrationModel
     */
    readonly id?: number;
    /**
     * The version of an integration.
     * @type {number}
     * @memberof IntegrationModel
     */
    integrationVersion?: number;
    /**
     * The last modified by.
     * @type {string}
     * @memberof IntegrationModel
     */
    readonly lastModifiedBy?: string;
    /**
     * The last modified date.
     * @type {Date}
     * @memberof IntegrationModel
     */
    readonly lastModifiedDate?: Date;
    /**
     * The overview of an integration.
     * @type {string}
     * @memberof IntegrationModel
     */
    overview?: string;
    /**
     * The published date.
     * @type {Date}
     * @memberof IntegrationModel
     */
    publishedDate?: Date;
    /**
     * A status of an integration.
     * @type {string}
     * @memberof IntegrationModel
     */
    status?: IntegrationModelStatusEnum;
    /**
     * 
     * @type {Array<TagModel>}
     * @memberof IntegrationModel
     */
    tags?: Array<TagModel>;
    /**
     * The workflow ids belonging to this integration.
     * @type {Array<string>}
     * @memberof IntegrationModel
     */
    workflowIds?: Array<string>;
    /**
     * 
     * @type {number}
     * @memberof IntegrationModel
     */
    version?: number;
}


/**
 * @export
 */
export const IntegrationModelStatusEnum = {
    Published: 'PUBLISHED',
    Unpublished: 'UNPUBLISHED'
} as const;
export type IntegrationModelStatusEnum = typeof IntegrationModelStatusEnum[keyof typeof IntegrationModelStatusEnum];


/**
 * Check if a given object implements the IntegrationModel interface.
 */
export function instanceOfIntegrationModel(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function IntegrationModelFromJSON(json: any): IntegrationModel {
    return IntegrationModelFromJSONTyped(json, false);
}

export function IntegrationModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): IntegrationModel {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'category': !exists(json, 'category') ? undefined : CategoryModelFromJSON(json['category']),
        'createdBy': !exists(json, 'createdBy') ? undefined : json['createdBy'],
        'createdDate': !exists(json, 'createdDate') ? undefined : (new Date(json['createdDate'])),
        'componentName': !exists(json, 'componentName') ? undefined : json['componentName'],
        'componentVersion': !exists(json, 'componentVersion') ? undefined : json['componentVersion'],
        'id': !exists(json, 'id') ? undefined : json['id'],
        'integrationVersion': !exists(json, 'integrationVersion') ? undefined : json['integrationVersion'],
        'lastModifiedBy': !exists(json, 'lastModifiedBy') ? undefined : json['lastModifiedBy'],
        'lastModifiedDate': !exists(json, 'lastModifiedDate') ? undefined : (new Date(json['lastModifiedDate'])),
        'overview': !exists(json, 'overview') ? undefined : json['overview'],
        'publishedDate': !exists(json, 'publishedDate') ? undefined : (new Date(json['publishedDate'])),
        'status': !exists(json, 'status') ? undefined : json['status'],
        'tags': !exists(json, 'tags') ? undefined : ((json['tags'] as Array<any>).map(TagModelFromJSON)),
        'workflowIds': !exists(json, 'workflowIds') ? undefined : json['workflowIds'],
        'version': !exists(json, '__version') ? undefined : json['__version'],
    };
}

export function IntegrationModelToJSON(value?: IntegrationModel | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'category': CategoryModelToJSON(value.category),
        'componentName': value.componentName,
        'componentVersion': value.componentVersion,
        'integrationVersion': value.integrationVersion,
        'overview': value.overview,
        'publishedDate': value.publishedDate === undefined ? undefined : (value.publishedDate.toISOString()),
        'status': value.status,
        'tags': value.tags === undefined ? undefined : ((value.tags as Array<any>).map(TagModelToJSON)),
        'workflowIds': value.workflowIds,
        '__version': value.version,
    };
}

