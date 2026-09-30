# Build-time Rendering

Many applications ship raster images derived from an SVG kept in version control: PWA icons,
favicons, app store icons, Open Graph images. JairoSVG's [CLI](cli.html) can be run during the
build to produce them, so no bitmaps need to be committed and no external tools (Inkscape,
ImageMagick, a headless browser) are required.

No dedicated build plugin is needed: the CLI entry point `io.brunoborges.jairosvg.cli.Main` is part
of the regular `jairosvg` artifact and can be invoked in-process by your build tool.

> **Requirements:** the build must run on **Java 25+**. Requires JairoSVG **1.0.15** or later
> (earlier versions do not create missing output directories).

## Maven

Use the [Exec Maven Plugin](https://www.mojohaus.org/exec-maven-plugin/) `java` goal with
`jairosvg` as a *plugin* dependency. It does not need to be a dependency of your project.

Each `<execution>` renders one image. Writing into `${project.build.outputDirectory}` during
`process-resources` means the images are packaged like anything in `src/main/resources`.

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <version>3.6.4</version>
    <dependencies>
        <dependency>
            <groupId>io.brunoborges</groupId>
            <artifactId>jairosvg</artifactId>
            <version>1.0.14</version>
        </dependency>
    </dependencies>
    <configuration>
        <mainClass>io.brunoborges.jairosvg.cli.Main</mainClass>
        <includeProjectDependencies>false</includeProjectDependencies>
        <includePluginDependencies>true</includePluginDependencies>
    </configuration>
    <executions>
        <execution>
            <id>pwa-icon-512</id>
            <phase>process-resources</phase>
            <goals>
                <goal>java</goal>
            </goals>
            <configuration>
                <arguments>
                    <argument>${project.basedir}/src/main/icons/logo.svg</argument>
                    <argument>-o</argument>
                    <argument>${project.build.outputDirectory}/META-INF/resources/icons/icon-512.png</argument>
                    <argument>--output-width</argument>
                    <argument>512</argument>
                    <argument>--output-height</argument>
                    <argument>512</argument>
                </arguments>
            </configuration>
        </execution>
    </executions>
</plugin>
```

Run `mvn process-resources` to refresh the images without a full build.

### Several sizes from one SVG

Add one execution per output. Configuration shared by all executions (such as `mainClass`)
stays at plugin level:

```xml
<execution>
    <id>pwa-icon-192</id>
    <phase>process-resources</phase>
    <goals><goal>java</goal></goals>
    <configuration>
        <arguments>
            <argument>${project.basedir}/src/main/icons/logo.svg</argument>
            <argument>-o</argument>
            <argument>${project.build.outputDirectory}/META-INF/resources/icons/icon-192.png</argument>
            <argument>--output-width</argument><argument>192</argument>
            <argument>--output-height</argument><argument>192</argument>
        </arguments>
    </configuration>
</execution>
<execution>
    <id>apple-touch-icon</id>
    <phase>process-resources</phase>
    <goals><goal>java</goal></goals>
    <configuration>
        <arguments>
            <argument>${project.basedir}/src/main/icons/logo.svg</argument>
            <argument>-o</argument>
            <argument>${project.build.outputDirectory}/META-INF/resources/icons/apple-touch-icon.png</argument>
            <argument>--output-width</argument><argument>180</argument>
            <argument>--output-height</argument><argument>180</argument>
            <!-- iOS does not support transparent touch icons -->
            <argument>-b</argument><argument>white</argument>
        </arguments>
    </configuration>
</execution>
```

### Output format and size

- The output file extension selects the format: `.png`, `.jpg`/`.jpeg`, `.tif`/`.tiff`, `.pdf`,
  `.ps`, `.eps`. Use `-f` to set it explicitly.
- With only `--output-width` or `--output-height`, the other dimension follows the SVG's aspect
  ratio. With neither, the SVG's own size is used (optionally multiplied by `-s`).
- `-b COLOR` paints a background; PNG and TIFF are transparent otherwise.
- PDF output needs Apache PDFBox: add `org.apache.pdfbox:pdfbox` to the plugin `<dependencies>`.

See the [CLI Reference](cli.html) for all options. Unknown options and invalid values fail the
build.

### Writing to a generated-resources directory

If you prefer the conventional `target/generated-resources` layout (for example so other plugins
can pick up the images), render in `generate-resources` and register the directory with the
[Build Helper Maven Plugin](https://www.mojohaus.org/build-helper-maven-plugin/):

```xml
<!-- in each exec-maven-plugin execution -->
<phase>generate-resources</phase>
...
<argument>-o</argument>
<argument>${project.build.directory}/generated-resources/icons/META-INF/resources/icons/icon-512.png</argument>

<!-- and -->
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>build-helper-maven-plugin</artifactId>
    <version>3.6.1</version>
    <executions>
        <execution>
            <id>add-rendered-icons</id>
            <phase>generate-resources</phase>
            <goals><goal>add-resource</goal></goals>
            <configuration>
                <resources>
                    <resource>
                        <directory>${project.build.directory}/generated-resources/icons</directory>
                    </resource>
                </resources>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## Gradle

Register a `JavaExec` task on a dedicated configuration and make `processResources` depend on it:

```kotlin
val jairosvg by configurations.creating

dependencies {
    jairosvg("io.brunoborges:jairosvg:1.0.14")
}

val renderIcons by tasks.registering(JavaExec::class) {
    val svg = file("src/main/icons/logo.svg")
    val out = layout.buildDirectory.file("generated-resources/icons/META-INF/resources/icons/icon-512.png")
    inputs.file(svg)
    outputs.file(out)
    classpath = jairosvg
    mainClass.set("io.brunoborges.jairosvg.cli.Main")
    args(svg.path, "-o", out.get().asFile.path, "--output-width", "512", "--output-height", "512")
}

sourceSets.main {
    resources.srcDir(layout.buildDirectory.dir("generated-resources/icons"))
}

tasks.processResources {
    dependsOn(renderIcons)
}
```

Gradle's input/output tracking skips the task when neither the SVG nor the arguments changed.

## Acknowledgements

This recipe was inspired by
[viritin/svg-render-maven-plugin](https://github.com/viritin/svg-render-maven-plugin), a dedicated
plugin built on JairoSVG that offers extra conveniences such as `{size}` placeholders and incremental rendering.
