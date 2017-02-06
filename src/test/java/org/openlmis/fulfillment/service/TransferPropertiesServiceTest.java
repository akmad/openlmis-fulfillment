package org.openlmis.fulfillment.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.domain.FtpTransferProperties;
import org.openlmis.fulfillment.domain.TransferProperties;
import org.openlmis.fulfillment.service.referencedata.FacilityDto;
import org.openlmis.fulfillment.service.referencedata.FacilityReferenceDataService;
import org.openlmis.fulfillment.repository.TransferPropertiesRepository;

import java.util.Random;
import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
public class TransferPropertiesServiceTest {
  @Mock
  private TransferPropertiesRepository transferPropertiesRepository;

  @Mock
  private FacilityReferenceDataService facilityReferenceDataService;

  @InjectMocks
  private TransferPropertiesService transferPropertiesService;

  @Test
  public void shouldSaveSetting() {
    // given
    final TransferProperties properties = randomSetting();
    final FacilityDto facility = mock(FacilityDto.class);

    when(facility.getId()).thenReturn(UUID.randomUUID());
    when(facilityReferenceDataService.findOne(any(UUID.class))).thenReturn(facility);
    when(transferPropertiesRepository.findFirstByFacilityId(any(UUID.class)))
        .thenReturn(null);

    // when
    transferPropertiesService.save(properties);

    // then
    verify(transferPropertiesRepository, atLeastOnce()).save(properties);
  }

  @Test(expected = DuplicateTransferPropertiesException.class)
  public void shouldNotSaveSettingIfFacilityIdDuplicated() {
    // given
    final TransferProperties properties = randomSetting();
    final TransferProperties duplicate = randomSetting();
    FacilityDto facility = mock(FacilityDto.class);

    when(facility.getId()).thenReturn(UUID.randomUUID());
    when(facilityReferenceDataService.findOne(any(UUID.class))).thenReturn(facility);
    when(transferPropertiesRepository.findFirstByFacilityId(any(UUID.class)))
        .thenReturn(duplicate);

    // when
    transferPropertiesService.save(properties);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldNotSaveSettingIfFacilityDoesNotExist() {
    // given
    TransferProperties properties = randomSetting();

    when(facilityReferenceDataService.findOne(any(UUID.class))).thenReturn(null);
    when(transferPropertiesRepository.findFirstByFacilityId(any(UUID.class)))
        .thenReturn(null);

    // when
    transferPropertiesService.save(properties);
  }

  @Test
  public void shouldGetByFacility() {
    // given
    TransferProperties properties = randomSetting();

    when(transferPropertiesRepository.findFirstByFacilityId(any(UUID.class)))
        .thenReturn(properties);

    // when
    TransferProperties result = transferPropertiesService.getByFacility(properties.getFacilityId());

    // then
    assertEquals(result.getId(), properties.getId());
  }

  @Test
  public void shouldNotGetByFacilityIfFacilityDoesNotExist() {
    // given
    TransferProperties properties = randomSetting();

    when(transferPropertiesRepository.findFirstByFacilityId(any(UUID.class)))
        .thenReturn(null);

    // when
    TransferProperties result = transferPropertiesService.getByFacility(properties.getFacilityId());

    // then
    assertNull(result);
  }

  private FtpTransferProperties randomSetting() {
    FtpTransferProperties properties = new FtpTransferProperties();
    properties.setId(UUID.randomUUID());
    properties.setFacilityId(UUID.randomUUID());
    properties.setServerHost(RandomStringUtils.random(10));
    properties.setServerPort(new Random().nextInt(1000));
    properties.setRemoteDirectory(RandomStringUtils.random(10));
    properties.setUsername(RandomStringUtils.random(10));
    properties.setPassword(RandomStringUtils.random(10));

    return properties;
  }
}
