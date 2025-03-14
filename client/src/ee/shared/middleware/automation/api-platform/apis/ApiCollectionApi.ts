/* tslint:disable */
/* eslint-disable */
/**
 * The Automation API Platform Internal API
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
  ApiCollection,
  Environment,
} from '../models/index';
import {
    ApiCollectionFromJSON,
    ApiCollectionToJSON,
    EnvironmentFromJSON,
    EnvironmentToJSON,
} from '../models/index';

export interface CreateApiCollectionRequest {
    apiCollection: Omit<ApiCollection, 'createdBy'|'createdDate'|'id'|'lastModifiedBy'|'lastModifiedDate'|'projectDeploymentId'>;
}

export interface DeleteApiCollectionRequest {
    id: number;
}

export interface GetApiCollectionRequest {
    id: number;
}

export interface GetWorkspaceApiCollectionsRequest {
    id: number;
    environment?: Environment;
    projectId?: number;
    tagId?: number;
}

export interface UpdateApiCollectionRequest {
    id: number;
    apiCollection: Omit<ApiCollection, 'createdBy'|'createdDate'|'id'|'lastModifiedBy'|'lastModifiedDate'|'projectDeploymentId'>;
}

/**
 * 
 */
export class ApiCollectionApi extends runtime.BaseAPI {

    /**
     * Create a new API collection.
     * Create a new API collection
     */
    async createApiCollectionRaw(requestParameters: CreateApiCollectionRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<ApiCollection>> {
        if (requestParameters['apiCollection'] == null) {
            throw new runtime.RequiredError(
                'apiCollection',
                'Required parameter "apiCollection" was null or undefined when calling createApiCollection().'
            );
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        const response = await this.request({
            path: `/api-collections`,
            method: 'POST',
            headers: headerParameters,
            query: queryParameters,
            body: ApiCollectionToJSON(requestParameters['apiCollection']),
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => ApiCollectionFromJSON(jsonValue));
    }

    /**
     * Create a new API collection.
     * Create a new API collection
     */
    async createApiCollection(requestParameters: CreateApiCollectionRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<ApiCollection> {
        const response = await this.createApiCollectionRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Delete an API collection.
     * Delete an API collection
     */
    async deleteApiCollectionRaw(requestParameters: DeleteApiCollectionRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<void>> {
        if (requestParameters['id'] == null) {
            throw new runtime.RequiredError(
                'id',
                'Required parameter "id" was null or undefined when calling deleteApiCollection().'
            );
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/api-collections/{id}`.replace(`{${"id"}}`, encodeURIComponent(String(requestParameters['id']))),
            method: 'DELETE',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.VoidApiResponse(response);
    }

    /**
     * Delete an API collection.
     * Delete an API collection
     */
    async deleteApiCollection(requestParameters: DeleteApiCollectionRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<void> {
        await this.deleteApiCollectionRaw(requestParameters, initOverrides);
    }

    /**
     * Get an API collection by id.
     * Get an API collection by id
     */
    async getApiCollectionRaw(requestParameters: GetApiCollectionRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<ApiCollection>> {
        if (requestParameters['id'] == null) {
            throw new runtime.RequiredError(
                'id',
                'Required parameter "id" was null or undefined when calling getApiCollection().'
            );
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/api-collections/{id}`.replace(`{${"id"}}`, encodeURIComponent(String(requestParameters['id']))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => ApiCollectionFromJSON(jsonValue));
    }

    /**
     * Get an API collection by id.
     * Get an API collection by id
     */
    async getApiCollection(requestParameters: GetApiCollectionRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<ApiCollection> {
        const response = await this.getApiCollectionRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Get all workspace API collections.
     * Get all workspace API collections
     */
    async getWorkspaceApiCollectionsRaw(requestParameters: GetWorkspaceApiCollectionsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<ApiCollection>>> {
        if (requestParameters['id'] == null) {
            throw new runtime.RequiredError(
                'id',
                'Required parameter "id" was null or undefined when calling getWorkspaceApiCollections().'
            );
        }

        const queryParameters: any = {};

        if (requestParameters['environment'] != null) {
            queryParameters['environment'] = requestParameters['environment'];
        }

        if (requestParameters['projectId'] != null) {
            queryParameters['projectId'] = requestParameters['projectId'];
        }

        if (requestParameters['tagId'] != null) {
            queryParameters['tagId'] = requestParameters['tagId'];
        }

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/workspaces/{id}/api-collections`.replace(`{${"id"}}`, encodeURIComponent(String(requestParameters['id']))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => jsonValue.map(ApiCollectionFromJSON));
    }

    /**
     * Get all workspace API collections.
     * Get all workspace API collections
     */
    async getWorkspaceApiCollections(requestParameters: GetWorkspaceApiCollectionsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<ApiCollection>> {
        const response = await this.getWorkspaceApiCollectionsRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Update an existing API collection.
     * Update an existing API collection
     */
    async updateApiCollectionRaw(requestParameters: UpdateApiCollectionRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<ApiCollection>> {
        if (requestParameters['id'] == null) {
            throw new runtime.RequiredError(
                'id',
                'Required parameter "id" was null or undefined when calling updateApiCollection().'
            );
        }

        if (requestParameters['apiCollection'] == null) {
            throw new runtime.RequiredError(
                'apiCollection',
                'Required parameter "apiCollection" was null or undefined when calling updateApiCollection().'
            );
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        const response = await this.request({
            path: `/api-collections/{id}`.replace(`{${"id"}}`, encodeURIComponent(String(requestParameters['id']))),
            method: 'PUT',
            headers: headerParameters,
            query: queryParameters,
            body: ApiCollectionToJSON(requestParameters['apiCollection']),
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => ApiCollectionFromJSON(jsonValue));
    }

    /**
     * Update an existing API collection.
     * Update an existing API collection
     */
    async updateApiCollection(requestParameters: UpdateApiCollectionRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<ApiCollection> {
        const response = await this.updateApiCollectionRaw(requestParameters, initOverrides);
        return await response.value();
    }

}
