### GRADLE (**build.gradle**)

Add the repository to your build.gradle file:
```gradle
plugins {
    id 'io.github.goooler.shadow' version '8.1.8'
    id 'java'
}

repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.Wuason6x9:SimpleToastApi:RELEASE-VERSION'
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
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.Wuason6x9:SimpleToastApi:RELEASE-VERSION")
}

tasks.shadowJar {
    relocate("dev.wuason.toastapi", "my.project")
}
```

### MAVEN (**.pom**)

Add the repository to your pom.xml file:
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.Wuason6x9</groupId>
    <artifactId>SimpleToastApi</artifactId>
    <version>RELEASE-VERSION</version>
    <scope>provided</scope>
</dependency>
```

Example of use:

```java
    public void sendToast(Player player) {
        ItemStack itemStack = new ItemStack(Material.DIAMOND);

        String text = GsonComponentSerializer.gson().serialize(
                MiniMessage.miniMessage().deserialize("<rainbow>Hello!</rainbow>")
        );
        
        SimpleToast.sendToast(itemStack, player, text, EToastType.CHALLENGE);
    }
```
