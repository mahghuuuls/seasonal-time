# Seasonal Time

A Forge mod for Minecraft 1.12.2 that sets how many real minutes a Minecraft day and a Minecraft
night last. With [Serene Seasons](https://www.curseforge.com/minecraft/mc-mods/serene-seasons)
installed, the split between day and night follows the season: long days in summer, long nights in
winter.

- Player-facing description: [MOD-PAGE.md](MOD-PAGE.md)
- Release history: [CHANGELOG.md](CHANGELOG.md)
- License: MIT, see [LICENSE](LICENSE). Template notices: [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md)

Mod ID: `seasonaltime`.

## Install side

Required on the server and on every player's game. Forge refuses a player without it on a server
that has it.

## Behavior

- The server tick rate does not change. Only world time moves faster or slower.
- Day is world time 0 to 11999 and night is 12000 to 23999, as in vanilla. Each takes the configured
  number of real minutes.
- `/time set`, `/time add`, sleeping, and other mods can still change the time. Seasonal Time goes
  on from the new value.
- `/gamerule doDaylightCycle false` pauses time and `true` resumes it. The pause survives a restart.
  While the mod runs, the real vanilla rule is kept off; on a clean stop the pause state is written
  back into the rule before the world is saved.
- Dimensions that share the Overworld's time, such as the Nether, the End, and
  [The Aether](https://www.curseforge.com/minecraft/mc-mods/aether), follow the Overworld. A
  dimension with its own clock is advanced only when it is listed in `controlledDimensions`.

## Serene Seasons

Optional, for [Serene Seasons](https://www.curseforge.com/minecraft/mc-mods/serene-seasons) 1.2.18
or later.

- Seasonal adjustment: the share of the cycle that is daytime moves smoothly from
  `winterSolsticeDaylightPercent` in the middle of Mid Winter, through 50 percent in the middle of
  Mid Spring and Mid Autumn, to `summerSolsticeDaylightPercent` in the middle of Mid Summer. Day
  plus night keeps the same total length. Dimensions without Serene Seasons seasons use the base
  lengths.
- Serene Seasons day alignment (`alignSereneSeasonsDay`, on by default): one Serene Seasons day
  lasts one Seasonal Time cycle, on the server and on every player's game, so a sub-season lasts the
  number of Minecraft days set in Serene Seasons. Its `Day Duration` setting is replaced while this
  is on; its config file is not changed. Changing the cycle length, or turning alignment on or off,
  moves the current Serene Seasons season once. If one cycle is too long for Serene Seasons to count
  a year (very long cycles with very long sub-seasons), alignment is skipped with a warning in the
  log.

## Configuration

`config/seasonaltime.cfg`:

| Option | Default | Range | Meaning |
| --- | --- | --- | --- |
| `baseDayDurationMinutes` | 30 | 1-1440 | Real minutes for daytime without seasonal adjustment |
| `baseNightDurationMinutes` | 30 | 1-1440 | Real minutes for nighttime without seasonal adjustment |
| `seasonalAdjustmentEnabled` | true | | Split the cycle by season when Serene Seasons is installed |
| `summerSolsticeDaylightPercent` | 70.0 | 50.1-99.0 | Daytime share in the middle of Mid Summer |
| `winterSolsticeDaylightPercent` | 30.0 | 1.0-49.9 | Daytime share in the middle of Mid Winter |
| `alignSereneSeasonsDay` | true | | Serene Seasons day alignment |
| `controlledDimensions` | 0 | | Dimension IDs whose own clock is advanced |
| `debug` | false | | A status line in the log every 60 seconds |

Out-of-range numbers are clamped and unreadable values fall back to the default, each with a warning.

## Commands

All need operator permission (level 2).

- `/seasonaltime get [day|night|seasonal|effective]`
- `/seasonaltime set <day|night> <minutes>`
- `/seasonaltime set <summerDaylightPercent|winterDaylightPercent> <percent>`
- `/seasonaltime seasonal <true|false>`
- `/seasonaltime reload`
- `/seasonaltime debug`

`set` and `seasonal` save to the config file and apply at once.

## Time Control

Seasonal Time replaces [Time Control](https://www.curseforge.com/minecraft/mc-mods/time-control).
Do not install both.

## Removing the mod

After a clean stop, the world keeps a normal vanilla daylight cycle. After a crash or a forced stop
(for example Ctrl+C in a server console), the world may keep `doDaylightCycle` off; run
`/gamerule doDaylightCycle true` once.

## License

MIT. See [LICENSE](LICENSE).
