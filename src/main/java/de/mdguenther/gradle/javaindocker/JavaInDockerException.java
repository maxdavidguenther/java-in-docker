package de.mdguenther.gradle.javaindocker;

/**
 * Root class for all exceptions thrown by Java in Docker.
 */
public class JavaInDockerException extends RuntimeException {

  /**
   * Create an exception with a message.
   *
   * @param message the message
   */
  public JavaInDockerException(final String message) {
    super(message);
  }

  /**
   * Create an exception with a message and a cause.
   *
   * @param message the message
   * @param cause an exception that caused this exception
   */
  public JavaInDockerException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
