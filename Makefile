.PHONY: help build clean test check assemble shadow-jar descriptor

GRADLE ?= ./gradlew
JAR := app/build/libs/app.jar

help:
	@printf '%s\n' \
		'Protospector build targets:' \
		'  make build        Clean and build the Burp extension JAR' \
		'  make shadow-jar   Build the shaded extension JAR' \
		'  make check        Run Gradle checks' \
		'  make clean        Remove build outputs' \

build:
	$(GRADLE) clean shadowJar
	@printf '\nBuilt extension: %s\n' '$(JAR)'

shadow-jar:
	$(GRADLE) shadowJar
	@printf '\nBuilt extension: %s\n' '$(JAR)'

check:
	$(GRADLE) check

clean:
	$(GRADLE) clean

test:
	$(GRADLE) test
