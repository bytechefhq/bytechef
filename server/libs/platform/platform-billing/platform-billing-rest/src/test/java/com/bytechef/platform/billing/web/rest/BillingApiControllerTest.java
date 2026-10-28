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

package com.bytechef.platform.billing.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.billing.domain.BillingSubscription;
import com.bytechef.platform.billing.dto.BillingSubscriptionDTO;
import com.bytechef.platform.billing.facade.BillingSubscriptionFacade;
import com.bytechef.platform.billing.web.rest.mapper.BillingMapperImpl;
import com.bytechef.platform.billing.web.rest.model.BillingSubscriptionModel;
import com.bytechef.platform.billing.web.rest.model.CheckoutSessionModel;
import com.bytechef.platform.billing.web.rest.model.JobExecutionsPackageCheckoutRequestModel;
import com.bytechef.web.rest.mapper.DateTimeMapper;
import java.lang.reflect.Field;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;

/**
 * @author Matija Petanjek
 */
class BillingApiControllerTest {

    private final BillingSubscriptionFacade billingSubscriptionFacade = mock(BillingSubscriptionFacade.class);
    private final ConversionService conversionService = mock(ConversionService.class);

    private final BillingApiController controller =
        new BillingApiController(billingSubscriptionFacade, conversionService);

    @Test
    void testCreateJobExecutionsPackageCheckoutSessionReturnsUrl() {
        when(billingSubscriptionFacade.createJobExecutionsPackageCheckoutSession(3)).thenReturn("https://checkout");

        ResponseEntity<CheckoutSessionModel> response = controller.createJobExecutionsPackageCheckoutSession(
            new JobExecutionsPackageCheckoutRequestModel(3));

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()
            .getCheckoutUrl()).isEqualTo("https://checkout");
    }

    @Test
    void testGetCurrentSubscriptionMapsUnitCounters() throws ReflectiveOperationException {
        BillingSubscription subscription = new BillingSubscription();

        subscription.setStatus(BillingSubscription.Status.ACTIVE);
        subscription.setSubscriptionModel(BillingSubscription.SubscriptionModel.PREPAID);
        subscription.setPlanUnitTotal(1000);
        subscription.setPlanUnitUsed(10);
        subscription.setExtraUnitTotal(500);
        subscription.setExtraUnitUsed(5);

        BillingSubscriptionDTO dto = new BillingSubscriptionDTO(subscription, 0, null);

        BillingMapperImpl billingMapper = new BillingMapperImpl();

        Field dateTimeMapperField = BillingMapperImpl.class.getDeclaredField("dateTimeMapper");

        dateTimeMapperField.setAccessible(true);
        dateTimeMapperField.set(billingMapper, mock(DateTimeMapper.class));

        BillingSubscriptionModel convertedModel = billingMapper.convert(dto);

        when(billingSubscriptionFacade.fetchCurrentSubscription()).thenReturn(Optional.of(dto));
        when(conversionService.convert(dto, BillingSubscriptionModel.class)).thenReturn(convertedModel);

        ResponseEntity<BillingSubscriptionModel> response = controller.getCurrentSubscription();

        BillingSubscriptionModel model = response.getBody();

        assertThat(model).isNotNull();
        assertThat(model.getSubscriptionModel()).isEqualTo(BillingSubscriptionModel.SubscriptionModelEnum.PREPAID);
        assertThat(model.getPlanUnitTotal()).isEqualTo(1000);
        assertThat(model.getPlanUnitUsed()).isEqualTo(10);
        assertThat(model.getExtraUnitTotal()).isEqualTo(500);
        assertThat(model.getExtraUnitUsed()).isEqualTo(5);
    }
}
