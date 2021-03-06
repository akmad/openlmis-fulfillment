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

package org.openlmis.fulfillment.web.util;

import java.util.UUID;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Test;
import org.openlmis.fulfillment.ProofOfDeliveryDataBuilder;
import org.openlmis.fulfillment.domain.ProofOfDelivery;
import org.openlmis.fulfillment.testutils.ToStringTestUtils;

public class ProofOfDeliveryDtoTest {

  @Test
  public void equalsContract() {
    EqualsVerifier
        .forClass(ProofOfDeliveryDto.class)
        .withRedefinedSuperclass()
        .withPrefabValues(
            ShipmentObjectReferenceDto.class,
            new ShipmentObjectReferenceDto(UUID.randomUUID()),
            new ShipmentObjectReferenceDto(UUID.randomUUID()))
        .suppress(Warning.NONFINAL_FIELDS)
        .withIgnoredFields("serviceUrl")
        .verify();
  }

  @Test
  public void shouldImplementToString() {
    ProofOfDelivery domain = new ProofOfDeliveryDataBuilder().build();
    ProofOfDeliveryDto dto = new ProofOfDeliveryDto();
    domain.export(dto);

    ToStringTestUtils.verify(ProofOfDeliveryDto.class, dto);
  }

}
