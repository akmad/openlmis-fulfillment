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

package org.openlmis.fulfillment.web.shipment;

import org.openlmis.fulfillment.domain.Shipment;
import org.openlmis.fulfillment.domain.ShipmentLineItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ShipmentDtoBuilder {

  @Value("${service.url}")
  private String serviceUrl;

  /**
   * Create a new list of {@link ShipmentDto} based on data
   * from {@link Shipment}.
   *
   * @param shipments collection used to create {@link ShipmentDto} list (can be {@code null})
   * @return new list of {@link ShipmentDto}. Empty list if passed argument is {@code null}.
   */
  public List<ShipmentDto> build(Collection<Shipment> shipments) {
    if (null == shipments) {
      return Collections.emptyList();
    }
    return shipments.stream()
        .map(this::export)
        .collect(Collectors.toList());
  }

  /**
   * Create a new instance of {@link ShipmentDto} based on data
   * from {@link org.openlmis.fulfillment.domain.Shipment}.
   *
   * @param shipment instance used to create {@link ShipmentDto} (can be {@code null})
   * @return new instance of {@link ShipmentDto}. {@code null} if passed argument is {@code null}.
   */
  public ShipmentDto build(Shipment shipment) {
    if (null == shipment) {
      return null;
    }
    return export(shipment);
  }

  private ShipmentDto export(Shipment shipment) {
    ShipmentDto dto = new ShipmentDto();
    dto.setServiceUrl(serviceUrl);
    shipment.export(dto);
    dto.setLineItems(exportToDtos(shipment.getLineItems()));

    return dto;
  }

  private List<ShipmentLineItemDto> exportToDtos(List<ShipmentLineItem> lineItems) {
    List<ShipmentLineItemDto> lineItemDtos = new ArrayList<>(lineItems.size());
    lineItems.forEach(l -> lineItemDtos.add(exportToDto(l)));
    return lineItemDtos;
  }

  private ShipmentLineItemDto exportToDto(ShipmentLineItem lineItem) {
    ShipmentLineItemDto lineItemDto = new ShipmentLineItemDto();
    lineItemDto.setServiceUrl(serviceUrl);
    lineItem.export(lineItemDto);
    return lineItemDto;
  }

}
