package org.openlmis.fulfillment.service;

import org.openlmis.fulfillment.util.Message;

import lombok.Getter;

@Getter
public class FulfillmentException extends Exception {
  private final String messageKey;
  private final String[] params;
  private final Message message;

  /**
   * Creates new Fulfillment exception with message key and params.
   *
   * @param messageKey key that is related with exception message.
   * @param params     params that will be used in the exception message.
   */
  public FulfillmentException(String messageKey, String... params) {
    super(messageKey);
    this.messageKey = messageKey;
    this.params = params;
    this.message = new Message(messageKey, (Object[]) params);
  }

  /**
   * Creates new Fulfillment exception with message key and params.
   *
   * @param cause      the cause.
   * @param messageKey key that is related with exception message.
   * @param params     params that will be used in the exception message.
   */
  public FulfillmentException(Throwable cause, String messageKey, String... params) {
    super(messageKey, cause);
    this.messageKey = messageKey;
    this.params = params;
    this.message = new Message(messageKey, (Object[]) params);
  }

  public Message asMessage() {
    return message;
  }

  /**
   * Overrides Exception's public String getMessage().
   *
   * @return a localized string description
   */
  @Override
  public String getMessage() {
    return this.message.toString();
  }
}
