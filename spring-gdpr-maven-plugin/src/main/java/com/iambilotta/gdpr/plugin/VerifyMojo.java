package com.iambilotta.gdpr.plugin;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Fails the build if the expected GDPR artifacts are missing under
 * {@code target/generated-sources/annotations/spring/gdpr/}. Wire to the {@code verify}
 * phase to enforce evidence-as-code in CI.
 */
@Mojo(name = "verify", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class VerifyMojo extends AbstractGdprMojo {

    @Parameter(property = "gdpr.requireDpia", defaultValue = "true")
    private boolean requireDpia;

    @Parameter(property = "gdpr.requireRopa", defaultValue = "true")
    private boolean requireRopa;

    @Override
    public void execute() throws MojoExecutionException {
        if (requireRopa) {
            File ropa = new File(processorOutputDir, "ropa.csv");
            if (!ropa.isFile()) {
                throw new MojoExecutionException(
                        "spring-gdpr verify: missing ROPA at " + ropa.getAbsolutePath()
                                + ". Did the annotation processor run? Add spring-gdpr-processor to the compile classpath.");
            }
        }
        if (requireDpia) {
            File dpia = new File(processorOutputDir, "dpia.md");
            if (!dpia.isFile()) {
                throw new MojoExecutionException(
                        "spring-gdpr verify: missing DPIA at " + dpia.getAbsolutePath());
            }
        }
        getLog().info("spring-gdpr verify: OK");
    }
}
