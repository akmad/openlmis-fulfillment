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

package org.openlmis.fulfillment.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;

@Component
public class DateHelper {

  @Autowired
  private Clock clock;

  /**
   * Obtains the current date time from the system clock in the system time zone.
   * @return the current date time using the system clock
   */
  public ZonedDateTime getCurrentDateTimeWithSystemZone() {
    return ZonedDateTime.now(clock);
  }

  public LocalDate getCurrentDate() {
    return LocalDate.now(clock);
  }

}
