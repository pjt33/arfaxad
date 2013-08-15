# Arfaxad

Arfaxad is a song lyric projection program, developed primarily to be a useful tool for churches.


## Design goals

* Songs should be stored in a format which is suited for version control.
* Everything should be accessible quickly via keyboard, which is the expected peripheral of choice for power users.
  In particular, with practice it should be possible to jump to any slide in the current song with minimal keystrokes.
* Mouse interaction should also be supported for casual users.


## Configuration

Arfaxad supports multiple profiles, which can be useful if the same computer is shared by people who use different
localisations, or if you want to have different song directories for meetings in English and Spanish. (Currently
there's no direct support for sharing songs between profiles, so that requires manual handling via symlinks).

To find the profiles, it looks in sequence at `/etc/arfaxad.conf`, `./arfaxad.conf`, and `~/.arfaxad/arfaxad.conf`.
(The second of those is a nod at Windows, although I'm sure the Windows support could be more idiomatic).
In each location it parses the file for a line like

    directory.profiles=~/.arfaxad/profiles

If none of the files exists, that value (`~/.arfaxad/profiles`) is the default.

It then looks in the named directory for files which can be parsed as a list of `key=value` pairs.
The supported keys are

* `name`: the name of the profile
* `directory.songs`: the directory from which to load songs
* `logo`: the path of the logo image
* `localisation`: the locale to use (e.g. `en-GB`, `es`, etc.)
* `log`: the path for the copyright log

This currently needs to be set up manually.


## Song markup

Songs are divided into slides, and there are various types of slide corresponding to the typical structure of a song.
By correctly identifying the slides you make it easy to jump to the correct one.
The four primary slide types are `Verse`, `Pre-chorus`, `Chorus`, and `Bridge`. There's also a general-purpose `Slide`,
but that doesn't support quick jumping.

When entering a song you should type the slide type before the slide on a single line with no whitespace. E.g.

    Verse
    O come, all ye faithful
    Joyful and triumphant
    O come ye, O come ye to Bethlehem
    Come and behold Him
    Born the King of angels

    Chorus
    Oh come, let us adore Him
    Oh come, let us adore Him
    Oh come, let us adore Him
    Christ the Lord

    Verse
    God of gods, light of light
    Lo, he abhors not the virgin's womb
    Very God
    Begotten not created 

etc. Note that the verse numbers will be calculated automatically and displayed in the preview. 

The `Slide` type is for use when a single verse or chorus is too large for one slide and you want to split it in two
without affecting the numbering.

There isn't much markup supported within a slide, but you can put text in italics by wrapping it in `[i]...[/i]`
and in superscript by wrapping it in `[sup]...[/sup]`.

When a song is displayed, the font will be sized automatically. In the editor window, lines which are too long to
show at maximum size will be in a shade of blue, and lines which force the use of a font size smaller than the
(currently hard-coded) ideal minimum will be in a shade of red. This allows you to identify the line which is
limiting the font size. If the limit is vertical (due to the number of lines in a slide) then the slide type line
will be coloured thus instead.


## History

The original Arfaxad was written in January 2008 to replace an awful system which used a PowerPoint file per song.
The source was in Spanish. Prior to uploading it to GitHub, everything has been renamed to be in English; this is
one of the main reasons for adding it en masse without attempting to preserve the previous versioning.
