/* tslint:disable */
/* eslint-disable */
/**
 * The Platform User Internal API
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: 1
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


import * as runtime from '../runtime';
import type {
  Authority,
} from '../models/index';
import {
    AuthorityFromJSON,
    AuthorityToJSON,
} from '../models/index';

/**
 * 
 */
export class AuthorityApi extends runtime.BaseAPI {

    /**
     * Get all authorities.
     * Get all authorities
     */
    async getAuthoritiesRaw(initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<Authority>>> {
        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/authorities`,
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => jsonValue.map(AuthorityFromJSON));
    }

    /**
     * Get all authorities.
     * Get all authorities
     */
    async getAuthorities(initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<Authority>> {
        const response = await this.getAuthoritiesRaw(initOverrides);
        return await response.value();
    }

}
