package de.mdguenther.gradle.javaindocker;

public class JavaInDockerException extends RuntimeException {

  public JavaInDockerException(final String message) {
    super(message);
  }

  public JavaInDockerException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
