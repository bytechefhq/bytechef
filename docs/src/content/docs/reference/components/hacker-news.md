---
title: "Hacker News"
description: "Hacker News is a social news website focused on computer science, startups, and technology-related topics."
---

Hacker News is a social news website focused on computer science, startups, and technology-related topics.


Categories: Social Media


Type: hackerNews/v1

<hr />




## Actions


### Fetch Top Stories
Name: fetchTopStories

Fetch top stories from Hacker News.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| numberOfStories | Number Of Stories | INTEGER | Number of stories to fetch. | true |

#### Example JSON Structure
```json
{
  "label" : "Fetch Top Stories",
  "name" : "fetchTopStories",
  "parameters" : {
    "numberOfStories" : 1
  },
  "type" : "hackerNews/v1/fetchTopStories"
}
```

#### Output



Type: ARRAY


Items Type: OBJECT


#### Properties
|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
null





#### Output Example
```json
[ { } ]
```




