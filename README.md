#ComposerTrigger for Jenkins

The ComposerTrigger schedules a build when a dependency of a composer project has an update

##Configuration
* in the configuration of a job activate ``check for dependency updates with composer``
* enter a cron-expression to check periodically for updates
* enter the path to the php-binary
* enter the path to composer.phar

please make sure that during the build process a `` composer update`` is performed!

