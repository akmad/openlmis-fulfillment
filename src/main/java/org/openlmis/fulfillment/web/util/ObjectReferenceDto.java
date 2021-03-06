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

import static org.apache.commons.lang3.StringUtils.joinWith;
import static org.openlmis.fulfillment.service.ResourceNames.BASE_PATH;
import static org.openlmis.fulfillment.service.ResourceNames.SEPARATOR;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.openlmis.fulfillment.domain.Identifiable;

@EqualsAndHashCode
@ToString
@JsonInclude(Include.NON_NULL)
public class ObjectReferenceDto implements Identifiable {

  @Getter
  private final UUID id;

  @Getter
  private final String href;

  protected ObjectReferenceDto() {
    this(null);
  }

  public ObjectReferenceDto(UUID id) {
    this(id, null);
  }

  /**
   * Returns new object reference.
   *
   * @param id   object id
   */
  public ObjectReferenceDto(UUID id, String serviceUrl, String resourceName) {
    this(id, joinWith(SEPARATOR, serviceUrl + BASE_PATH, resourceName, id));
  }

  private ObjectReferenceDto(UUID id, String href) {
    this.id = id;
    this.href = href;
  }

  public static ObjectReferenceDto create(UUID id, String serviceUrl, String resourceName) {
    return new ObjectReferenceDto(id, serviceUrl, resourceName);
  }

}
