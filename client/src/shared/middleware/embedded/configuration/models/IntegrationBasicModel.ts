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
import type { IntegrationStatusModel } from './IntegrationStatusModel';
import {
    IntegrationStatusModelFromJSON,
    IntegrationStatusModelFromJSONTyped,
    IntegrationStatusModelToJSON,
} from './IntegrationStatusModel';

/**
 * A group of workflows that make one logical integration.
 * @export
 * @interface IntegrationBasicModel
 */
export interface IntegrationBasicModel {
    /**
     * If multiple instances of an integration are allowed or not.
     * @type {boolean}
     * @memberof IntegrationBasicModel
     */
    allowMultipleInstances: boolean;
    /**
     * The name of the integration's component.
     * @type {string}
     * @memberof IntegrationBasicModel
     */
    componentName: string;
    /**
     * The version of the integration's component.
     * @type {number}
     * @memberof IntegrationBasicModel
     */
    componentVersion: number;
    /**
     * The created by.
     * @type {string}
     * @memberof IntegrationBasicModel
     */
    readonly createdBy?: string;
    /**
     * The created date.
     * @type {Date}
     * @memberof IntegrationBasicModel
     */
    readonly createdDate?: Date;
    /**
     * The description of an integration.
     * @type {string}
     * @memberof IntegrationBasicModel
     */
    description?: string;
    /**
     * The id of an integration.
     * @type {number}
     * @memberof IntegrationBasicModel
     */
    readonly id?: number;
    /**
     * The last modified by.
     * @type {string}
     * @memberof IntegrationBasicModel
     */
    readonly lastModifiedBy?: string;
    /**
     * The last modified date.
     * @type {Date}
     * @memberof IntegrationBasicModel
     */
    readonly lastModifiedDate?: Date;
    /**
     * The last published date.
     * @type {Date}
     * @memberof IntegrationBasicModel
     */
    readonly lastPublishedDate?: Date;
    /**
     * 
     * @type {IntegrationStatusModel}
     * @memberof IntegrationBasicModel
     */
    lastStatus?: IntegrationStatusModel;
    /**
     * The last version of an integration.
     * @type {number}
     * @memberof IntegrationBasicModel
     */
    readonly lastVersion?: number;
}

/**
 * Check if a given object implements the IntegrationBasicModel interface.
 */
export function instanceOfIntegrationBasicModel(value: object): boolean {
    if (!('allowMultipleInstances' in value)) return false;
    if (!('componentName' in value)) return false;
    if (!('componentVersion' in value)) return false;
    return true;
}

export function IntegrationBasicModelFromJSON(json: any): IntegrationBasicModel {
    return IntegrationBasicModelFromJSONTyped(json, false);
}

export function IntegrationBasicModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): IntegrationBasicModel {
    if (json == null) {
        return json;
    }
    return {
        
        'allowMultipleInstances': json['allowMultipleInstances'],
        'componentName': json['componentName'],
        'componentVersion': json['componentVersion'],
        'createdBy': json['createdBy'] == null ? undefined : json['createdBy'],
        'createdDate': json['createdDate'] == null ? undefined : (new Date(json['createdDate'])),
        'description': json['description'] == null ? undefined : json['description'],
        'id': json['id'] == null ? undefined : json['id'],
        'lastModifiedBy': json['lastModifiedBy'] == null ? undefined : json['lastModifiedBy'],
        'lastModifiedDate': json['lastModifiedDate'] == null ? undefined : (new Date(json['lastModifiedDate'])),
        'lastPublishedDate': json['lastPublishedDate'] == null ? undefined : (new Date(json['lastPublishedDate'])),
        'lastStatus': json['lastStatus'] == null ? undefined : IntegrationStatusModelFromJSON(json['lastStatus']),
        'lastVersion': json['lastVersion'] == null ? undefined : json['lastVersion'],
    };
}

export function IntegrationBasicModelToJSON(value?: Omit<IntegrationBasicModel, 'createdBy'|'createdDate'|'id'|'lastModifiedBy'|'lastModifiedDate'|'lastPublishedDate'|'lastVersion'> | null): any {
    if (value == null) {
        return value;
    }
    return {
        
        'allowMultipleInstances': value['allowMultipleInstances'],
        'componentName': value['componentName'],
        'componentVersion': value['componentVersion'],
        'description': value['description'],
        'lastStatus': IntegrationStatusModelToJSON(value['lastStatus']),
    };
}

