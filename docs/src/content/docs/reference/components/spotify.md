---
title: "Spotify"
description: "Spotify is a popular music streaming service that offers a vast library of songs, podcasts, and playlists for users to enjoy."
---
## Reference
<hr />

Spotify is a popular music streaming service that offers a vast library of songs, podcasts, and playlists for users to enjoy.



Version: 1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Client Id | STRING | TEXT  |  |
| Client Secret | STRING | TEXT  |  |





<hr />



## Triggers



<hr />



## Actions


### Play/Resume Playback
Start or resume current playback on an active device.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Device | STRING | TEXT  |  |
| Item | {STRING\(context_uri), [STRING]\(uris), INTEGER\(position_ms)} | OBJECT_BUILDER  |  |




### Add Items to a Playlist
Adds one or more items to your playlist.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Playlist | STRING | SELECT  |  |
| Tracks | [STRING] | ARRAY_BUILDER  |  |
| Item | {INTEGER\(position)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING\(snapshot_id)} | OBJECT_BUILDER  |






### Create Playlist
Creates a new playlist

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Name | STRING | TEXT  |  The name for the new playlist.  |
| Description | STRING | TEXT  |  The description for the new playlist.  |
| Public | BOOLEAN | SELECT  |  The public status for the new playlist.  |
| Collaborative | BOOLEAN | SELECT  |  If the playlist is collaborative or not.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| BOOLEAN | SELECT  |
| STRING | TEXT  |
| {STRING\(spotify)} | OBJECT_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| {STRING\(href), STRING\(id), STRING\(type), STRING\(uri)} | OBJECT_BUILDER  |
| BOOLEAN | SELECT  |






