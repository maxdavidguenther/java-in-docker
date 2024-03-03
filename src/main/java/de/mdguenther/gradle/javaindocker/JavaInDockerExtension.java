package de.mdguenther.gradle.javaindocker;

import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

public interface JavaInDockerExtension {

  @Input()
  public abstract Property<String> getServiceName();

  @Optional
  @Input()
  public abstract Property<String> getDockerComposeFile();

  @Optional
  @Input()
  public abstract Property<String> getContainerName();
}
