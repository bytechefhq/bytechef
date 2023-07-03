/* tslint:disable */
/* eslint-disable */
/**
 * Automation Connection API
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
  ConnectionModel,
  TagModel,
  UpdateTagsRequestModel,
} from '../models';
import {
    ConnectionModelFromJSON,
    ConnectionModelToJSON,
    TagModelFromJSON,
    TagModelToJSON,
    UpdateTagsRequestModelFromJSON,
    UpdateTagsRequestModelToJSON,
} from '../models';

export interface CreateConnectionRequest {
    connectionModel: ConnectionModel;
}

export interface DeleteConnectionRequest {
    id: number;
}

export interface GetComponentConnectionsRequest {
    componentName: string;
    componentVersion: number;
}

export interface GetConnectionRequest {
    id: number;
}

export interface GetConnectionsRequest {
    componentNames?: Array<string>;
    tagIds?: Array<number>;
}

export interface UpdateConnectionRequest {
    id: number;
    connectionModel: ConnectionModel;
}

export interface UpdateConnectionTagsRequest {
    id: number;
    updateTagsRequestModel: UpdateTagsRequestModel;
}

/**
 * 
 */
export class ConnectionsApi extends runtime.BaseAPI {

    /**
     * Create a new connection.
     * Create a new connection
     */
    async createConnectionRaw(requestParameters: CreateConnectionRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<ConnectionModel>> {
        if (requestParameters.connectionModel === null || requestParameters.connectionModel === undefined) {
            throw new runtime.RequiredError('connectionModel','Required parameter requestParameters.connectionModel was null or undefined when calling createConnection.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        const response = await this.request({
            path: `/connections`,
            method: 'POST',
            headers: headerParameters,
            query: queryParameters,
            body: ConnectionModelToJSON(requestParameters.connectionModel),
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => ConnectionModelFromJSON(jsonValue));
    }

    /**
     * Create a new connection.
     * Create a new connection
     */
    async createConnection(requestParameters: CreateConnectionRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<ConnectionModel> {
        const response = await this.createConnectionRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Delete a connection.
     * Delete a connection
     */
    async deleteConnectionRaw(requestParameters: DeleteConnectionRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<void>> {
        if (requestParameters.id === null || requestParameters.id === undefined) {
            throw new runtime.RequiredError('id','Required parameter requestParameters.id was null or undefined when calling deleteConnection.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/connections/{id}`.replace(`{${"id"}}`, encodeURIComponent(String(requestParameters.id))),
            method: 'DELETE',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.VoidApiResponse(response);
    }

    /**
     * Delete a connection.
     * Delete a connection
     */
    async deleteConnection(requestParameters: DeleteConnectionRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<void> {
        await this.deleteConnectionRaw(requestParameters, initOverrides);
    }

    /**
     * Get component connections.
     * Get component connections
     */
    async getComponentConnectionsRaw(requestParameters: GetComponentConnectionsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<ConnectionModel>>> {
        if (requestParameters.componentName === null || requestParameters.componentName === undefined) {
            throw new runtime.RequiredError('componentName','Required parameter requestParameters.componentName was null or undefined when calling getComponentConnections.');
        }

        if (requestParameters.componentVersion === null || requestParameters.componentVersion === undefined) {
            throw new runtime.RequiredError('componentVersion','Required parameter requestParameters.componentVersion was null or undefined when calling getComponentConnections.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/component-definitions/{componentName}/{componentVersion}/connections`.replace(`{${"componentName"}}`, encodeURIComponent(String(requestParameters.componentName))).replace(`{${"componentVersion"}}`, encodeURIComponent(String(requestParameters.componentVersion))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => jsonValue.map(ConnectionModelFromJSON));
    }

    /**
     * Get component connections.
     * Get component connections
     */
    async getComponentConnections(requestParameters: GetComponentConnectionsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<ConnectionModel>> {
        const response = await this.getComponentConnectionsRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Get a connection by id.
     * Get a connection by id
     */
    async getConnectionRaw(requestParameters: GetConnectionRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<ConnectionModel>> {
        if (requestParameters.id === null || requestParameters.id === undefined) {
            throw new runtime.RequiredError('id','Required parameter requestParameters.id was null or undefined when calling getConnection.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/connections/{id}`.replace(`{${"id"}}`, encodeURIComponent(String(requestParameters.id))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => ConnectionModelFromJSON(jsonValue));
    }

    /**
     * Get a connection by id.
     * Get a connection by id
     */
    async getConnection(requestParameters: GetConnectionRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<ConnectionModel> {
        const response = await this.getConnectionRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Get connection tags.
     * Get connection tags
     */
    async getConnectionTagsRaw(initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<TagModel>>> {
        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/connections/tags`,
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => jsonValue.map(TagModelFromJSON));
    }

    /**
     * Get connection tags.
     * Get connection tags
     */
    async getConnectionTags(initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<TagModel>> {
        const response = await this.getConnectionTagsRaw(initOverrides);
        return await response.value();
    }

    /**
     * Get all connections.
     * Get all connections
     */
    async getConnectionsRaw(requestParameters: GetConnectionsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<ConnectionModel>>> {
        const queryParameters: any = {};

        if (requestParameters.componentNames) {
            queryParameters['componentNames'] = requestParameters.componentNames;
        }

        if (requestParameters.tagIds) {
            queryParameters['tagIds'] = requestParameters.tagIds;
        }

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/connections`,
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => jsonValue.map(ConnectionModelFromJSON));
    }

    /**
     * Get all connections.
     * Get all connections
     */
    async getConnections(requestParameters: GetConnectionsRequest = {}, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<ConnectionModel>> {
        const response = await this.getConnectionsRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Update an existing connection.
     * Update an existing connection
     */
    async updateConnectionRaw(requestParameters: UpdateConnectionRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<ConnectionModel>> {
        if (requestParameters.id === null || requestParameters.id === undefined) {
            throw new runtime.RequiredError('id','Required parameter requestParameters.id was null or undefined when calling updateConnection.');
        }

        if (requestParameters.connectionModel === null || requestParameters.connectionModel === undefined) {
            throw new runtime.RequiredError('connectionModel','Required parameter requestParameters.connectionModel was null or undefined when calling updateConnection.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        const response = await this.request({
            path: `/connections/{id}`.replace(`{${"id"}}`, encodeURIComponent(String(requestParameters.id))),
            method: 'PUT',
            headers: headerParameters,
            query: queryParameters,
            body: ConnectionModelToJSON(requestParameters.connectionModel),
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => ConnectionModelFromJSON(jsonValue));
    }

    /**
     * Update an existing connection.
     * Update an existing connection
     */
    async updateConnection(requestParameters: UpdateConnectionRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<ConnectionModel> {
        const response = await this.updateConnectionRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Updates tags of an existing connection.
     * Updates tags of an existing connection
     */
    async updateConnectionTagsRaw(requestParameters: UpdateConnectionTagsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<void>> {
        if (requestParameters.id === null || requestParameters.id === undefined) {
            throw new runtime.RequiredError('id','Required parameter requestParameters.id was null or undefined when calling updateConnectionTags.');
        }

        if (requestParameters.updateTagsRequestModel === null || requestParameters.updateTagsRequestModel === undefined) {
            throw new runtime.RequiredError('updateTagsRequestModel','Required parameter requestParameters.updateTagsRequestModel was null or undefined when calling updateConnectionTags.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        const response = await this.request({
            path: `/connections/{id}/tags`.replace(`{${"id"}}`, encodeURIComponent(String(requestParameters.id))),
            method: 'PUT',
            headers: headerParameters,
            query: queryParameters,
            body: UpdateTagsRequestModelToJSON(requestParameters.updateTagsRequestModel),
        }, initOverrides);

        return new runtime.VoidApiResponse(response);
    }

    /**
     * Updates tags of an existing connection.
     * Updates tags of an existing connection
     */
    async updateConnectionTags(requestParameters: UpdateConnectionTagsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<void> {
        await this.updateConnectionTagsRaw(requestParameters, initOverrides);
    }

}
