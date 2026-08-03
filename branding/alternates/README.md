# Alternate icon marks

Kept as backups for the shipped icon in `../icon-source.svg`.

## tsunami-line-art.svg

The Material Symbols `tsunami` glyph on the Tide gradient — a breaking wave in
line art, busier than the shipped bands but more literal.

Source: [Material Symbols](https://fonts.google.com/icons), licensed under the
Apache License 2.0. Shipping it requires no attribution in the app, but the
licence should be noted here.

To adopt it, convert the glyph path into `ic_launcher_foreground.xml`. Note the
Material grid is `0 -960 960 960`, so the path needs a `translateY` of `+960 *
scale` on top of the usual centring.
