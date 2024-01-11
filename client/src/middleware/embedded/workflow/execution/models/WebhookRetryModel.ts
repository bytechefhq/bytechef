/* tslint:disable */
/* eslint-disable */
/**
 * Embedded Execution API
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
 * 
 * @export
 * @interface WebhookRetryModel
 */
export interface WebhookRetryModel {
    /**
     * 
     * @type {number}
     * @memberof WebhookRetryModel
     */
    initialInterval?: number;
    /**
     * 
     * @type {number}
     * @memberof WebhookRetryModel
     */
    maxInterval?: number;
    /**
     * 
     * @type {number}
     * @memberof WebhookRetryModel
     */
    maxAttempts?: number;
    /**
     * 
     * @type {number}
     * @memberof WebhookRetryModel
     */
    multiplier?: number;
}

/**
 * Check if a given object implements the WebhookRetryModel interface.
 */
export function instanceOfWebhookRetryModel(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function WebhookRetryModelFromJSON(json: any): WebhookRetryModel {
    return WebhookRetryModelFromJSONTyped(json, false);
}

export function WebhookRetryModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): WebhookRetryModel {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'initialInterval': !exists(json, 'initialInterval') ? undefined : json['initialInterval'],
        'maxInterval': !exists(json, 'maxInterval') ? undefined : json['maxInterval'],
        'maxAttempts': !exists(json, 'maxAttempts') ? undefined : json['maxAttempts'],
        'multiplier': !exists(json, 'multiplier') ? undefined : json['multiplier'],
    };
}

export function WebhookRetryModelToJSON(value?: WebhookRetryModel | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'initialInterval': value.initialInterval,
        'maxInterval': value.maxInterval,
        'maxAttempts': value.maxAttempts,
        'multiplier': value.multiplier,
    };
}

