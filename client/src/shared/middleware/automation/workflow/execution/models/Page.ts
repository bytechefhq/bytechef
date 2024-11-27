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
/**
 * A sublist of a list of objects. It allows gain information about the position of it in the containing entire list.
 * @export
 * @interface Page
 */
export interface Page {
    /**
     * The current page.
     * @type {number}
     * @memberof Page
     */
    number?: number;
    /**
     * The size of the page.
     * @type {number}
     * @memberof Page
     */
    size?: number;
    /**
     * The number of elements.
     * @type {number}
     * @memberof Page
     */
    numberOfElements?: number;
    /**
     * The total number of pages.
     * @type {number}
     * @memberof Page
     */
    totalPages?: number;
    /**
     * The total number of elements.
     * @type {number}
     * @memberof Page
     */
    totalElements?: number;
    /**
     * The list of elements.
     * @type {Array<object>}
     * @memberof Page
     */
    content?: Array<object>;
}

/**
 * Check if a given object implements the Page interface.
 */
export function instanceOfPage(value: object): value is Page {
    return true;
}

export function PageFromJSON(json: any): Page {
    return PageFromJSONTyped(json, false);
}

export function PageFromJSONTyped(json: any, ignoreDiscriminator: boolean): Page {
    if (json == null) {
        return json;
    }
    return {
        
        'number': json['number'] == null ? undefined : json['number'],
        'size': json['size'] == null ? undefined : json['size'],
        'numberOfElements': json['numberOfElements'] == null ? undefined : json['numberOfElements'],
        'totalPages': json['totalPages'] == null ? undefined : json['totalPages'],
        'totalElements': json['totalElements'] == null ? undefined : json['totalElements'],
        'content': json['content'] == null ? undefined : json['content'],
    };
}

export function PageToJSON(json: any): Page {
    return PageToJSONTyped(json, false);
}

export function PageToJSONTyped(value?: Page | null, ignoreDiscriminator: boolean = false): any {
    if (value == null) {
        return value;
    }

    return {
        
        'number': value['number'],
        'size': value['size'],
        'numberOfElements': value['numberOfElements'],
        'totalPages': value['totalPages'],
        'totalElements': value['totalElements'],
        'content': value['content'],
    };
}

