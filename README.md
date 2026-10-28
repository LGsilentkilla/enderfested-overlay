# Ender Infestation

Ender Infestation is a Minecraft mod for Minecraft 1.21.1 using NeoForge 21.1.172.

## Features

- Adds the Ender Infestation potion effect and potion variants.
- Corruption can spread through affected entities and lingering potion clouds.
- Corrupted blocks use the mod's Enderfested block variants.
- Chorus growth can occur near active corruption clouds.
- Unstable Mixture explosions can damage entities and destroy blocks.
- Includes a custom emissive overlay rendering system for Enderfested blocks.
- Adds custom advancements for key mod milestones.

## Custom Overlay

The intended overlay texture path is:

`src/main/resources/assets/ender_infestation/textures/misc/enderfested_overlay.png`

The overlay should preserve its original colors and transparency. Emissive rendering can make the overlay appear bright in darkness, but it does not by itself illuminate nearby blocks.

## Testing Checklist

Before using the mod in an important world, test it in a separate creative test world:

- Confirm the mod loads and its blocks appear in the Creative inventory.
- Test the drinkable, splash, and lingering potion variants.
- Check that corruption spreads as intended and stops when lingering clouds disappear.
- Check chorus growth frequency and placement.
- Verify that Unstable Mixture explosions destroy blocks at the intended strength.
- Confirm the overlay follows block faces, preserves transparency, and renders without floating or camera-alignment issues.
- Check the advancement triggers.

## Compatibility

- Minecraft: 1.21.1
- NeoForge: 21.1.172
- Mod ID: `ender_infestation`
- Java package: `com.luigi.enderinfestation`

## Development Status

This project has not been verified by a successful build or in-game test in this document's preparation context. Treat the features above as intended functionality and verify them in-game before relying on them in a main survival world.
