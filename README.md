# local-settings-component
Component to read settings.xml without the full maven encumberance

If you have an instance of a PlexusContainer, you can get a `SettingsSupplier` instance from that container.
It will read the local settings.xml and hand you instances of `Settings`.

# Current Known Behaviors

* Will intentionally fail to test on Windows
* Will fail if no SDKMAN maven install is present.  (It doesn't need to be set as the MAVEN_HOME, just present)
* Will attempt to locate the global settings file in the `~/.sdkman` dir if no MAVEN_HOME is set
* Will create a local repository directory if not present
