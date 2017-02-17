package org.openlmis.fulfillment.service;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.openlmis.fulfillment.i18n.MessageKeys.ERROR_ORDER_INVALID_STATUS;
import static org.openlmis.fulfillment.service.OrderSearchParams.builder;

import com.google.common.collect.Sets;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openlmis.fulfillment.domain.OrderStatus;
import org.openlmis.fulfillment.web.ValidationException;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OrderSearchParamsTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void shouldConvertStatusToNullIfStatusFieldIsBlank() throws Exception {
    assertThat(builder().status(null).build().getStatusAsEnum(), is(nullValue()));
    assertThat(builder().status(Sets.newHashSet()).build().getStatusAsEnum(), is(nullValue()));
  }

  @Test
  public void shouldConvertStatusToCorrectEnumValue() throws Exception {
    Set<String> status = Stream.of(OrderStatus.values())
        .map(Enum::toString)
        .collect(Collectors.toSet());

    assertThat(
        builder().status(status).build().getStatusAsEnum(),
        is(equalTo(EnumSet.allOf(OrderStatus.class)))
    );
  }

  @Test
  public void shouldThrowExceptionIfStatusIsIncorrect() throws Exception {
    exception.expect(ValidationException.class);
    exception.expect(hasProperty("messageKey", equalTo(ERROR_ORDER_INVALID_STATUS)));

    builder().status(Sets.newHashSet("ala has a cat")).build().getStatusAsEnum();
  }
}