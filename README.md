# local-settings-component
Component to read settings.xml without the full maven encumberance

If you have an instance of a PlexusContainer, you can get a `SettingsSupplier` instance from that container.
It will read the local settings.xml and hand you instances of `Settings`.