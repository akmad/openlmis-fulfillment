/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2017 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *  
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 * See the GNU Affero General Public License for more details. You should have received a copy of
 * the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org. 
 */

package org.openlmis.fulfillment.web;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.OrderDataBuilder;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderNumberConfiguration;
import org.openlmis.fulfillment.domain.ProofOfDelivery;
import org.openlmis.fulfillment.domain.UpdateDetails;
import org.openlmis.fulfillment.extension.ExtensionManager;
import org.openlmis.fulfillment.extension.point.OrderNumberGenerator;
import org.openlmis.fulfillment.repository.OrderNumberConfigurationRepository;
import org.openlmis.fulfillment.repository.ProofOfDeliveryRepository;
import org.openlmis.fulfillment.service.ExporterBuilder;
import org.openlmis.fulfillment.service.OrderService;
import org.openlmis.fulfillment.service.referencedata.FacilityReferenceDataService;
import org.openlmis.fulfillment.service.referencedata.PeriodReferenceDataService;
import org.openlmis.fulfillment.service.referencedata.ProgramDto;
import org.openlmis.fulfillment.service.referencedata.ProgramReferenceDataService;
import org.openlmis.fulfillment.service.referencedata.UserReferenceDataService;
import org.openlmis.fulfillment.testutils.UpdateDetailsDataBuilder;
import org.openlmis.fulfillment.util.AuthenticationHelper;
import org.openlmis.fulfillment.util.DateHelper;
import org.openlmis.fulfillment.web.util.OrderDto;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class OrderControllerTest {

  @InjectMocks
  private OrderController orderController;

  @Mock
  private AuthenticationHelper authenticationHelper;

  @Mock
  private DateHelper dateHelper;

  @Mock
  private ProgramReferenceDataService programReferenceDataService;

  @Mock
  private OrderService orderService;

  @Mock
  private OrderNumberGenerator orderNumberGenerator;

  @Mock
  private OrderNumberConfigurationRepository orderNumberConfigurationRepository;

  @Mock
  private ExtensionManager extensionManager;

  @Mock
  private ProofOfDeliveryRepository proofOfDeliveryRepository;

  @Mock
  private ProofOfDelivery proofOfDelivery;

  @Mock
  private ExporterBuilder exporterBuilder;

  @Mock
  private FacilityReferenceDataService facilities;

  @Mock
  private ProgramReferenceDataService programs;

  @Mock
  private PeriodReferenceDataService periods;

  @Mock
  private UserReferenceDataService users;

  @Mock
  private ProgramDto programDto;

  private static final String ORDER_NUMBER = "orderNumber";
  private static final String SERVICE_URL = "localhost";

  private OAuth2Authentication authentication = mock(OAuth2Authentication.class);
  private OrderDto orderDto = new OrderDto();

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    when(dateHelper.getCurrentDateTimeWithSystemZone()).thenReturn(
        ZonedDateTime.of(2015, 5, 7, 10, 5, 20, 500, ZoneId.systemDefault()));

    OrderNumberConfiguration orderNumberConfiguration =
        new OrderNumberConfiguration("prefix", false, false, false);

    UpdateDetails updateDetails = new UpdateDetailsDataBuilder()
        .withUpdaterId(UUID.fromString("35316636-6264-6331-2d34-3933322d3462"))
        .withUpdatedDate(ZonedDateTime.now())
        .build();

    Order order = new OrderDataBuilder().withUpdateDetails(updateDetails).build();

    when(orderService.save(any())).thenReturn(order);
    when(programReferenceDataService.findOne(any())).thenReturn(programDto);
    when(authentication.isClientOnly()).thenReturn(true);
    when(orderNumberConfigurationRepository.findAll()).thenReturn(Lists.newArrayList(
        orderNumberConfiguration));
    when(extensionManager.getExtension(any(), any())).thenReturn(orderNumberGenerator);
    when(orderNumberGenerator.generate(any())).thenReturn(ORDER_NUMBER);
    when(proofOfDeliveryRepository.save(any(ProofOfDelivery.class))).thenReturn(proofOfDelivery);

    orderDto.setUpdaterId(UUID.fromString("35316636-6264-6331-2d34-3933322d3462"));

    ReflectionTestUtils.setField(exporterBuilder, "serviceUrl", SERVICE_URL);
    ReflectionTestUtils.setField(exporterBuilder, "facilities", facilities);
    ReflectionTestUtils.setField(exporterBuilder, "programs", programs);
    ReflectionTestUtils.setField(exporterBuilder, "periods", periods);
    ReflectionTestUtils.setField(exporterBuilder, "users", users);
  }

  @Test
  public void shouldGetLastUpdaterFromDtoIfCurrentUserIsNull() {
    when(authenticationHelper.getCurrentUser()).thenReturn(null);
    doCallRealMethod().when(exporterBuilder).export(any(Order.class), any());

    OrderDto savedOrder = orderController.createOrder(orderDto, authentication);

    assertEquals(savedOrder.getLastUpdater().getId(), orderDto.getLastUpdater().getId());
  }

}