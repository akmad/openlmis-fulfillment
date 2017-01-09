package org.openlmis.fulfillment.web;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.openlmis.fulfillment.i18n.MessageKeys.ERROR_IO;
import static org.openlmis.fulfillment.i18n.MessageKeys.ERROR_ORDER_RETRY_INVALID_STATUS;
import static org.openlmis.fulfillment.util.ConfigurationSettingKeys.FULFILLMENT_EMAIL_NOREPLY;
import static org.openlmis.fulfillment.util.ConfigurationSettingKeys.FULFILLMENT_EMAIL_ORDER_CREATION_BODY;
import static org.openlmis.fulfillment.util.ConfigurationSettingKeys.FULFILLMENT_EMAIL_ORDER_CREATION_SUBJECT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderLineItem;
import org.openlmis.fulfillment.domain.OrderStatus;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.fulfillment.service.ConfigurationSettingException;
import org.openlmis.fulfillment.service.ConfigurationSettingService;
import org.openlmis.fulfillment.service.OrderFileStorage;
import org.openlmis.fulfillment.service.OrderFtpSender;
import org.openlmis.fulfillment.service.OrderStorageException;
import org.openlmis.fulfillment.service.notification.NotificationService;
import org.openlmis.fulfillment.web.util.OrderDto;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;

import guru.nidi.ramltester.junit.RamlMatchers;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@SuppressWarnings({"PMD.TooManyMethods", "PMD.UnusedPrivateField"})
public class OrderControllerIntegrationTest extends BaseWebIntegrationTest {

  private static final String RESOURCE_URL = "/api/orders";
  private static final String SEARCH_URL = RESOURCE_URL + "/search";

  private static final String ID_URL = RESOURCE_URL + "/{id}";
  private static final String ID_EXPORT = ID_URL + "/export";
  private static final String ID_RETRY = ID_URL + "/retry";
  private static final String ID_FINALIZE = ID_URL + "/finalize";
  private static final String API_ORDERS_ID_PRINT = ID_URL + "/print";

  private static final String ACCESS_TOKEN = "access_token";
  private static final String REQUESTING_FACILITY = "requestingFacility";
  private static final String SUPPLYING_FACILITY = "supplyingFacility";
  private static final String PROGRAM = "program";
  private static final String NUMBER = "10.90";
  private static final String FORMAT = "format";

  private static final UUID ID = UUID.fromString("1752b457-0a4b-4de0-bf94-5a6a8002427e");

  private UUID facility = UUID.randomUUID();
  private UUID facility1 = UUID.randomUUID();
  private UUID facility2 = UUID.randomUUID();
  private UUID program1 = UUID.randomUUID();
  private UUID program2 = UUID.randomUUID();
  private UUID period1 = UUID.randomUUID();
  private UUID period2 = UUID.randomUUID();
  private UUID product1 = UUID.randomUUID();
  private UUID product2 = UUID.randomUUID();

  @MockBean
  private OrderRepository orderRepository;

  @MockBean
  private OrderFileStorage orderStorage;

  @MockBean
  private OrderFtpSender orderFtpSender;

  @MockBean
  private NotificationService notificationService;

  @MockBean
  private ConfigurationSettingService configurationSettingService;

  private Order firstOrder = new Order();
  private Order secondOrder = new Order();
  private Order thirdOrder = new Order();

  private OrderDto firstOrderDto;

  @Before
  public void setUp() throws ConfigurationSettingException {
    this.setUpBootstrapData();

    firstOrder = addOrder(UUID.randomUUID(), facility, period1, "orderCode", UUID.randomUUID(),
        INITIAL_USER_ID, facility, facility, facility, OrderStatus.ORDERED,
        new BigDecimal("1.29"));

    secondOrder = addOrder(UUID.randomUUID(), facility2, period1, "O2", program1, INITIAL_USER_ID,
        facility2, facility2, facility1, OrderStatus.RECEIVED, new BigDecimal(100));

    thirdOrder = addOrder(UUID.randomUUID(), facility2, period2, "O3", program2, INITIAL_USER_ID,
        facility2, facility2, facility1, OrderStatus.RECEIVED, new BigDecimal(200));

    addOrderLineItem(secondOrder, product1, 35L, 50L);

    addOrderLineItem(secondOrder, product2, 10L, 15L);

    addOrderLineItem(thirdOrder, product1, 50L, 50L);

    addOrderLineItem(thirdOrder, product2, 5L, 10L);

    OrderLineItem orderLineItem = addOrderLineItem(firstOrder, product1, 35L, 50L);

    List<OrderLineItem> orderLineItems = new ArrayList<>();
    orderLineItems.add(orderLineItem);
    firstOrder.setOrderLineItems(orderLineItems);
    firstOrder.setExternalId(secondOrder.getExternalId());

    firstOrderDto =  OrderDto.newInstance(firstOrder);

    given(orderRepository.findAll()).willReturn(
        Lists.newArrayList(firstOrder, secondOrder, thirdOrder)
    );

    given(orderRepository.save(any(Order.class)))
        .willAnswer(new SaveAnswer<Order>() {

          @Override
          void extraSteps(Order obj) {
            obj.setCreatedDate(LocalDateTime.now());
          }

        });

    given(configurationSettingService.getStringValue(FULFILLMENT_EMAIL_NOREPLY))
        .willReturn("noreply@openlmis.org");
    given(configurationSettingService.getStringValue(FULFILLMENT_EMAIL_ORDER_CREATION_SUBJECT))
        .willReturn("New order");
    given(configurationSettingService.getStringValue(FULFILLMENT_EMAIL_ORDER_CREATION_BODY))
        .willReturn("Create an order: {id} with status: {status}");
  }

  private Order addOrder(UUID requisition, UUID facility, UUID processingPeriod,
                         String orderCode, UUID program, UUID user,
                         UUID requestingFacility, UUID receivingFacility,
                         UUID supplyingFacility, OrderStatus orderStatus, BigDecimal cost) {
    Order order = new Order();
    order.setId(UUID.randomUUID());
    order.setExternalId(requisition);
    order.setEmergency(false);
    order.setFacilityId(facility);
    order.setProcessingPeriodId(processingPeriod);
    order.setOrderCode(orderCode);
    order.setQuotedCost(cost);
    order.setStatus(orderStatus);
    order.setProgramId(program);
    order.setCreatedDate(LocalDateTime.now());
    order.setCreatedById(user);
    order.setRequestingFacilityId(requestingFacility);
    order.setReceivingFacilityId(receivingFacility);
    order.setSupplyingFacilityId(supplyingFacility);
    order.setOrderLineItems(new ArrayList<>());

    given(orderRepository.findOne(order.getId())).willReturn(order);
    given(orderRepository.exists(order.getId())).willReturn(true);

    return order;
  }

  private OrderLineItem addOrderLineItem(Order order, UUID product, Long filledQuantity,
                                         Long orderedQuantity) {
    OrderLineItem orderLineItem = new OrderLineItem();
    orderLineItem.setId(UUID.randomUUID());
    orderLineItem.setOrder(order);
    orderLineItem.setOrderableProductId(product);
    orderLineItem.setOrderedQuantity(orderedQuantity);
    orderLineItem.setFilledQuantity(filledQuantity);
    orderLineItem.setApprovedQuantity(3L);

    order.getOrderLineItems().add(orderLineItem);

    return orderLineItem;
  }

  private void denyUserAllRights() {
    wireMockRule.stubFor(
        get(urlMatching(REFERENCEDATA_API_USERS + UUID_REGEX + "/hasRight.*"))
            .willReturn(aResponse()
                .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                .withBody("{ \"result\":\"false\" }"))
    );
  }

  @Test
  public void shouldFinalizeOrder() {
    firstOrder.setStatus(OrderStatus.ORDERED);

    restAssured.given()
        .queryParam(ACCESS_TOKEN, getToken())
        .pathParam("id", firstOrder.getId().toString())
        .contentType(APPLICATION_JSON_VALUE)
        .when()
        .put(ID_FINALIZE)
        .then()
        .statusCode(200);

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.responseChecks());

  }

  @Test
  public void shouldNotFinalizeIfWrongOrderStatus() {
    firstOrder.setStatus(OrderStatus.SHIPPED);

    restAssured.given()
        .queryParam(ACCESS_TOKEN, getToken())
        .pathParam("id", firstOrder.getId().toString())
        .contentType(APPLICATION_JSON_VALUE)
        .when()
        .put(ID_FINALIZE)
        .then()
        .statusCode(400);

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.responseChecks());
  }

  @Test
  public void shouldNotFinalizeIfOrderDoesNotExist() {
    UUID id = UUID.randomUUID();

    given(orderRepository.findOne(id)).willReturn(null);

    restAssured.given()
        .queryParam(ACCESS_TOKEN, getToken())
        .pathParam("id", id.toString())
        .contentType(APPLICATION_JSON_VALUE)
        .when()
        .put(ID_FINALIZE)
        .then()
        .statusCode(404);

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.responseChecks());
  }

  @Test
  public void shouldPrintOrderAsCsv() {
    String csvContent = restAssured.given()
        .queryParam(FORMAT, "csv")
        .queryParam(ACCESS_TOKEN, getToken())
        .pathParam("id", secondOrder.getId())
        .when()
        .get(API_ORDERS_ID_PRINT)
        .then()
        .statusCode(200)
        .extract().body().asString();

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
    assertTrue(csvContent.startsWith("productName,filledQuantity,orderedQuantity"));
  }

  @Test
  public void shouldPrintOrderAsPdf() {
    restAssured.given()
        .queryParam(FORMAT, "pdf")
        .queryParam(ACCESS_TOKEN, getToken())
        .pathParam("id", thirdOrder.getId().toString())
        .when()
        .get(API_ORDERS_ID_PRINT)
        .then()
        .statusCode(200);

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldReturnBadRequestIfThereIsNoOrderToPrint() {
    given(orderRepository.findOne(firstOrder.getId())).willReturn(null);

    restAssured.given()
        .queryParam(FORMAT, "pdf")
        .queryParam(ACCESS_TOKEN, getToken())
        .pathParam("id", firstOrder.getId().toString())
        .when()
        .get(API_ORDERS_ID_PRINT)
        .then()
        .statusCode(400);

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldFindBySupplyingFacility() {
    given(orderRepository.searchOrders(firstOrder.getSupplyingFacilityId(), null, null))
        .willReturn(Lists.newArrayList(firstOrder));

    OrderDto[] response = restAssured.given()
        .queryParam(SUPPLYING_FACILITY, firstOrder.getSupplyingFacilityId())
        .queryParam(ACCESS_TOKEN, getToken())
        .when()
        .get(SEARCH_URL)
        .then()
        .statusCode(200)
        .extract().as(OrderDto[].class);

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
    assertEquals(1, response.length);
    for (OrderDto order : response) {
      assertEquals(
          order.getSupplyingFacilityId(),
          firstOrder.getSupplyingFacilityId());
    }
  }

  @Test
  public void shouldFindBySupplyingFacilityAndRequestingFacility() {
    given(orderRepository.searchOrders(
        firstOrder.getSupplyingFacilityId(), firstOrder.getRequestingFacilityId(), null
    )).willReturn(Lists.newArrayList(firstOrder));

    OrderDto[] response = restAssured.given()
        .queryParam(SUPPLYING_FACILITY, firstOrder.getSupplyingFacilityId())
        .queryParam(REQUESTING_FACILITY, firstOrder.getRequestingFacilityId())
        .queryParam(ACCESS_TOKEN, getToken())
        .when()
        .get(SEARCH_URL)
        .then()
        .statusCode(200)
        .extract().as(OrderDto[].class);

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
    assertEquals(1, response.length);
    for (OrderDto order : response) {
      assertEquals(
          order.getSupplyingFacilityId(),
          firstOrder.getSupplyingFacilityId());
      assertEquals(
          order.getRequestingFacilityId(),
          firstOrder.getRequestingFacilityId());
    }
  }

  @Test
  public void shouldFindBySupplyingFacilityAndRequestingFacilityAndProgram() {
    given(orderRepository.searchOrders(
        firstOrder.getSupplyingFacilityId(), firstOrder.getRequestingFacilityId(),
        firstOrder.getProgramId()
    )).willReturn(Lists.newArrayList(firstOrder));

    OrderDto[] response = restAssured.given()
        .queryParam(SUPPLYING_FACILITY, firstOrder.getSupplyingFacilityId())
        .queryParam(REQUESTING_FACILITY, firstOrder.getRequestingFacilityId())
        .queryParam(PROGRAM, firstOrder.getProgramId())
        .queryParam(ACCESS_TOKEN, getToken())
        .when()
        .get(SEARCH_URL)
        .then()
        .statusCode(200)
        .extract().as(OrderDto[].class);

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
    assertEquals(1, response.length);
    for (OrderDto order : response) {
      assertEquals(
          order.getSupplyingFacilityId(),
          firstOrder.getSupplyingFacilityId());
      assertEquals(
          order.getRequestingFacilityId(),
          firstOrder.getRequestingFacilityId());
      assertEquals(
          order.getProgramId(),
          firstOrder.getProgramId());
    }
  }

  @Test
  public void shouldDeleteOrder() {
    restAssured.given()
        .queryParam(ACCESS_TOKEN, getToken())
        .contentType(APPLICATION_JSON_VALUE)
        .pathParam("id", firstOrder.getId())
        .when()
        .delete(ID_URL)
        .then()
        .statusCode(204);

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldNotDeleteNonexistentOrder() {
    given(orderRepository.findOne(firstOrder.getId())).willReturn(null);

    restAssured.given()
        .queryParam(ACCESS_TOKEN, getToken())
        .contentType(APPLICATION_JSON_VALUE)
        .pathParam("id", firstOrder.getId())
        .when()
        .delete(ID_URL)
        .then()
        .statusCode(404);

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldReturnConflictCodeForExistingOrder() {

    doThrow(DataIntegrityViolationException.class).when(orderRepository).delete(any(Order.class));

    restAssured.given()
        .queryParam(ACCESS_TOKEN, getToken())
        .contentType(APPLICATION_JSON_VALUE)
        .pathParam("id", firstOrder.getId())
        .when()
        .delete(ID_URL)
        .then()
        .statusCode(409);

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldCreateOrder() {
    firstOrder.getOrderLineItems().clear();
    firstOrder.setSupplyingFacilityId(UUID.fromString(FACILITY_ID));
    firstOrderDto =  OrderDto.newInstance(firstOrder);

    restAssured.given()
        .queryParam(ACCESS_TOKEN, getToken())
        .contentType(APPLICATION_JSON_VALUE)
        .body(firstOrderDto)
        .when()
        .post(RESOURCE_URL)
        .then()
        .statusCode(201);

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldUpdateOrder() {
    firstOrderDto.setQuotedCost(new BigDecimal(NUMBER));

    OrderDto response = restAssured.given()
        .queryParam(ACCESS_TOKEN, getToken())
        .contentType(APPLICATION_JSON_VALUE)
        .pathParam("id", firstOrder.getId())
        .body(firstOrderDto)
        .when()
        .put(ID_URL)
        .then()
        .statusCode(200)
        .extract().as(OrderDto.class);

    assertEquals(response.getQuotedCost(), new BigDecimal(NUMBER));
    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldCreateNewOrderIfDoesNotExist() {
    given(orderRepository.findOne(firstOrder.getId())).willReturn(null);

    firstOrderDto.setQuotedCost(new BigDecimal(NUMBER));

    OrderDto response = restAssured.given()
        .queryParam(ACCESS_TOKEN, getToken())
        .contentType(APPLICATION_JSON_VALUE)
        .pathParam("id", ID)
        .body(firstOrderDto)
        .when()
        .put(ID_URL)
        .then()
        .statusCode(200)
        .extract().as(OrderDto.class);

    assertEquals(response.getQuotedCost(), new BigDecimal(NUMBER));
    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldReturnConflictWhenUpdatingOrderCode() {
    given(orderRepository.save(any(Order.class))).willThrow(DataIntegrityViolationException.class);

    firstOrder.setSupplyingFacilityId(UUID.fromString(FACILITY_ID));

    given(orderRepository.findOne(firstOrder.getId())).willReturn(firstOrder);
    firstOrder.setOrderLineItems(null);
    firstOrderDto =  OrderDto.newInstance(firstOrder);

    restAssured.given()
        .queryParam(ACCESS_TOKEN, getToken())
        .contentType(APPLICATION_JSON_VALUE)
        .pathParam("id", ID)
        .body(firstOrderDto)
        .when()
        .put(ID_URL)
        .then()
        .statusCode(409);

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldGetAllOrders() {

    OrderDto[] response = restAssured.given()
        .queryParam(ACCESS_TOKEN, getToken())
        .contentType(APPLICATION_JSON_VALUE)
        .when()
        .get(RESOURCE_URL)
        .then()
        .statusCode(200)
        .extract().as(OrderDto[].class);

    Iterable<OrderDto> orders = Arrays.asList(response);
    assertTrue(orders.iterator().hasNext());
    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldGetChosenOrder() {

    OrderDto response = restAssured.given()
        .queryParam(ACCESS_TOKEN, getToken())
        .contentType(APPLICATION_JSON_VALUE)
        .pathParam("id", firstOrder.getId())
        .when()
        .get(ID_URL)
        .then()
        .statusCode(200)
        .extract().as(OrderDto.class);

    assertTrue(orderRepository.exists(response.getId()));
    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldNotGetNonexistentOrder() {
    given(orderRepository.findOne(firstOrder.getId())).willReturn(null);

    restAssured.given()
        .queryParam(ACCESS_TOKEN, getToken())
        .contentType(APPLICATION_JSON_VALUE)
        .pathParam("id", firstOrder.getId())
        .when()
        .get(ID_URL)
        .then()
        .statusCode(404);

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldReturnConflictForExistingOrderCode() {
    given(orderRepository.save(any(Order.class))).willThrow(DataIntegrityViolationException.class);

    firstOrder.getOrderLineItems().clear();
    firstOrder.setSupplyingFacilityId(UUID.fromString(FACILITY_ID));

    given(orderRepository.findOne(firstOrder.getId())).willReturn(firstOrder);
    firstOrder.setOrderLineItems(null);
    firstOrderDto =  OrderDto.newInstance(firstOrder);

    restAssured.given()
        .queryParam(ACCESS_TOKEN, getToken())
        .contentType(APPLICATION_JSON_VALUE)
        .body(firstOrderDto)
        .when()
        .post(RESOURCE_URL)
        .then()
        .statusCode(409);

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldReturnForbiddenWhenUserHasNoRightsToCreateOrder() {
    denyUserAllRights();

    restAssured.given()
        .queryParam(ACCESS_TOKEN, getToken())
        .contentType(APPLICATION_JSON_VALUE)
        .body(firstOrderDto)
        .when()
        .post(RESOURCE_URL)
        .then()
        .statusCode(403);

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldExportOrderIfTypeIsNotSpecified() {
    String csvContent = restAssured.given()
        .queryParam(ACCESS_TOKEN, getToken())
        .pathParam("id", secondOrder.getId())
        .when()
        .get(ID_EXPORT)
        .then()
        .statusCode(200)
        .extract().body().asString();

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
    assertTrue(csvContent.startsWith("Order number,Facility code,Product code,Product name,"
        + "Approved quantity,Period,Order date"));

    String orderDate = secondOrder.getCreatedDate().format(DateTimeFormatter.ofPattern("dd/MM/yy"));

    for (OrderLineItem lineItem : secondOrder.getOrderLineItems()) {
      String string = secondOrder.getOrderCode()
          + ",facilityCode,Product Code,Product Name," + lineItem.getApprovedQuantity()
          + ",03/16," + orderDate;
      assertThat(csvContent, containsString(string));
    }
  }

  @Test
  public void shouldExportOrderIfTypeIsCsv() {
    String csvContent = restAssured.given()
        .queryParam(ACCESS_TOKEN, getToken())
        .pathParam("id", secondOrder.getId())
        .queryParam("type", "csv")
        .when()
        .get(ID_EXPORT)
        .then()
        .statusCode(200)
        .extract().body().asString();

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
    assertTrue(csvContent.startsWith("Order number,Facility code,Product code,Product name,"
        + "Approved quantity,Period,Order date"));
  }

  @Test
  public void shouldNotExportOrderIfTypeIsDifferentThanCsv() {
    restAssured.given()
        .queryParam(ACCESS_TOKEN, getToken())
        .pathParam("id", secondOrder.getId())
        .queryParam("type", "pdf")
        .when()
        .get(ID_EXPORT)
        .then()
        .statusCode(400);

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldReturnBadRequestIfThereIsNoOrderToExport() {
    given(orderRepository.findOne(firstOrder.getId())).willReturn(null);

    restAssured.given()
        .queryParam(ACCESS_TOKEN, getToken())
        .pathParam("id", firstOrder.getId())
        .queryParam("type", "csv")
        .when()
        .get(ID_EXPORT)
        .then()
        .statusCode(404);

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldReturn404ForRetryEndpointWhenOrderDoesNotExist() {
    given(orderRepository.findOne(firstOrder.getId())).willReturn(null);

    restAssured.given()
        .queryParam(ACCESS_TOKEN, getToken())
        .pathParam("id", firstOrder.getId())
        .when()
        .get(ID_RETRY)
        .then()
        .statusCode(404);

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldNotAllowToRetryIfOrderHasIncorrectStatus() {
    firstOrder.setStatus(OrderStatus.READY_TO_PACK);

    given(orderRepository.findOne(firstOrder.getId())).willReturn(firstOrder);

    String message = restAssured.given()
        .queryParam(ACCESS_TOKEN, getToken())
        .pathParam("id", firstOrder.getId())
        .when()
        .get(ID_RETRY)
        .then()
        .statusCode(400)
        .extract()
        .path("messageKey");

    assertThat(message, equalTo(ERROR_ORDER_RETRY_INVALID_STATUS));
    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldReturnErrorMessageIfThereWillBeProblemWithRetry() throws OrderStorageException {
    firstOrder.setStatus(OrderStatus.TRANSFER_FAILED);

    given(orderRepository.findOne(firstOrder.getId())).willReturn(firstOrder);
    willThrow(new OrderStorageException(new IOException(), ERROR_IO, ""))
        .given(orderStorage).store(any(Order.class));

    String message = restAssured.given()
        .queryParam(ACCESS_TOKEN, getToken())
        .pathParam("id", firstOrder.getId())
        .when()
        .get(ID_RETRY)
        .then()
        .statusCode(400)
        .extract()
        .path("messageKey");

    assertThat(message, equalTo(ERROR_IO));
    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldAllowToDoManuallyRetry() {
    firstOrder.setStatus(OrderStatus.TRANSFER_FAILED);

    given(orderRepository.findOne(firstOrder.getId())).willReturn(firstOrder);

    restAssured.given()
        .queryParam(ACCESS_TOKEN, getToken())
        .pathParam("id", firstOrder.getId())
        .when()
        .get(ID_RETRY)
        .then()
        .statusCode(200);

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }
}
