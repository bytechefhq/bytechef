/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.exception;

import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.exception.AbstractErrorType;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AppErrorType extends AbstractErrorType {

    /**
     * Marks a response as ByteChef's own app-level error, so a client can tell it from a status a workflow chose.
     */
    public static final String APP_ERROR_HEADER = "X-ByteChef-App-Error";

    public static final AppErrorType APP_NAME_ALREADY_EXISTS = new AppErrorType(100);
    public static final AppErrorType APP_NOT_FOUND = new AppErrorType(101);
    public static final AppErrorType APP_DISABLED = new AppErrorType(102);
    public static final AppErrorType APP_CONNECTION_NOT_FOUND = new AppErrorType(103);
    public static final AppErrorType APP_OPERATION_NOT_ALLOWED = new AppErrorType(104);
    public static final AppErrorType APP_CONNECTION_UNAVAILABLE = new AppErrorType(105);
    public static final AppErrorType APP_PROXY_PATH_REJECTED = new AppErrorType(106);
    public static final AppErrorType APP_PROXY_UNSUPPORTED = new AppErrorType(107);
    public static final AppErrorType APP_EXECUTION_TIMEOUT = new AppErrorType(108);
    public static final AppErrorType APP_BINDING_KEY_INVALID = new AppErrorType(109);
    public static final AppErrorType APP_BINDING_KEY_ALREADY_EXISTS = new AppErrorType(110);
    public static final AppErrorType APP_CONNECTION_NOT_BINDABLE = new AppErrorType(111);
    public static final AppErrorType APP_PROXY_REQUEST_TOO_LARGE = new AppErrorType(112);
    public static final AppErrorType APP_PROXY_RESPONSE_TOO_LARGE = new AppErrorType(113);
    public static final AppErrorType APP_VISIBILITY_UNSUPPORTED = new AppErrorType(114);
    public static final AppErrorType APP_PROXY_REQUEST_INVALID = new AppErrorType(115);
    public static final AppErrorType APP_PROXY_UPSTREAM_UNAVAILABLE = new AppErrorType(116);
    public static final AppErrorType APP_FIELD_TOO_LONG = new AppErrorType(117);
    public static final AppErrorType APP_WORKFLOW_NOT_FOUND = new AppErrorType(118);
    public static final AppErrorType APP_WORKFLOW_UNAVAILABLE = new AppErrorType(119);
    public static final AppErrorType APP_WORKFLOW_INPUT_INVALID = new AppErrorType(120);
    public static final AppErrorType APP_WORKFLOW_RUN_NOT_FOUND = new AppErrorType(121);
    public static final AppErrorType APP_WORKFLOW_NOT_BINDABLE = new AppErrorType(122);

    public static final AppErrorType APP_PROXY_STORAGE_FAILED = new AppErrorType(123);
    public static final AppErrorType APP_API_COLLECTION_NOT_FOUND = new AppErrorType(124);
    public static final AppErrorType APP_API_COLLECTION_NOT_BINDABLE = new AppErrorType(125);
    public static final AppErrorType APP_API_COLLECTION_UNAVAILABLE = new AppErrorType(126);
    public static final AppErrorType APP_API_ENDPOINT_NOT_FOUND = new AppErrorType(127);
    public static final AppErrorType APP_EXECUTION_FAILED = new AppErrorType(128);

    private static final Map<Integer, String> NAMES = Map.ofEntries(
        Map.entry(APP_NAME_ALREADY_EXISTS.getErrorKey(), "APP_NAME_ALREADY_EXISTS"),
        Map.entry(APP_NOT_FOUND.getErrorKey(), "APP_NOT_FOUND"),
        Map.entry(APP_DISABLED.getErrorKey(), "APP_DISABLED"),
        Map.entry(APP_CONNECTION_NOT_FOUND.getErrorKey(), "APP_CONNECTION_NOT_FOUND"),
        Map.entry(APP_OPERATION_NOT_ALLOWED.getErrorKey(), "APP_OPERATION_NOT_ALLOWED"),
        Map.entry(APP_CONNECTION_UNAVAILABLE.getErrorKey(), "APP_CONNECTION_UNAVAILABLE"),
        Map.entry(APP_PROXY_PATH_REJECTED.getErrorKey(), "APP_PROXY_PATH_REJECTED"),
        Map.entry(APP_PROXY_UNSUPPORTED.getErrorKey(), "APP_PROXY_UNSUPPORTED"),
        Map.entry(APP_EXECUTION_TIMEOUT.getErrorKey(), "APP_EXECUTION_TIMEOUT"),
        Map.entry(APP_BINDING_KEY_INVALID.getErrorKey(), "APP_BINDING_KEY_INVALID"),
        Map.entry(APP_BINDING_KEY_ALREADY_EXISTS.getErrorKey(), "APP_BINDING_KEY_ALREADY_EXISTS"),
        Map.entry(APP_CONNECTION_NOT_BINDABLE.getErrorKey(), "APP_CONNECTION_NOT_BINDABLE"),
        Map.entry(APP_PROXY_REQUEST_TOO_LARGE.getErrorKey(), "APP_PROXY_REQUEST_TOO_LARGE"),
        Map.entry(APP_PROXY_RESPONSE_TOO_LARGE.getErrorKey(), "APP_PROXY_RESPONSE_TOO_LARGE"),
        Map.entry(APP_VISIBILITY_UNSUPPORTED.getErrorKey(), "APP_VISIBILITY_UNSUPPORTED"),
        Map.entry(APP_PROXY_REQUEST_INVALID.getErrorKey(), "APP_PROXY_REQUEST_INVALID"),
        Map.entry(APP_PROXY_UPSTREAM_UNAVAILABLE.getErrorKey(), "APP_PROXY_UPSTREAM_UNAVAILABLE"),
        Map.entry(APP_FIELD_TOO_LONG.getErrorKey(), "APP_FIELD_TOO_LONG"),
        Map.entry(APP_WORKFLOW_NOT_FOUND.getErrorKey(), "APP_WORKFLOW_NOT_FOUND"),
        Map.entry(APP_WORKFLOW_UNAVAILABLE.getErrorKey(), "APP_WORKFLOW_UNAVAILABLE"),
        Map.entry(APP_WORKFLOW_INPUT_INVALID.getErrorKey(), "APP_WORKFLOW_INPUT_INVALID"),
        Map.entry(APP_WORKFLOW_RUN_NOT_FOUND.getErrorKey(), "APP_WORKFLOW_RUN_NOT_FOUND"),
        Map.entry(APP_WORKFLOW_NOT_BINDABLE.getErrorKey(), "APP_WORKFLOW_NOT_BINDABLE"),
        Map.entry(APP_PROXY_STORAGE_FAILED.getErrorKey(), "APP_PROXY_STORAGE_FAILED"),
        Map.entry(APP_API_COLLECTION_NOT_FOUND.getErrorKey(), "APP_API_COLLECTION_NOT_FOUND"),
        Map.entry(APP_API_COLLECTION_NOT_BINDABLE.getErrorKey(), "APP_API_COLLECTION_NOT_BINDABLE"),
        Map.entry(APP_API_COLLECTION_UNAVAILABLE.getErrorKey(), "APP_API_COLLECTION_UNAVAILABLE"),
        Map.entry(APP_API_ENDPOINT_NOT_FOUND.getErrorKey(), "APP_API_ENDPOINT_NOT_FOUND"),
        Map.entry(APP_EXECUTION_FAILED.getErrorKey(), "APP_EXECUTION_FAILED"));

    private AppErrorType(int errorKey) {
        super(App.class, errorKey);
    }

    /**
     * Returns the name of the constant carrying {@code errorKey}, or {@code null} when no constant does. The generic
     * error type carries only the numeric key, so this is how a response body names the error.
     */
    public static @Nullable String nameOf(int errorKey) {
        return NAMES.get(errorKey);
    }
}
