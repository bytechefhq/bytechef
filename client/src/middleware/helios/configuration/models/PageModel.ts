/* tslint:disable */
/* eslint-disable */
/**
 * Automation Configuration API
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
 * A sublist of a list of objects. It allows gain information about the position of it in the containing entire list.
 * @export
 * @interface PageModel
 */
export interface PageModel {
    /**
     * The current page.
     * @type {number}
     * @memberof PageModel
     */
    number?: number;
    /**
     * The size of the page.
     * @type {number}
     * @memberof PageModel
     */
    size?: number;
    /**
     * The number of elements.
     * @type {number}
     * @memberof PageModel
     */
    numberOfElements?: number;
    /**
     * The total number of pages.
     * @type {number}
     * @memberof PageModel
     */
    totalPages?: number;
    /**
     * The total number of elements.
     * @type {number}
     * @memberof PageModel
     */
    totalElements?: number;
    /**
     * The list of elements.
     * @type {Array<object>}
     * @memberof PageModel
     */
    content?: Array<object>;
}

/**
 * Check if a given object implements the PageModel interface.
 */
export function instanceOfPageModel(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function PageModelFromJSON(json: any): PageModel {
    return PageModelFromJSONTyped(json, false);
}

export function PageModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): PageModel {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'number': !exists(json, 'number') ? undefined : json['number'],
        'size': !exists(json, 'size') ? undefined : json['size'],
        'numberOfElements': !exists(json, 'numberOfElements') ? undefined : json['numberOfElements'],
        'totalPages': !exists(json, 'totalPages') ? undefined : json['totalPages'],
        'totalElements': !exists(json, 'totalElements') ? undefined : json['totalElements'],
        'content': !exists(json, 'content') ? undefined : json['content'],
    };
}

export function PageModelToJSON(value?: PageModel | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'number': value.number,
        'size': value.size,
        'numberOfElements': value.numberOfElements,
        'totalPages': value.totalPages,
        'totalElements': value.totalElements,
        'content': value.content,
    };
}

