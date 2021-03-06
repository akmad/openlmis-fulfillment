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

package org.openlmis.fulfillment.domain;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.openlmis.fulfillment.testutils.ShipmentLineItemDataBuilder;
import org.openlmis.fulfillment.testutils.ToStringTestUtils;
import java.util.UUID;

public class ShipmentLineItemTest {

  private UUID lineItemId = UUID.randomUUID();
  private UUID orderableId = UUID.randomUUID();
  private UUID lotId = UUID.randomUUID();
  private Long quantityShipped = 15L;

  @Test
  public void shouldExportValues() {
    DummyShipmentLineItemDto exporter = new DummyShipmentLineItemDto();

    ShipmentLineItem shipmentLineItem = createShipmentLineItem();
    shipmentLineItem.export(exporter);

    assertEquals(lineItemId, exporter.getId());
    assertEquals(orderableId, exporter.getOrderableId());
    assertEquals(lotId, exporter.getLotId());
    assertEquals(quantityShipped, exporter.getQuantityShipped());
  }

  @Test
  public void shouldImplementToString() {
    ShipmentLineItem shipmentLineItem = new ShipmentLineItemDataBuilder().build();
    ToStringTestUtils.verify(ShipmentLineItem.class, shipmentLineItem);
  }

  @Test
  public void shouldReturnTrueIfLineItemHasSomethingToShipped() {
    assertThat(createShipmentLineItem().isShipped(), is(true));
  }

  @Test
  public void shouldReturnFalseIfLineItemHasNothingToShipped() {
    assertThat(
        new ShipmentLineItemDataBuilder().withQuantityShipped(0L).build().isShipped(),
        is(false)
    );
    assertThat(
        new ShipmentLineItemDataBuilder().withQuantityShipped(null).build().isShipped(),
        is(false)
    );
  }

  private ShipmentLineItem createShipmentLineItem() {
    return new ShipmentLineItemDataBuilder()
        .withId(lineItemId)
        .withLotId(lotId)
        .withOrderableId(orderableId)
        .withQuantityShipped(quantityShipped)
        .build();
  }

}
