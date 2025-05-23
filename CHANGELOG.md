# Changelog

<a id="v1.3.4"></a>
## [Backup (Privacy Friendly) v1.3.4](https://github.com/SecUSo/privacy-friendly-backup/releases/tag/v1.3.4) - 2024-12-04

## What's Changed
* Fix crash on devices running API 22 or lower by [@udenr](https://github.com/udenr) in [#36](https://github.com/SecUSo/privacy-friendly-backup/pull/36)
* Update dependencies by [@udenr](https://github.com/udenr) in [#37](https://github.com/SecUSo/privacy-friendly-backup/pull/37)
* Change project structure by [@udenr](https://github.com/udenr) in [#38](https://github.com/SecUSo/privacy-friendly-backup/pull/38)


**Full Changelog**: https://github.com/SecUSo/privacy-friendly-backup/compare/v1.3.3...v1.3.4

[Changes][v1.3.4]


<a id="v1.3.3"></a>
## [Backup (Privacy Friendly) v1.3.3](https://github.com/SecUSo/privacy-friendly-backup/releases/tag/v1.3.3) - 2024-11-12

## What's Changed
* Feature: Export and import multiple backups at once by [@udenr](https://github.com/udenr) in [#31](https://github.com/SecUSo/privacy-friendly-backup/pull/31)

**Full Changelog**: https://github.com/SecUSo/privacy-friendly-backup/compare/v1.3.2...v1.3.3

[Changes][v1.3.3]


<a id="v1.3.2"></a>
## [Backup (Privacy Friendly) v1.3.2](https://github.com/SecUSo/privacy-friendly-backup/releases/tag/v1.3.2) - 2024-10-21

## What's Changed
* Fix minor spelling and grammar mistakes in German strings.xml by [@realpixelcode](https://github.com/realpixelcode) in [#27](https://github.com/SecUSo/privacy-friendly-backup/pull/27)
* documentation: Fixed incorrect weblink by [@jahway603](https://github.com/jahway603) in [#21](https://github.com/SecUSo/privacy-friendly-backup/pull/21)

## New Contributors
* [@realpixelcode](https://github.com/realpixelcode) made their first contribution in [#27](https://github.com/SecUSo/privacy-friendly-backup/pull/27)
* [@jahway603](https://github.com/jahway603) made their first contribution in [#21](https://github.com/SecUSo/privacy-friendly-backup/pull/21)

**Full Changelog**: https://github.com/SecUSo/privacy-friendly-backup/compare/v1.3.1...v1.3.2

[Changes][v1.3.2]


<a id="v1.3.1"></a>
## [Backup (Privacy Friendly) v1.3.1](https://github.com/SecUSo/privacy-friendly-backup/releases/tag/v1.3.1) - 2024-07-04

## What's Changed
* Update README.md by [@Poussinou](https://github.com/Poussinou) in [#11](https://github.com/SecUSo/privacy-friendly-backup/pull/11)
* Added the ability to display PFA backup errors in the overview by [@Kamuno](https://github.com/Kamuno) in [#12](https://github.com/SecUSo/privacy-friendly-backup/pull/12)
* Update workflow and fix dependencies by [@udenr](https://github.com/udenr) in [#25](https://github.com/SecUSo/privacy-friendly-backup/pull/25)
* Updates App to Target SDK 34 by [@coderPaddyS](https://github.com/coderPaddyS) in [#26](https://github.com/SecUSo/privacy-friendly-backup/pull/26)

## New Contributors
* [@Poussinou](https://github.com/Poussinou) made their first contribution in [#11](https://github.com/SecUSo/privacy-friendly-backup/pull/11)
* [@udenr](https://github.com/udenr) made their first contribution in [#25](https://github.com/SecUSo/privacy-friendly-backup/pull/25)
* [@coderPaddyS](https://github.com/coderPaddyS) made their first contribution in [#26](https://github.com/SecUSo/privacy-friendly-backup/pull/26)

**Full Changelog**: https://github.com/SecUSo/privacy-friendly-backup/compare/v1.3...v1.3.1

[Changes][v1.3.1]


<a id="v1.3"></a>
## [Backup (Privacy Friendly) v1.3](https://github.com/SecUSo/privacy-friendly-backup/releases/tag/v1.3) - 2022-07-29

## What's Changed
* Switch to using SHA256 instead of insecure SHA1 by [@Kamuno](https://github.com/Kamuno) in [#8](https://github.com/SecUSo/privacy-friendly-backup/pull/8)
* Fixed restore actions not completing correctly and encryption api support by [@Kamuno](https://github.com/Kamuno) in [#9](https://github.com/SecUSo/privacy-friendly-backup/pull/9)

**Full Changelog**: https://github.com/SecUSo/privacy-friendly-backup/compare/v1.2...v1.3

[Changes][v1.3]


<a id="v1.2"></a>
## [Backup (Privacy Friendly) v1.2](https://github.com/SecUSo/privacy-friendly-backup/releases/tag/v1.2) - 2021-10-22

- Added support for the Pedometer from F-Droid

[Changes][v1.2]


<a id="v1.1"></a>
## [Backup (Privacy Friendly) v1.1](https://github.com/SecUSo/privacy-friendly-backup/releases/tag/v1.1) - 2021-07-15

- Added query elements to integrate the package visibility changes on API 30

[Changes][v1.1]


<a id="v1.0"></a>
## [Backup (Privacy Friendly) v1.0](https://github.com/SecUSo/privacy-friendly-backup/releases/tag/v1.0) - 2021-01-27

Privacy Friendly Backup is an application that works with other privacy friendly apps to enable backups. The app allows you to create and manage backups. This includes importing backups into the app and exporting backups to external storage media.
The Privacy Friendly Backup app communicates with other Privacy Friendly apps and extracts or injects data into and out of the app to enable the creation and restoration of backups.

The app provides encryption via an interface to another app that provides encryption as functionality via the openpgp-api. The user is free to choose their own encryption provider via the app's settings.
For encryption to work, the provider must be installed externally. We recommend https://www.openkeychain.org/ , as this app is available in the regular PlayStore as well as in the F-Droid Store and is completely open source.

[Changes][v1.0]


[v1.3.4]: https://github.com/SecUSo/privacy-friendly-backup/compare/v1.3.3...v1.3.4
[v1.3.3]: https://github.com/SecUSo/privacy-friendly-backup/compare/v1.3.2...v1.3.3
[v1.3.2]: https://github.com/SecUSo/privacy-friendly-backup/compare/v1.3.1...v1.3.2
[v1.3.1]: https://github.com/SecUSo/privacy-friendly-backup/compare/v1.3...v1.3.1
[v1.3]: https://github.com/SecUSo/privacy-friendly-backup/compare/v1.2...v1.3
[v1.2]: https://github.com/SecUSo/privacy-friendly-backup/compare/v1.1...v1.2
[v1.1]: https://github.com/SecUSo/privacy-friendly-backup/compare/v1.0...v1.1
[v1.0]: https://github.com/SecUSo/privacy-friendly-backup/tree/v1.0

<!-- Generated by https://github.com/rhysd/changelog-from-release v3.8.1 -->
