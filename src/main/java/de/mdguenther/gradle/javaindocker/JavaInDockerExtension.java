package de.mdguenther.gradle.javaindocker;

import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

public interface JavaInDockerExtension {

  @Input()
  Property<String> getServiceName();

  @Optional
  @Input()
  Property<String> getDockerComposeFile();

  @Optional
  @Input()
  Property<String> getContainerName();

  @Optional
  @Input()
  ListProperty<String> getAdditionalDockerRunArgs();
}
