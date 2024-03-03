package de.mdguenther.gradle.javaindocker;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Exec;
import org.gradle.api.tasks.Input;

/**
 * Removes the docker container with the name of {@link #getContainerName()} with {@code --force}.
 */
public abstract class RemoveContainerTask extends Exec {

  /**
   * The name of the container to remove.
   *
   * @return the name of the container
   */
  @Input()
  public abstract Property<String> getContainerName();

  /**
   * Create the task.
   */
  @Inject
  public RemoveContainerTask() {}

  @Override
  public void exec() {
    final List<String> args = new ArrayList<>();

    args.add("rm");
    args.add("--force");
    args.add(getContainerName().get());

    setCommandLine("docker");
    setArgs(args);

    getLogger().info("Commandline: " + getCommandLine());

    final ByteArrayOutputStream captureErrorOutput = new ByteArrayOutputStream();
    setErrorOutput(captureErrorOutput);
    final ByteArrayOutputStream captureStandardOutput = new ByteArrayOutputStream();
    setStandardOutput(captureStandardOutput);

    super.exec();

    if (getExecutionResult().get().getExitValue() != 0 || !getLogger().isQuietEnabled()) {
      final Charset consoleCharset = System.console() != null && System.console().charset() != null
        ? System.console().charset()
        : StandardCharsets.UTF_8;
      final String capturedErrorOutput = captureErrorOutput.toString(consoleCharset);

      if (capturedErrorOutput.equals("Error response from daemon: No such container: " + getContainerName() + "\n")) {
        setIgnoreExitValue(true);
      } else {
        final String capturedStandardOutput = captureStandardOutput.toString(consoleCharset);
        getLogger().info(capturedStandardOutput);
        getLogger().error(capturedErrorOutput);
      }
    }
  }
}
