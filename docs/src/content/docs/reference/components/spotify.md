---
title: "Spotify"
description: "Spotify is a popular music streaming service that offers a vast library of songs, podcasts, and playlists for users to enjoy."
---

Spotify is a popular music streaming service that offers a vast library of songs, podcasts, and playlists for users to enjoy.



Type: spotify/v1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| clientId | Client Id | STRING | TEXT  |  | true  |
| clientSecret | Client Secret | STRING | TEXT  |  | true  |





<hr />



## Actions


### Play/Resume Playback
Start or resume current playback on an active device.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| deviceId | Device ID | STRING | TEXT  |  | false  |
| __item | Item | {STRING\(context_uri), [STRING]\(uris), INTEGER\(position_ms)} | OBJECT_BUILDER  |  | null  |




### Add Items to a Playlist
Adds one or more items to your playlist.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| playlist_id | Playlist ID | STRING | SELECT  |  | true  |
| uris | Tracks | [STRING] | ARRAY_BUILDER  |  | true  |
| __item | Item | {INTEGER\(position)} | OBJECT_BUILDER  |  | null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {STRING\(snapshot_id)} | OBJECT_BUILDER  |






### Create Playlist
Creates a new playlist

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| name | Name | STRING | TEXT  |  The name for the new playlist.  |  true  |
| description | Description | STRING | TEXT  |  The description for the new playlist.  |  false  |
| public | Public | BOOLEAN | SELECT  |  The public status for the new playlist.  |  true  |
| collaborative | Collaborative | BOOLEAN | SELECT  |  If the playlist is collaborative or not.  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| collaborative | BOOLEAN | SELECT  |
| description | STRING | TEXT  |
| external_urls | {STRING\(spotify)} | OBJECT_BUILDER  |
| href | STRING | TEXT  |
| id | STRING | TEXT  |
| name | STRING | TEXT  |
| type | STRING | TEXT  |
| uri | STRING | TEXT  |
| owner | {STRING\(href), STRING\(id), STRING\(type), STRING\(uri)} | OBJECT_BUILDER  |
| public | BOOLEAN | SELECT  |








## Triggers



<hr />

