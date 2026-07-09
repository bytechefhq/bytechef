/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.billing.web.rest.mapper;

import com.bytechef.platform.billing.dto.BillingSubscriptionDTO;
import com.bytechef.platform.billing.web.rest.mapper.config.PlatformBillingMapperSpringConfig;
import com.bytechef.platform.billing.web.rest.model.BillingSubscriptionModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Matija Petanjek
 */
@Mapper(config = PlatformBillingMapperSpringConfig.class)
public interface BillingMapper extends Converter<BillingSubscriptionDTO, BillingSubscriptionModel> {

    @Override
    @Mapping(source = "subscription.planName", target = "planName")
    @Mapping(target = "status", expression = "java(dto.subscription().getStatus().name())")
    @Mapping(source = "subscription.taskLimit", target = "taskLimit")
    @Mapping(source = "subscription.currentPeriodEnd", target = "currentPeriodEnd")
    @Mapping(source = "subscription.cancelAtPeriodEnd", target = "cancelAtPeriodEnd")
    @Mapping(source = "subscription.scheduledPlanName", target = "scheduledPlanName")
    @Mapping(source = "tasksUsed", target = "tasksUsed")
    BillingSubscriptionModel convert(BillingSubscriptionDTO dto);
}
