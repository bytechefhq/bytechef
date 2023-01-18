/* tslint:disable */
/* eslint-disable */
/**
 * OpenAPI definition
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: v1
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


import * as runtime from '../runtime';
import type {
  ConnectionsModel,
} from '../models';
import {
    ConnectionsModelFromJSON,
    ConnectionsModelToJSON,
} from '../models';

/**
 * 
 */
export class ConnectionDefinitionsApi extends runtime.BaseAPI {

    /**
     * Returns all connection definitions.
     */
    async getConnectionDefinitionsRaw(initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<ConnectionsModel>>> {
        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/definitions/connections`,
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => jsonValue.map(ConnectionsModelFromJSON));
    }

    /**
     * Returns all connection definitions.
     */
    async getConnectionDefinitions(initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<ConnectionsModel>> {
        const response = await this.getConnectionDefinitionsRaw(initOverrides);
        return await response.value();
    }

}
