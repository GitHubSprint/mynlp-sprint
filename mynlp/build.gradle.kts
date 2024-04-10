plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

dependencies {
    api("org.jetbrains.kotlin:kotlin-stdlib")

    compileOnly("org.apache.lucene:lucene-core:9.10.0")

    //logs
    compileOnly(group = "org.slf4j", name = "slf4j-api", version = "2.0.9")
    compileOnly(group = "commons-logging", name = "commons-logging", version = "1.3.1")
    compileOnly(group = "org.apache.logging.log4j", name = "log4j-api", version = "2.23.1")
    compileOnly(group = "ch.qos.logback", name = "logback-classic", version = "1.5.2")

    compileOnly("net.openhft:zero-allocation-hashing:0.16")
    compileOnly(group = "org.fusesource.jansi", name = "jansi", version = "2.4.1")


    testImplementation("junit:junit:4.13.2")
    testImplementation("ch.qos.logback:logback-classic:1.5.2")
    testImplementation("com.mayabot.mynlp.resource:mynlp-resource-cws:1.0.0")
    testImplementation("org.apache.lucene:lucene-core:9.10.0")

    testImplementation("net.openhft:zero-allocation-hashing:0.16")


    testImplementation("com.mayabot.mynlp.resource:mynlp-resource-coredict:1.0.0")
    testImplementation("com.mayabot.mynlp.resource:mynlp-resource-pos:1.0.0")
    testImplementation("com.mayabot.mynlp.resource:mynlp-resource-ner:1.0.0")
    // pinyin
    testImplementation("com.mayabot.mynlp.resource:mynlp-resource-pinyin:1.1.0")
    testImplementation("com.mayabot.mynlp.resource:mynlp-resource-transform:1.0.2")
    testImplementation("com.mayabot.mynlp.resource:mynlp-resource-cws:1.0.0")
    testImplementation("com.mayabot.mynlp.resource:mynlp-resource-custom:1.0.0")
}


tasks {
    test {
        // 默认单个Test的内存是500M左右
        maxHeapSize = "1G"
    }
    jar {
        manifest {
            attributes["Main-Class"] = "com.mayabot.nlp.cli.MynlpCliKt"
        }
    }
    shadowJar {
        archiveClassifier.set("bin")
    }
}