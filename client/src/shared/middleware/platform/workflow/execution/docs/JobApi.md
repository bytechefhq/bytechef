# JobApi

All URIs are relative to */api/platform/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getJob**](JobApi.md#getjob) | **GET** /jobs/{id} | Get a job by id |
| [**getJobsPage**](JobApi.md#getjobspage) | **GET** /jobs | Get a page of jobs |
| [**getLatestJob**](JobApi.md#getlatestjob) | **GET** /jobs/latest | Get the latest job |
| [**getLatestTriggerExecution**](JobApi.md#getlatesttriggerexecution) | **GET** /trigger-executions/latest | Get the latest trigger execution |
| [**restartJob**](JobApi.md#restartjob) | **PUT** /jobs/{id}/restart | Restart a job |
| [**stopJob**](JobApi.md#stopjob) | **PUT** /jobs/{id}/stop | Stop a job |



## getJob

> Job getJob(id)

Get a job by id

Get a job by id.

### Example

```ts
import {
  Configuration,
  JobApi,
} from '';
import type { GetJobRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new JobApi();

  const body = {
    // number | The id of a job to return.
    id: 789,
  } satisfies GetJobRequest;

  try {
    const data = await api.getJob(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **id** | `number` | The id of a job to return. | [Defaults to `undefined`] |

### Return type

[**Job**](Job.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The job object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getJobsPage

> Page getJobsPage(pageNumber)

Get a page of jobs

Get a page of jobs.

### Example

```ts
import {
  Configuration,
  JobApi,
} from '';
import type { GetJobsPageRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new JobApi();

  const body = {
    // number | The number of the page to return. (optional)
    pageNumber: 56,
  } satisfies GetJobsPageRequest;

  try {
    const data = await api.getJobsPage(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **pageNumber** | `number` | The number of the page to return. | [Optional] [Defaults to `0`] |

### Return type

[**Page**](Page.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The page of jobs. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getLatestJob

> Job getLatestJob()

Get the latest job

Get the latest job.

### Example

```ts
import {
  Configuration,
  JobApi,
} from '';
import type { GetLatestJobRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new JobApi();

  try {
    const data = await api.getLatestJob();
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters

This endpoint does not need any parameter.

### Return type

[**Job**](Job.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The latest job. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getLatestTriggerExecution

> TriggerExecution getLatestTriggerExecution()

Get the latest trigger execution

Get the latest trigger execution.

### Example

```ts
import {
  Configuration,
  JobApi,
} from '';
import type { GetLatestTriggerExecutionRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new JobApi();

  try {
    const data = await api.getLatestTriggerExecution();
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters

This endpoint does not need any parameter.

### Return type

[**TriggerExecution**](TriggerExecution.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The latest trigger execution. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## restartJob

> restartJob(id)

Restart a job

Restart a job.

### Example

```ts
import {
  Configuration,
  JobApi,
} from '';
import type { RestartJobRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new JobApi();

  const body = {
    // number | The id of a job to restart.
    id: 789,
  } satisfies RestartJobRequest;

  try {
    const data = await api.restartJob(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **id** | `number` | The id of a job to restart. | [Defaults to `undefined`] |

### Return type

`void` (Empty response body)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Successful operation. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## stopJob

> stopJob(id)

Stop a job

Stop a job.

### Example

```ts
import {
  Configuration,
  JobApi,
} from '';
import type { StopJobRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new JobApi();

  const body = {
    // number | The id of a job to stop.
    id: 789,
  } satisfies StopJobRequest;

  try {
    const data = await api.stopJob(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **id** | `number` | The id of a job to stop. | [Defaults to `undefined`] |

### Return type

`void` (Empty response body)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Successful operation. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

