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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.service.referencedata.RightDto;
import org.openlmis.fulfillment.service.referencedata.RightReferenceDataService;
import org.openlmis.fulfillment.service.referencedata.UserDto;
import org.openlmis.fulfillment.service.referencedata.UserReferenceDataService;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
public class AuthenticationHelperTest {

  @Mock
  private UserReferenceDataService userReferenceDataService;

  @Mock
  private RightReferenceDataService rightReferenceDataService;

  @InjectMocks
  private AuthenticationHelper authenticationHelper;

  private OAuth2Authentication authentication = mock(OAuth2Authentication.class);

  private UUID userId = UUID.randomUUID();

  @Before
  public void setUp() {
    when(authentication.getPrincipal()).thenReturn(userId);

    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);

    SecurityContextHolder.setContext(securityContext);
  }

  @Test
  public void shouldReturnUser() {
    // given
    UserDto userMock = mock(UserDto.class);
    when(authentication.isClientOnly()).thenReturn(false);
    when(userReferenceDataService.findOne(userId)).thenReturn(userMock);

    // when
    UserDto user = authenticationHelper.getCurrentUser();

    // then
    assertEquals(userMock, user);
  }

  @Test(expected = AuthenticationException.class)
  public void shouldThrowExceptionIfUserDoesNotExist() {
    // given
    when(authentication.isClientOnly()).thenReturn(false);
    when(userReferenceDataService.findOne(any(UUID.class))).thenReturn(null);

    // when
    authenticationHelper.getCurrentUser();
  }

  @Test
  public void shouldNotThrowExceptionIfAuthenticationIsClientOnly() {
    // given
    when(authentication.isClientOnly()).thenReturn(true);
    when(userReferenceDataService.findOne(any(UUID.class))).thenReturn(null);

    // when
    UserDto user = authenticationHelper.getCurrentUser();

    //then
    assertEquals(null, user);
    verify(authentication, never()).getPrincipal();
  }

  @Test
  public void shouldReturnRight() throws Exception {
    // given
    RightDto right = mock(RightDto.class);
    when(rightReferenceDataService.findRight("rightName")).thenReturn(right);

    // when
    RightDto dto = authenticationHelper.getRight("rightName");

    // then
    assertNotNull(dto);
    assertThat(dto, is(right));
  }

  @Test(expected = AuthenticationException.class)
  public void shouldThrowExceptionIfRightDoesNotExist() {
    // given
    when(rightReferenceDataService.findRight(anyString())).thenReturn(null);

    // when
    authenticationHelper.getRight("rightName");
  }
}
