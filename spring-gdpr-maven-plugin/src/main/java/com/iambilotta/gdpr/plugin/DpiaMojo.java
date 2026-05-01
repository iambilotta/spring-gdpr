package com.iambilotta.gdpr.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Copies the generated {@code dpia.md} to {@code target/spring-gdpr/dpia.md} so it can be
 * attached to a release artifact or a regulator submission package without exposing the
 * deeper {@code generated-sources/} layout.
 */
@Mojo(name = "dpia", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public class DpiaMojo extends AbstractGdprMojo {

    @Override
    public void execute() throws MojoExecutionException {
        File source = requireFile("dpia.md");
        if (!publishDir.exists() && !publishDir.mkdirs()) {
            throw new MojoExecutionException("spring-gdpr: cannot create " + publishDir.getAbsolutePath());
        }
        File target = new File(publishDir, "dpia.md");
        try {
            Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new MojoExecutionException("spring-gdpr: failed to copy DPIA: " + ex.getMessage(), ex);
        }
        getLog().info("spring-gdpr dpia: " + target.getAbsolutePath());
    }
}
