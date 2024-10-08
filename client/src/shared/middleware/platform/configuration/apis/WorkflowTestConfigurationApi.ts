/* tslint:disable */
/* eslint-disable */
/**
 * The Platform Configuration Internal API
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
  SaveWorkflowTestConfigurationConnectionRequest,
  SaveWorkflowTestConfigurationInputsRequest,
  WorkflowTestConfiguration,
  WorkflowTestConfigurationConnection,
} from '../models/index';
import {
    SaveWorkflowTestConfigurationConnectionRequestFromJSON,
    SaveWorkflowTestConfigurationConnectionRequestToJSON,
    SaveWorkflowTestConfigurationInputsRequestFromJSON,
    SaveWorkflowTestConfigurationInputsRequestToJSON,
    WorkflowTestConfigurationFromJSON,
    WorkflowTestConfigurationToJSON,
    WorkflowTestConfigurationConnectionFromJSON,
    WorkflowTestConfigurationConnectionToJSON,
} from '../models/index';

export interface GetWorkflowTestConfigurationRequest {
    workflowId: string;
}

export interface GetWorkflowTestConfigurationConnectionsRequest {
    workflowId: string;
    workflowNodeName: string;
}

export interface SaveWorkflowTestConfigurationRequest {
    workflowId: string;
    workflowTestConfiguration: Omit<WorkflowTestConfiguration, 'createdBy'|'createdDate'|'lastModifiedBy'|'lastModifiedDate'|'workflowId'>;
}

export interface SaveWorkflowTestConfigurationConnectionOperationRequest {
    workflowId: string;
    workflowNodeName: string;
    workflowConnectionKey: string;
    saveWorkflowTestConfigurationConnectionRequest: SaveWorkflowTestConfigurationConnectionRequest;
}

export interface SaveWorkflowTestConfigurationInputsOperationRequest {
    workflowId: string;
    saveWorkflowTestConfigurationInputsRequest: SaveWorkflowTestConfigurationInputsRequest;
}

/**
 * 
 */
export class WorkflowTestConfigurationApi extends runtime.BaseAPI {

    /**
     * Get a workflow test configuration.
     * Get a workflow test configuration
     */
    async getWorkflowTestConfigurationRaw(requestParameters: GetWorkflowTestConfigurationRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<WorkflowTestConfiguration>> {
        if (requestParameters['workflowId'] == null) {
            throw new runtime.RequiredError(
                'workflowId',
                'Required parameter "workflowId" was null or undefined when calling getWorkflowTestConfiguration().'
            );
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/workflow-test-configurations/{workflowId}`.replace(`{${"workflowId"}}`, encodeURIComponent(String(requestParameters['workflowId']))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => WorkflowTestConfigurationFromJSON(jsonValue));
    }

    /**
     * Get a workflow test configuration.
     * Get a workflow test configuration
     */
    async getWorkflowTestConfiguration(requestParameters: GetWorkflowTestConfigurationRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<WorkflowTestConfiguration> {
        const response = await this.getWorkflowTestConfigurationRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Get a workflow test configuration connections.
     * Get a workflow test configuration connections
     */
    async getWorkflowTestConfigurationConnectionsRaw(requestParameters: GetWorkflowTestConfigurationConnectionsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<WorkflowTestConfigurationConnection>>> {
        if (requestParameters['workflowId'] == null) {
            throw new runtime.RequiredError(
                'workflowId',
                'Required parameter "workflowId" was null or undefined when calling getWorkflowTestConfigurationConnections().'
            );
        }

        if (requestParameters['workflowNodeName'] == null) {
            throw new runtime.RequiredError(
                'workflowNodeName',
                'Required parameter "workflowNodeName" was null or undefined when calling getWorkflowTestConfigurationConnections().'
            );
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/workflow-test-configurations/{workflowId}/connections/{workflowNodeName}`.replace(`{${"workflowId"}}`, encodeURIComponent(String(requestParameters['workflowId']))).replace(`{${"workflowNodeName"}}`, encodeURIComponent(String(requestParameters['workflowNodeName']))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => jsonValue.map(WorkflowTestConfigurationConnectionFromJSON));
    }

    /**
     * Get a workflow test configuration connections.
     * Get a workflow test configuration connections
     */
    async getWorkflowTestConfigurationConnections(requestParameters: GetWorkflowTestConfigurationConnectionsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<WorkflowTestConfigurationConnection>> {
        const response = await this.getWorkflowTestConfigurationConnectionsRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Create new or update an existing workflow test configuration.
     * Create new or update an existing workflow test configuration
     */
    async saveWorkflowTestConfigurationRaw(requestParameters: SaveWorkflowTestConfigurationRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<WorkflowTestConfiguration>> {
        if (requestParameters['workflowId'] == null) {
            throw new runtime.RequiredError(
                'workflowId',
                'Required parameter "workflowId" was null or undefined when calling saveWorkflowTestConfiguration().'
            );
        }

        if (requestParameters['workflowTestConfiguration'] == null) {
            throw new runtime.RequiredError(
                'workflowTestConfiguration',
                'Required parameter "workflowTestConfiguration" was null or undefined when calling saveWorkflowTestConfiguration().'
            );
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        const response = await this.request({
            path: `/workflow-test-configurations/{workflowId}`.replace(`{${"workflowId"}}`, encodeURIComponent(String(requestParameters['workflowId']))),
            method: 'PUT',
            headers: headerParameters,
            query: queryParameters,
            body: WorkflowTestConfigurationToJSON(requestParameters['workflowTestConfiguration']),
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => WorkflowTestConfigurationFromJSON(jsonValue));
    }

    /**
     * Create new or update an existing workflow test configuration.
     * Create new or update an existing workflow test configuration
     */
    async saveWorkflowTestConfiguration(requestParameters: SaveWorkflowTestConfigurationRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<WorkflowTestConfiguration> {
        const response = await this.saveWorkflowTestConfigurationRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Save a workflow test configuration connection.
     * Save a workflow test configuration connection
     */
    async saveWorkflowTestConfigurationConnectionRaw(requestParameters: SaveWorkflowTestConfigurationConnectionOperationRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<void>> {
        if (requestParameters['workflowId'] == null) {
            throw new runtime.RequiredError(
                'workflowId',
                'Required parameter "workflowId" was null or undefined when calling saveWorkflowTestConfigurationConnection().'
            );
        }

        if (requestParameters['workflowNodeName'] == null) {
            throw new runtime.RequiredError(
                'workflowNodeName',
                'Required parameter "workflowNodeName" was null or undefined when calling saveWorkflowTestConfigurationConnection().'
            );
        }

        if (requestParameters['workflowConnectionKey'] == null) {
            throw new runtime.RequiredError(
                'workflowConnectionKey',
                'Required parameter "workflowConnectionKey" was null or undefined when calling saveWorkflowTestConfigurationConnection().'
            );
        }

        if (requestParameters['saveWorkflowTestConfigurationConnectionRequest'] == null) {
            throw new runtime.RequiredError(
                'saveWorkflowTestConfigurationConnectionRequest',
                'Required parameter "saveWorkflowTestConfigurationConnectionRequest" was null or undefined when calling saveWorkflowTestConfigurationConnection().'
            );
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        const response = await this.request({
            path: `/workflow-test-configurations/{workflowId}/connections/{workflowNodeName}/{workflowConnectionKey}`.replace(`{${"workflowId"}}`, encodeURIComponent(String(requestParameters['workflowId']))).replace(`{${"workflowNodeName"}}`, encodeURIComponent(String(requestParameters['workflowNodeName']))).replace(`{${"workflowConnectionKey"}}`, encodeURIComponent(String(requestParameters['workflowConnectionKey']))),
            method: 'PUT',
            headers: headerParameters,
            query: queryParameters,
            body: SaveWorkflowTestConfigurationConnectionRequestToJSON(requestParameters['saveWorkflowTestConfigurationConnectionRequest']),
        }, initOverrides);

        return new runtime.VoidApiResponse(response);
    }

    /**
     * Save a workflow test configuration connection.
     * Save a workflow test configuration connection
     */
    async saveWorkflowTestConfigurationConnection(requestParameters: SaveWorkflowTestConfigurationConnectionOperationRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<void> {
        await this.saveWorkflowTestConfigurationConnectionRaw(requestParameters, initOverrides);
    }

    /**
     * Save a workflow test configuration inputs.
     * Save a workflow test configuration inputs
     */
    async saveWorkflowTestConfigurationInputsRaw(requestParameters: SaveWorkflowTestConfigurationInputsOperationRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<void>> {
        if (requestParameters['workflowId'] == null) {
            throw new runtime.RequiredError(
                'workflowId',
                'Required parameter "workflowId" was null or undefined when calling saveWorkflowTestConfigurationInputs().'
            );
        }

        if (requestParameters['saveWorkflowTestConfigurationInputsRequest'] == null) {
            throw new runtime.RequiredError(
                'saveWorkflowTestConfigurationInputsRequest',
                'Required parameter "saveWorkflowTestConfigurationInputsRequest" was null or undefined when calling saveWorkflowTestConfigurationInputs().'
            );
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        const response = await this.request({
            path: `/workflow-test-configurations/{workflowId}/inputs`.replace(`{${"workflowId"}}`, encodeURIComponent(String(requestParameters['workflowId']))),
            method: 'PUT',
            headers: headerParameters,
            query: queryParameters,
            body: SaveWorkflowTestConfigurationInputsRequestToJSON(requestParameters['saveWorkflowTestConfigurationInputsRequest']),
        }, initOverrides);

        return new runtime.VoidApiResponse(response);
    }

    /**
     * Save a workflow test configuration inputs.
     * Save a workflow test configuration inputs
     */
    async saveWorkflowTestConfigurationInputs(requestParameters: SaveWorkflowTestConfigurationInputsOperationRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<void> {
        await this.saveWorkflowTestConfigurationInputsRaw(requestParameters, initOverrides);
    }

}
