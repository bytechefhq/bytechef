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

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| clientId | Client Id | STRING |  | true |
| clientSecret | Client Secret | STRING |  | true |





<hr />



## Actions


### Play/Resume Playback
Name: startResumePlayback

Start or resume current playback on an active device.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| deviceId | Device ID | STRING |  | false |
| __item | Item | OBJECT <details> <summary> Properties </summary> {STRING\(context_uri), [STRING]\(uris), INTEGER\(position_ms)} </details> |  | null |


#### JSON Example
```json
{
  "label" : "Play/Resume Playback",
  "name" : "startResumePlayback",
  "parameters" : {
    "deviceId" : "",
    "__item" : {
      "context_uri" : "",
      "uris" : [ "" ],
      "position_ms" : 1
    }
  },
  "type" : "spotify/v1/startResumePlayback"
}
```


### Add Items to a Playlist
Name: addItemsToPlaylist

Adds one or more items to your playlist.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| playlist_id | Playlist ID | STRING |  | true |
| uris | Tracks | ARRAY <details> <summary> Items </summary> [STRING] </details> |  | true |
| __item | Item | OBJECT <details> <summary> Properties </summary> {INTEGER\(position)} </details> |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| body | OBJECT <details> <summary> Properties </summary> {STRING\(snapshot_id)} </details> |




#### JSON Example
```json
{
  "label" : "Add Items to a Playlist",
  "name" : "addItemsToPlaylist",
  "parameters" : {
    "playlist_id" : "",
    "uris" : [ "" ],
    "__item" : {
      "position" : 1
    }
  },
  "type" : "spotify/v1/addItemsToPlaylist"
}
```


### Create Playlist
Name: createPlaylist

Creates a new playlist

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| name | Name | STRING | The name for the new playlist. | true |
| description | Description | STRING | The description for the new playlist. | false |
| public | Public | BOOLEAN <details> <summary> Options </summary> true, false </details> | The public status for the new playlist. | true |
| collaborative | Collaborative | BOOLEAN <details> <summary> Options </summary> true, false </details> | If the playlist is collaborative or not. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| collaborative | BOOLEAN <details> <summary> Options </summary> true, false </details> |
| description | STRING |
| external_urls | OBJECT <details> <summary> Properties </summary> {STRING\(spotify)} </details> |
| href | STRING |
| id | STRING |
| name | STRING |
| type | STRING |
| uri | STRING |
| owner | OBJECT <details> <summary> Properties </summary> {STRING\(href), STRING\(id), STRING\(type), STRING\(uri)} </details> |
| public | BOOLEAN <details> <summary> Options </summary> true, false </details> |




#### JSON Example
```json
{
  "label" : "Create Playlist",
  "name" : "createPlaylist",
  "parameters" : {
    "name" : "",
    "description" : "",
    "public" : false,
    "collaborative" : false
  },
  "type" : "spotify/v1/createPlaylist"
}
```




