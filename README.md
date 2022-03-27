# <p align="center"><img src="https://aetherclient.com/images/gato.png" width="40"> Minecraft Spotify Integration</p>

Spotify integration into minecraft, for use in pvp/hacked clients for free
(but give credits to cedo & aether, k thx)

## Setup
We used the [Spotify Java Wrapper](https://github.com/spotify-web-api-java/spotify-web-api-java) lib, specifically the version 
`6.5.4` (because the others break stuff idk)\

*The library includes a newer version of gson in its dependencies, higher than the one in minecraft 1.8.X, so this might cause an NoSuchMethodError pointing to JsonParser. remove the older one.*
### Maven:
`se.michaelthelin.spotify:spotify-web-api-java:6.5.4`
or build and add it as a jar to your dependencies
### Read the comments
Read the comments in the code, they will explain how to use & hook stuff.
If you find any issues let me know

##
I made this in a blank java project with none of the minecraft stuff so you're gonna have to add some imports, hopefully it works lol
