# Rade Keyboard — developer commands.
# All targets delegate to the Gradle wrapper. The Android SDK must be discoverable
# via local.properties (sdk.dir=...) or the ANDROID_HOME environment variable.

GRADLE := ./gradlew

.PHONY: help build release test lint install uninstall clean

help:
	@echo "Rade Keyboard - make targets:"
	@echo "  make build      Assemble the debug APK (assembleDebug)"
	@echo "  make release    Assemble the release App Bundle .aab (bundleRelease)"
	@echo "  make test       Run JVM unit tests (testDebugUnitTest)"
	@echo "  make lint       Run Android Lint (lintDebug)"
	@echo "  make install    Install the debug build on a connected device/emulator"
	@echo "  make uninstall   Remove the app from a connected device/emulator"
	@echo "  make clean      Clean build outputs"

build:
	$(GRADLE) assembleDebug

release:
	$(GRADLE) bundleRelease

test:
	$(GRADLE) testDebugUnitTest

lint:
	$(GRADLE) lintDebug

install:
	$(GRADLE) installDebug

uninstall:
	$(GRADLE) uninstallDebug

clean:
	$(GRADLE) clean
