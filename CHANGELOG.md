# Changelog

## 1.1.0

### Added

- `solsticeTiming` option: `MIDDLE`, the default, keeps the solstices and equinoxes in the middle of Mid Summer, Mid Winter, Mid Spring, and Mid Autumn, as in 1.0.0. `REALISTIC` moves them two thirds through Early Summer, Early Winter, Early Spring, and Early Autumn, close to the real calendar.
- `/seasonaltime set solsticeTiming <middle|realistic>`. `/seasonaltime get seasonal` and `/seasonaltime debug` show the timing.

### Changed

- A config file from 1.0.0 gets `solsticeTiming` added as `MIDDLE`, with one warning in the log.

## 1.0.0

### Added

- Configurable real-minute lengths for Minecraft day and night, without changing the server tick rate.
- `/gamerule doDaylightCycle` pauses and resumes time; the pause survives restarts.
- Time changes from `/time`, sleeping, and other mods are accepted.
- Optional [Serene Seasons](https://www.curseforge.com/minecraft/mc-mods/serene-seasons) support: the day and night split follows the season, and one Serene Seasons day lasts one Seasonal Time cycle on the server and every player's game.
- `/seasonaltime` commands: `get`, `set`, `seasonal`, `reload`, `debug`.
- `controlledDimensions` list for dimensions with their own clock.

### Compatibility

- Required on the server and on every player's game.
- Replaces [Time Control](https://www.curseforge.com/minecraft/mc-mods/time-control); do not install both.
