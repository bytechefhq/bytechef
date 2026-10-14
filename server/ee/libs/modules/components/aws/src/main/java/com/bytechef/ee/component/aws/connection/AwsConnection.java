/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.aws.connection;

import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.ee.component.aws.constant.AwsConstants.ACCESS_KEY_ID;
import static com.bytechef.ee.component.aws.constant.AwsConstants.EXTERNAL_ID;
import static com.bytechef.ee.component.aws.constant.AwsConstants.REGION;
import static com.bytechef.ee.component.aws.constant.AwsConstants.ROLE_ARN;
import static com.bytechef.ee.component.aws.constant.AwsConstants.SECRET_ACCESS_KEY;

import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.Property.ControlType;

/**
 * @author Ivica Cardic
 * @version ee
 */
public final class AwsConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .authorizations(
            authorization(Authorization.AuthorizationType.CUSTOM)
                .title("Access Keys")
                .properties(
                    string(ACCESS_KEY_ID)
                        .label("Access Key ID")
                        .description("The IAM access key ByteChef signs its AWS calls with.")
                        .required(true),
                    string(SECRET_ACCESS_KEY)
                        .label("Secret Access Key")
                        .controlType(ControlType.PASSWORD)
                        .required(true),
                    string(REGION)
                        .label("Region")
                        .description("The AWS region of the job queue and the staging bucket, for example eu-west-1.")
                        .required(true),
                    string(ROLE_ARN)
                        .label("Role ARN")
                        .description(
                            "A role to assume with the access keys before every run. Its maximum session duration " +
                                "must cover the run's completion budget plus 10 minutes.")
                        .required(false),
                    string(EXTERNAL_ID)
                        .label("External ID")
                        .description("The external ID the role's trust policy expects, if any.")
                        .required(false)));

    private AwsConnection() {
    }
}
