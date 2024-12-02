# Cryptojacking Listener - Bitcoin

This project is a part of a cryptojacking solution. The solution includes multiple listeners, each responsible for listening to a distinct cryptocurrency network. Each listener logs the latest blocks of that network, including the block's reception time and creation time. The logging format is consistent across all listeners, ensuring compatibility with the **detector** component of the cryptojacking solution. The detector expects the data in the same format to parse the log files correctly.

### Log Format
Each line in the log file follows this format:

<block hash hex value> (<block receive and log time on the host>) (<block creation time>)

For example:
000000000000000000064A648D001DEA08EF67E7AF02167D19F9B4BDD4241194 (2022/05/17, 18:54:53) (2022/05/17, 18:53:18)


### Project Overview
This project provides a UI to interface with the Bitcoin cryptocurrency peer-to-peer network. It facilitates the following features:
1. Specify the number of peers to connect with.
2. Define a starting time; only blocks created after this time will be recorded in the log file.

Additionally, console logs provide comprehensive block data, regardless of the specified starting time in the UI.

# CryptojackingTrap Detector
This project is a module of Cryptojackingtrap solution, which is responsible for detecting cryptojacking activity. 
cryptojacking is a malicious process of mining cryptocurrencies without victims' consent in an application. The 
detector receives different input files that feed its algorithm to decide whether the suspicious application is 
malicious or benign. One of these files is from the monitor module responsible for monitoring suspicious activity. 
There are other modules for listening to different cryptocurrency networks, and each of these modules creates a 
separate output file that must be given to the detector module. We have developed two listener modules for Bitcoin 
and Ethereum until now, and detector modularity makes it easy to extend to other listeners and enhances the detector 
precious.

## Opening the Project in an IDE

This project is Maven-based, so you can use Maven commands to set it up for your specific IDE. 

### For IntelliJ IDEA
To open the project in IntelliJ IDEA, follow these steps:

1. Navigate to the root folder of the project (where the `pom.xml` file is located).
2. Run the following command:

   ```bash
   mvn idea:idea
   ```

## Execution
The main class is `BlockChainListener.java` and by running this class you will have the main UI that let you enter your files 
and preferences and execute the main algorithm of detector. 

### Building the Project
This project is implemented in Java and can be compiled using Maven. To build the project, run the following commands:

```bash
mvn clean install
```

#### Example Interface
The interface allows users to configure the node count and connection settings for the Cryptojacking Listener - Bitcoin. Below is a snapshot of the Bitcoin listener interface:

<img width="527" alt="Cryptojackingtrap-Listener-Bitcoin (1)" src="https://user-images.githubusercontent.com/16403529/207733222-edd258e4-84e6-4fb9-bb52-3f7e8b1aaa6c.png">

#### How Does the Functionality Work?

1. **Setting Parameters**:  
   On the interface, you specify a starting date and time for the blocks whose hash values you want to record. Additionally, you define the number of nodes to connect to on the network. Once these parameters are set, click the "Start" button.

2. **Data Collection**:  
   The application begins acquiring block information from the connected nodes. It logs the hash values and timestamps of all blocks but does not save blocks created before the specified start time. 

3. **Logging and Persistence**:  
   When the application encounters the first block with a creation time equal to or greater than the specified start time, it begins saving the hash values and timestamps to a log file. All subsequent blocks are logged and persisted in the file as well.  

   As the application reaches real-time block processing, it slows down because, for example, in the Bitcoin network, blocks are typically released once every 10 minutes. When a new block is released, it is logged and saved immediately.

4. **Output File**:  
   The hash values are saved to the file located at:  
   `listener-bitcoin/src/test/resources/latestBlockHash-Bitcoin.txt`  

5. **Integration with the Detector Module**:  
   This file is used as input in the [detector module](https://github.com/CryptojackingTrap/detector) of the Cryptojacking solution. Refer to the detector module's documentation for details on how the file is utilized to detect cryptojacking.


Thank you for using the Cryptojacking Listener - Bitcoin. If you encounter any issues, feel free to submit a bug report or open an issue in this repository.


