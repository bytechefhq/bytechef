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
| context_uri | Context Uri | STRING | Spotify URI of the context to play (album, artist, playlist). | false |
| uris | Tracks | ARRAY <details> <summary> Items </summary> [STRING] </details> | Spotify track URIs to play. | false |
| position_ms | Position | INTEGER | The position in milliseconds to start playback from. | false |

#### Example JSON Structure
```json
{
  "label" : "Play/Resume Playback",
  "name" : "startResumePlayback",
  "parameters" : {
    "deviceId" : "",
    "context_uri" : "",
    "uris" : [ "" ],
    "position_ms" : 1
  },
  "type" : "spotify/v1/startResumePlayback"
}
```

#### Output

This action does not produce any output.




### Add Items to a Playlist
Name: addItemsToPlaylist

Adds one or more items to your playlist.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| playlist_id | Playlist ID | STRING |  | true |
| uris | Tracks | ARRAY <details> <summary> Items </summary> [STRING] </details> |  | true |
| position | Position | INTEGER | Position to insert the items, a zero-based index. | false |

#### Example JSON Structure
```json
{
  "label" : "Add Items to a Playlist",
  "name" : "addItemsToPlaylist",
  "parameters" : {
    "playlist_id" : "",
    "uris" : [ "" ],
    "position" : 1
  },
  "type" : "spotify/v1/addItemsToPlaylist"
}
```

#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| snapshot_id | STRING |  |




#### Output Example
```json
{
  "snapshot_id" : ""
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

#### Example JSON Structure
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

#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| collaborative | BOOLEAN <details> <summary> Options </summary> true, false </details> |  |
| description | STRING |  |
| external_urls | OBJECT <details> <summary> Properties </summary> {STRING\(spotify)} </details> |  |
| href | STRING |  |
| id | STRING |  |
| name | STRING |  |
| type | STRING |  |
| uri | STRING |  |
| owner | OBJECT <details> <summary> Properties </summary> {STRING\(href), STRING\(id), STRING\(type), STRING\(uri)} </details> |  |
| public | BOOLEAN <details> <summary> Options </summary> true, false </details> |  |




#### Output Example
```json
{
  "collaborative" : false,
  "description" : "",
  "external_urls" : {
    "spotify" : ""
  },
  "href" : "",
  "id" : "",
  "name" : "",
  "type" : "",
  "uri" : "",
  "owner" : {
    "href" : "",
    "id" : "",
    "type" : "",
    "uri" : ""
  },
  "public" : false
}
```




