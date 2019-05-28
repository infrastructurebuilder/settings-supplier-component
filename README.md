# settings-supplier-component

Component to read settings.xml without the *entirety* of full Maven encumberance (just most of it).  That doesn't mean that this is a very lightweight component
(dependency-wise); only that it's not pulling in the whole Maven tree just to read a file.  This would probably be a good candidate for an
OSGi bundle.

If you have an instance of a (`sisu`-backed) `PlexusContainer`, you can get a `SettingsSupplier` instance from that container.
It will read [ some form of ] the local settings.xml and hand you instances of `SettingsProxy`, which type-proxies almost the entire `Settings` object tree.
This means that you can use the `settings.xml` file from Maven as a general supplier of certain important types of runtime configuration, including
and especially credentials.

# Current Known Behaviors

* Will intentionally fail to test on Windows
* Will fail if no SDKMAN maven install is present.  (It doesn't need to be set as the MAVEN_HOME, just present)
* Will attempt to locate the global settings file in the `~/.sdkman` dir if no MAVEN_HOME is set
* Will create a local repository directory if not present
