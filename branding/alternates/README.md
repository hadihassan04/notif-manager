# Alternate icon marks

Kept as backups for the shipped icon in `../icon-source.svg`.

## tide-bands.svg

Two round-capped tide bands, stroke rather than outline so they stay readable at
24px. Shipped through 0.2.0; quieter than the wave but reads as an equals sign
at small sizes.

## tsunami-line-art.svg

The source of the shipped mark: the Material Symbols `tsunami` glyph on the Tide
gradient. Kept here at its original scale, which overflows the 66dp circle a
launcher mask guarantees — the shipped copy is scaled to 48x43 instead.

Source: [Material Symbols](https://fonts.google.com/icons), licensed under the
Apache License 2.0. Shipping it requires no attribution in the app, but the
licence should be noted here.

The Material grid is `0 -960 960 960`, so adopting any glyph from it needs a
`translateY` of `+480 * scale` on top of the usual centring, not `+960 * scale`:
the ink sits between -840 and -120, not against the top of the box.
