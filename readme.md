# MC: 1.16.5 - 1.21.8

### GRADLE (**build.gradle**)

Add the repository to your build.gradle file:
```gradle
plugins {
    id 'io.github.goooler.shadow' version '8.1.8'
    id 'java'
}

repositories {
    maven {
        name "TechmcStudios"
        url "https://repo.techmc.es/releases"
    }
}

dependencies {
    implementation 'dev.wuason:simple-toast:0.8'
}

tasks {
    shadowJar {
        relocate("dev.wuason.toastapi", "my.project")
    }
}
```

### GRADLE KOTLIN DSL (**build.gradle.kts**)

Add the repository to your build.gradle.kts file:
```kotlin
plugins {
    java
    id("io.github.goooler.shadow") version "8.1.8"
}

repositories {
    maven {
        name = "TechmcStudios"
        url = uri("https://repo.techmc.es/releases")
    }
}

dependencies {
    implementation("dev.wuason:simple-toast:0.8")
}

tasks.shadowJar {
    relocate("dev.wuason.toastapi", "my.project")
}
```

### MAVEN (**.pom**)

Add the repository to your pom.xml file:
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-shade-plugin</artifactId>
            <version>3.6.0</version>
            <executions>
                <execution>
                    <id>shade</id>
                    <phase>package</phase>
                    <goals>
                        <goal>shade</goal>
                    </goals>
                </execution>
            </executions>
            <configuration>
                <minimizeJar>true</minimizeJar>
                <relocations>
                    <relocation>
                        <pattern>dev.wuason.toastapi</pattern>
                        <!-- TODO: Change this to my own package name -->
                        <shadedPattern>my.project</shadedPattern>
                    </relocation>
                </relocations>
            </configuration>
        </plugin>
    </plugins>
</build>

<repositories>
    releases
        <id>techmc-studios</id>
        <name>TechMC Repository</name>
        <url>https://repo.techmc.es/</url>
    </repository>
</repositories>

<dependency>
    <groupId>dev.wuason</groupId>
    <artifactId>simple-toast</artifactId>
    <version>0.8</version>
    <scope>provided</scope>
</dependency>
```

### Usage Examples

Below are three different ways to build and send toast text content.

#### 1. Using Adventure Component (ComponentContent)
```java
import dev.wuason.toastapi.SimpleToast;
import dev.wuason.toastapi.content.ComponentContent;
import dev.wuason.toastapi.nms.EToastType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

public void sendComponentToast(Player player) {
    ItemStack icon = new ItemStack(Material.DIAMOND);
    Component comp = Component.text("Achievement:", NamedTextColor.GOLD)
            .append(Component.space())
            .append(Component.text("Shiny Diamond", NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
    SimpleToast.sendToast(icon, player, new ComponentContent(comp), EToastType.CHALLENGE);
}
```

#### 2. Using MiniMessage (MiniMessageContent)
```java
import dev.wuason.toastapi.SimpleToast;
import dev.wuason.toastapi.content.MiniMessageContent;
import dev.wuason.toastapi.nms.EToastType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

public void sendMiniMessageToast(Player player) {
    ItemStack icon = new ItemStack(Material.EMERALD);
    String mini = "<gradient:#ff0000:#ffff00><bold>Treasure Found</bold></gradient> <gray>(Level <green>5</green>)";
    SimpleToast.sendToast(icon, player, new MiniMessageContent(mini), EToastType.GOAL);
}
```

#### 3. Using Legacy / Hex Codes (TextContent)
TextContent parses ampersand codes into a JSON component automatically.
Supported:
- Colors: &0-&9 &a-&f (black, dark_blue, dark_green, dark_aqua, dark_red, dark_purple, gold, gray, dark_gray, blue, green, aqua, red, light_purple, yellow, white)
- Hex: &#RRGGBB (exactly 6 hex digits, case-insensitive)
- Styles: &l bold, &o italic, &n underlined, &m strikethrough, &k obfuscated
- Reset: &r (clears color + styles)
Applying a color (legacy or hex) resets active styles; styles stack until reset.
```java
import dev.wuason.toastapi.SimpleToast;
import dev.wuason.toastapi.content.TextContent;
import dev.wuason.toastapi.nms.EToastType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

public void sendLegacyToast(Player player) {
    ItemStack icon = new ItemStack(Material.GOLD_INGOT);
    String raw = "&a&lGreen Title&r Normal &#ff8800Hex &nUnderlined";
    SimpleToast.sendToast(icon, player, new TextContent(raw), EToastType.TASK);
}
```
Quick reference:
```
&0 black  &1 dark_blue  &2 dark_green  &3 dark_aqua
&4 dark_red &5 dark_purple &6 gold      &7 gray
&8 dark_gray &9 blue     &a green      &b aqua
&c red      &d light_purple &e yellow  &f white
Hex: &#RRGGBB   Styles: &l bold &o italic &n underlined &m strikethrough &k obfuscated &r reset
```
