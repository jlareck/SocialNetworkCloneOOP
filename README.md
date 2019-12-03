# Social network simulator
This program is social network simulator. It is system design task where is proected simple social network. In this program user can:
- post, comment and repost messages
- like and dislike messages
- subscribe on users
- print feed

All logic of this program build on using databse Mongodb. Feed of each user is composing with posts of user and posts of friends (users on who user is subscribed). In demo (SimpleTest) I also used multithreading so that multiple users can do some actions parallel and observe database on changes (used mongodb's change streams) that are made after subscribing on observable (mongodb databse).
