# cryptojacking-listener-bitcoin

This project is a part of cryptojacking solution. This solution include multiple listeners that each of the is 
responsible to listening to a distinct cryptocurrency network and make log of the latest blocks of that network in 
addition to block received tim and the block creation time. The format of logging is the same in all listeners. The 
output files are used in the detector project in cryptojacking solution and detector expect the same data format to 
parse the files. Each file line is recorded with the following format:

block hash hex value (block receive and log time in the executing host), (block creation time)


This project has a UI to start to listening to Bitcoin cryptocurrency peer to peer network and make a log file of 
the latest blocks hash values. In the UI it is facilated for the client to specify the number of peers to connect 
and the starting time that the blocks that are created after that time would be recorded. If you notice the consule 
data you will find the whole blocks and their time regardless of starting time in the UI.  

for example:
000000000000000000064A648D001DEA08EF67E7AF02167D19F9B4BDD4241194 (2022/05/17, 18:54:53) (2022/05/17, 18:53:18)

In the follwoing figure, a snapshot of the Bitcoin listener interface specifies the count of network nodes that the CryptojackingTrap Bitcoin listener should connect to receive the blocks.

<img width="527" alt="Cryptojackingtrap-Listener-Bitcoin (1)" src="https://user-images.githubusercontent.com/16403529/207733222-edd258e4-84e6-4fb9-bb52-3f7e8b1aaa6c.png">

# Building the project
This project is purely java based and can be compiled with maven commands:
mvn clean install