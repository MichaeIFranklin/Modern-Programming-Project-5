//******************************************************************************
//
//  Developer:     Michael Franklin
//
//  Project #:     Project 5
//
//  File Name:     Project5Server.java
//
//  Course:        COSC 4301 - Modern Programming
//
//  Due Date:      03/13/2022
//
//  Instructor:    Fred Kumi 
//
//  Description:   Server side of Project 5.
//
//
//******************************************************************************

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class Project5Server
{
	private final int serverPort = 4301;
	
	// minimum number of threads to keep open for doing calculations
	// (threads that won't accept client connections)
	private final int minProccessingThreads = 4; 
	
	private int clientNum; 
	private int threadCount;
	private Socket socket;
	
	private ServerSocket serverSocket;
	private ExecutorService executorService;

	
	// ***************************************************************
	//
	// Method: main
	//
	// Description: The main method of the server
	//
	// Parameters: String array
	//
	// Returns: N/A
	//
	// **************************************************************
	public static void main(String argvs[])
	{
		// create class object
		Project5Server server = new Project5Server();
		server.developerInfo();
		
		// setup server
		server.Setup();
		
		// if setup was successful
		if (server.serverSocket != null && !server.serverSocket.isClosed())
		{
			System.out.println("\nWaiting for first client to connect... ");
			
			// main loop of server
			while (true)
			{	
				// wait for client to connect
				server.ConnectClient();	
				
				// check to see if we have enough main threads open for calculations
				if (server.threadCount - server.clientNum > server.minProccessingThreads)
				{					
					// pass client off to a serverclient thread for handling
					server.executorService.execute(new ServerClient(server.socket, server));
					
					// increment the client count
					server.clientNum++;
				}
				else
				{
					// pass back a "server is busy" message and disconnect the client
					server.executorService.execute(new ServerClient(server.socket, server,true));
				}
				
			}
		}
	}
	
	
	// ***************************************************************
	//
	// Method: Setup
	//
	// Description: Sets up the client by connecting to the server
	//
	// Parameters: None
	//
	// Returns: N/A
	//
	// **************************************************************
	public void Setup()
	{
		try 
		{
			
			// setup server socket
			serverSocket = new ServerSocket(serverPort);
			clientNum = 0;
			
			// add shutdown hook
			Runtime.getRuntime().addShutdownHook(new Thread() {
			       public void run()
			       {
			           // run server close sequence before full termination
			    	   CloseServer();
			       }
			       });
			
			// setup Executor instance
			// Get count of available cores
			threadCount = Runtime.getRuntime().availableProcessors();
			
			System.out.println("Server has " + threadCount + " processors. Allowing that many threads\n");
			executorService = Executors.newFixedThreadPool(threadCount);
			 
			System.out.println("Server started.");
		} 
		catch (Exception e) 
		{
			if (e instanceof IOException) 
			{
				System.err.printf("IO Error while starting server on port %d.\n", serverPort);
				e.printStackTrace();
			}
			else
			{
				System.err.printf("Unhandled exception while starting server on port %d.\n", serverPort);
				e.printStackTrace();
			}
			// close server
			CloseServer();
		}
	}
	

	// ***************************************************************
	//
	// Method: ConnectClient
	//
	// Description: waits for a client to connect and sets up data 
	//				streams and client socket.
	//
	// Parameters: None
	//
	// Returns: N/A
	//
	// **************************************************************
	public void ConnectClient()
	{
		try 
		{
			// wait for client to connect
			socket = serverSocket.accept();
		} 
		catch (Exception e) 
		{
			if (e instanceof IOException) 
			{
				System.err.printf("IO Error while setting up client on port %d.\n", socket.getPort());
				e.printStackTrace();
			}
			else
			{
				System.err.printf("Unhandled exception while setting up client on port %d.\n", socket.getPort());
				e.printStackTrace();
			}
			// error, connection not yet established, wait for next client
		}
	}

	
	// ***************************************************************
	//
	// Method: ClientDisconnected
	//
	// Description: Called by a client thread to notify a client disconnect.
	//
	// Parameters: None
	//
	// Returns: N/A
	//
	// **************************************************************
	public void ClientDisconnected() 
	{
		// decrement clientNum
		clientNum--;
	}
		
	
	// ***************************************************************
	//
	// Method: QueueTaskExecution
	//
	// Description: Called by a client thread to queue a threaded task.
	//
	// Parameters: Callable: the task to be queued for execution
	//
	// Returns: Future: the returned result from the task
	//
	// **************************************************************
	public <T> Future<T> QueueTaskExecution(Callable<T> task) 
	{
		return executorService.submit(task);
	}
	
	
	// ***************************************************************
	//
	// Method: CloseServer
	//
	// Description: Closes the server, gracefully shuts down the executor 
	//				service if possible
	//
	// Parameters: None
	//
	// Returns: N/A
	//
	// **************************************************************
	public void CloseServer() 
	{
		// print server closing
		System.out.printf("Closing server on port %s...\n",	serverPort);
				
		if (executorService != null)
		{
			// try to close Executor service gracefully
			executorService.shutdown();
			try 
			{
			    if (!executorService.awaitTermination(1000, TimeUnit.MILLISECONDS)) 
			    {
			        executorService.shutdownNow();
			    } 
			} 
			catch (InterruptedException e) 
			{
				System.err.printf("Interrupted Exception while while trying to close down Executor service, forcing service shutdown.\n", serverPort);
				e.printStackTrace();
			    executorService.shutdownNow();
			}
		}
		
		
		// close server socket
		try
		{
			if (serverSocket != null && !serverSocket.isClosed())
				serverSocket.close();
			
			// print server closing
			System.out.printf("Server on port $s closed.\n", serverPort);
		} 
		catch (Exception e)
		{
			if (e instanceof IOException)
			{
				System.err.printf("IO Error while closing server on port $s.\n", serverPort);
				e.printStackTrace();
			}
			else
			{
				System.err.printf("Unhandled exception while closing server on port $s.\n", serverPort);
				e.printStackTrace();
			}
			
			// print server closing
			System.out.printf("Server on port $s not properly closed.\n", serverPort);
		}
	}
		
	
	//***************************************************************
	//
	//  Method:       developerInfo (Non Static)
	// 
	//  Description:  The developer information method of the program
	//
	//  Parameters:   None
	//
	//  Returns:      N/A 
	//
	//**************************************************************
	public void developerInfo()
	{
		System.out.println("Name:    Michael Franklin");
		System.out.println("Course:  COSC 4301 Modern Programming");
		System.out.println("Project: Five\n");
	}
}