# Social network clone
This program is social network clone. It is system design task where I implemented base functions of social network like twitter. In this program user can:
- post, comment and repost messages
- like and dislike messages
- subscribe on users
- print feed

All logic of this program build on using Mongo databse. Feed of each user is composing with posts of user and posts of friends (users on who user is subscribed). In demo (SimpleTest) I execute actions of different users parallel and observe database on changes (used mongodb's change streams) that are made after subscribing on observable (in this case observable is database).
