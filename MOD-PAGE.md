Sets how long days and nights last in Minecraft, in real minutes. It works like [Time Control](https://www.curseforge.com/minecraft/mc-mods/time-control). By default one full day lasts 1 hour: 30 minutes of day and 30 minutes of night. With [Serene Seasons](https://www.curseforge.com/minecraft/mc-mods/serene-seasons), days get longer in summer and nights get longer in winter: by default 42 minutes of day and 18 of night at midsummer, and the reverse at midwinter. Days and nights change a little every day until they reach the solstice in the middle of Mid Summer or Mid Winter.

<span style="color:#d6a100">**AI usage disclaimer:** This mod was developed with AI-agent assistance using [this agent workflow](https://github.com/mahghuuuls/minecraft-1.12.2-mod-agent-workflow). The project owner reviewed the work during development.</span>

**Configuration:** everything works with the defaults. `config/seasonaltime.cfg` has these options:

- **Day and night length:** `baseDayDurationMinutes` and `baseNightDurationMinutes`, 30 minutes each by default.
- **Season extremes:** `summerSolsticeDaylightPercent` (70) and `winterSolsticeDaylightPercent` (30), the share of the full day that is daylight at midsummer and midwinter.
- **Seasons on or off:** `seasonalAdjustmentEnabled`.
- **Serene Seasons day length:** `alignSereneSeasonsDay`, see below.
- **Dimensions:** `controlledDimensions`, for dimensions with their own clock. The Nether, the End, [The Aether](https://www.curseforge.com/minecraft/mc-mods/aether), and most other modded dimensions follow the Overworld and need no entry.

Change the file and run `/seasonaltime reload`, or change a value in game with `/seasonaltime set`, for example `/seasonaltime set day 40`. Both need operator permission.

**Serene Seasons day length:** while `alignSereneSeasonsDay` is true (the default), Seasonal Time replaces the `Day Duration` setting of Serene Seasons, so one Serene Seasons day is one Seasonal Time day and night, and a season lasts the number of days set in Serene Seasons. Set it to false to use Serene Seasons' own setting.

**Vanilla time commands:** `/time set`, `/time add`, `/time query`, and sleeping work as usual; time goes on from the new value. `/gamerule doDaylightCycle false` stops time and `/gamerule doDaylightCycle true` starts it again.

**Install:** required on server and client.

**Removing it:** if time stays frozen after removing the mod following a crash, run `/gamerule doDaylightCycle true` once.

Source: [GitHub](https://github.com/mahghuuuls/seasonal-time)
