# PlayerData2MCASelector
A simple tool to scrape the player position from player data files and create a selection file to be imported by MCA Selector.

## Usage

```
java -jar playerdata2mcaselector.jar <path-to-folder-with player-data-files> <output.csv> [radius]
```

## Example

This will select all chunks in a square around the chunk the players logged out (2 chunks in all directions).

```
java -jar playerdata2mcaselector.jar world/playerdata output.csv 2
```