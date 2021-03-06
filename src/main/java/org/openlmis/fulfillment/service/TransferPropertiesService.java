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

package org.openlmis.fulfillment.service;

import org.apache.commons.lang.NullArgumentException;
import org.openlmis.fulfillment.domain.TransferProperties;
import org.openlmis.fulfillment.repository.TransferPropertiesRepository;
import org.openlmis.fulfillment.service.referencedata.FacilityReferenceDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TransferPropertiesService {
  @Autowired
  private TransferPropertiesRepository transferPropertiesRepository;

  @Autowired
  private FacilityReferenceDataService facilityReferenceDataService;

  /**
   * Retrieves TransferProperties for given facility.
   *
   * @param facilityId id of facility.
   * @return TransferProperties entity with matching facility.
   */
  public TransferProperties getByFacility(UUID facilityId) {
    if (facilityId == null) {
      throw new NullArgumentException("facilityId");
    }

    return transferPropertiesRepository.findFirstByFacilityId(facilityId);
  }

  /**
   * Saves an entity of TransferProperties, first checking if it's facility exists.
   *
   * @param setting an instance of TransferProperties to be saved.
   * @return saved instance of TransferProperties.
   */
  public TransferProperties save(TransferProperties setting) {
    if (facilityReferenceDataService.findOne(setting.getFacilityId()) == null) {
      throw new IllegalArgumentException("Facility with given ID does not exist.");
    }

    TransferProperties existent = getByFacility(setting.getFacilityId());
    if (existent != null && existent.getId() != setting.getId()) {
      throw new DuplicateTransferPropertiesException();
    }

    return transferPropertiesRepository.save(setting);
  }
}
