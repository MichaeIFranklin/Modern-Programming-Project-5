//******************************************************************************
//
//  Developer:     Michael Franklin
//
//  Project #:     Project 5
//
//  File Name:     ServerClient.java
//
//  Course:        COSC 4301 - Modern Programming
//
//  Due Date:      03/13/2022
//
//  Instructor:    Fred Kumi 
//
//  Description:   Client handler for the server side of Project 5.
//
//
//******************************************************************************

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class ServerClient implements Runnable
{
	private final String closeCommand = "Bye";
	private final int packageSize = 10; // number of primes to send per response, if prime list gets too big
	private final String packageDelimiter = "~";
	private final String connectionToken = "y";
	
	private Project5Server server;
	private Socket clientSocket;
	private DataOutputStream dataOutStream;
	private DataInputStream dataInStream;
	private boolean disconnect; // true if server is full
	
	public ServerClient(Socket socket, Project5Server server, boolean disconnect)
	{
		clientSocket = socket;
		this.server = server;
		this.disconnect = disconnect;
	}
	
	public ServerClient(Socket socket, Project5Server server)
	{
		clientSocket = socket;
		this.server = server;
		this.disconnect = false;
	}
	
	
	// ***************************************************************
	//
	// Method: run
	//
	// Description: The main method of the server client handler
	//
	// Parameters: None
	//
	// Returns: N/A
	//
	// **************************************************************
	@Override
	public void run()
	{
		// setup client
		SetupClient();
		
		// if we do not need to disconnect the client right away
		if (!disconnect)
		{
			// send that we are fully connected to the client
			SendData(connectionToken);
			
			String input = "";
			// while no closing command has been sent and the socket is still open
			while (!input.equals(closeCommand) && !clientSocket.isClosed()) 
			{
				// wait for input from the client
				input = GetClientData();

				// if input is not the close connection command and socket is still open
				if (!input.equals(closeCommand) && !clientSocket.isClosed()) 
				{
					// parse and validate input
					ArrayList<Integer> numList = ParseClientData(input);

					// if we got valid data and socket is still open
					if (numList.size() != 0 && !clientSocket.isClosed())
					{
						// calculate sum, mean and standard deviation
						// then send to client
						CalculateOutput(numList);
					}
				}
			}
		}
		else
		{
			// server is full disconnect client right away
			SendData("xServer is full, please try again later");
		}
		
		// if the socket is still open
		if (!clientSocket.isClosed())
		{
			// send a closing statement to client
			SendData("xServer closing Connection.");
		}
		
		// close connection to client
		CloseConnection();	
	}

	
	// ***************************************************************
	//
	// Method: SetupClient
	//
	// Description: sets up data streams and client socket.
	//
	// Parameters: None
	//
	// Returns: N/A
	//
	// **************************************************************
	public void SetupClient()
	{
		try 
		{			
			// client connected successful
			// open output stream to the client
			dataOutStream = new DataOutputStream(clientSocket.getOutputStream());
			
			// open input stream from the client
			dataInStream = new DataInputStream(clientSocket.getInputStream());
			
			// print client socket setup successful
			System.out.printf("\nClient connected:\nIP:%s\nPort:%d\n",
					clientSocket.getInetAddress().toString(),
					clientSocket.getPort());
		} 
		catch (Exception e) 
		{
			if (e instanceof IOException) 
			{
				System.err.printf("IO Error while setting up client on port %d.\n", clientSocket.getPort());
				e.printStackTrace();
			}
			else
			{
				System.err.printf("Unhandled exception while setting up client on port %d.\n", clientSocket.getPort());
				e.printStackTrace();
			}
			// error, close the client connection
			CloseConnection();
		}
	}

	
	// ***************************************************************
	//
	// Method: PackageData
	//
	// Description: Sends large data to a client by first packaging it into 
	//				smaller chunks then sending each chunk
	//
	// Parameters: double: sum to send
	//			   double: mean to send
	//			   double: standard deviation to send
	//			   ArrayList<Double>: list of primes to send
	//
	// Returns: N/A
	//
	// **************************************************************
	public void PackageData(double sum, double mean, double stdev, ArrayList<Integer> primeList)
	{		
		// send the first three items, the answers, to the client
		SendData(sum + "," + mean + "," + stdev);
		
		// send the list of primes in chunks if it is too big
		for (int i = 0; i < primeList.size();)
		{
			StringBuilder outputString = new StringBuilder();
			// send packageSize number of primes at a time
			for (int count = 0; count < packageSize && i < primeList.size();count++)
			{
				// if we have reached the end data append a delimiter
				// to tell the client to stop listening for more data
				if (i == primeList.size()-1)
				{
					// append last item to outputString
					outputString.append(primeList.get(i) + "," + packageDelimiter);
				}
				// if we have reached the end of the chunk don't add a comma
				else if (count == packageSize-1)
				{
					// append item to outputString
					outputString.append(primeList.get(i));
				}
				else
				{
					// append item to outputString
					outputString.append(primeList.get(i) + ",");
				}
				
				// increment index
				i++;
			}
			// send the chunk to the client
			SendData(outputString.toString());
		}
	}
	
	
	// ***************************************************************
	//
	// Method: SendData
	//
	// Description: Sends the passed string to the client
	//
	// Parameters: String: data to be sent to the client
	//
	// Returns: N/A
	//
	// **************************************************************
	public void SendData(String data)
	{
		try 
		{
			dataOutStream.writeUTF(data);
		}
		catch (IOException e) 
		{
			System.err.printf("IO Error while sending data to client on port %d.\n", clientSocket.getPort());
			e.printStackTrace();
			// not a critical error, no need to quit
			// error, close the client connection
			CloseConnection();
		}
	}
	

	// ***************************************************************
	//
	// Method: GetClientData
	//
	// Description: waits for and accepts data from the client
	//
	// Parameters: None
	//
	// Returns: String: data from client
	//
	// **************************************************************
	public String GetClientData()
	{
		String data = "";
		try
		{
			data = dataInStream.readUTF();
		}
		catch (Exception e) 
		{
			if (e instanceof SocketException) 
			{
				System.out.printf("Client on port %d disconnected unexpectedly. Closing the connection fully.\n", clientSocket.getPort());
				// not a critical error, no need to quit
			}
			else if (e instanceof IOException) 
			{
				System.out.printf("IO Error while reading data from client on port %d.\n", clientSocket.getPort());
				e.printStackTrace();
				// not a critical error, no need to quit
			}
			else if (e instanceof EOFException) 
			{
				System.out.printf("End of Field Error while reading data from client on port %d.\n", clientSocket.getPort());
				e.printStackTrace();
				// not a critical error, no need to quit
			}
			else
			{
				System.err.printf("Unhandled exception while reading data from client on port %d.\n", clientSocket.getPort());
				e.printStackTrace();
				// not a critical error, no need to quit
			}
			// error, close the client connection
			CloseConnection();
		}
		
		return data;
	}
	
	
	// ***************************************************************
	//
	// Method: ParseClientData
	//
	// Description: parses and validates client input, producing a 
	//				list of integers to use for calculations based on 
	//				valid input
	//
	// Parameters: String: string input from client
	//
	// Returns: Double ArrayList: array of integers to use in 
	//			calculations
	//
	// **************************************************************
	public ArrayList<Integer> ParseClientData(String data)
	{
		ArrayList<Integer> numList = new ArrayList<Integer>();
		
		try
		{
			// first check that input has comma separation
			if (data.contains(","))
			{
				String intsString[] = data.split(",");
				
				// check if we got two items
				if (intsString.length == 2)
				{
					// try to parse ints
					int first = Integer.parseInt(intsString[0]);
					int second = Integer.parseInt(intsString[1]);
					
					// check that ints meet requirements and build list		
					numList = GenerateList(first, second);
				}
				else
				{
					// not two integers
					SendData("xPlease input two integers.");
				}
			}
			else
			{
				// no commas print error to client
				SendData("xPlease use commas to seperate your integers.\n");
			}
		}
		catch (Exception e)
		{
			if (e instanceof SocketException) 
			{
				System.out.printf("Client on port %d disconnected unexpectedly. Closing the connection fully.\n", clientSocket.getPort());
				// not a critical error, no need to quit
				// close the client connection
				CloseConnection();
			}
			else if (e instanceof NumberFormatException) 
			{
				// error during parsing of integers		
				//System.out.printf("Number Format Error while parsing data from client on port %d.\n", clientSocket.getPort());
				SendData("xPlease input two integers.");
				// not a critical error, no need to quit
			}
			else
			{
				System.err.printf("Unhandled exception while parsing data from client on port %d.\n", clientSocket.getPort());
				e.printStackTrace();
				// not a critical error, no need to quit
				// parse failed send error to client
				SendData("xParse Error, please try again.");
			}
		}
		
		return numList;
	}
		

	// ***************************************************************
	//
	// Method: GenerateList
	//
	// Description: takes in the two parsed ints, validates their values 
	//				and builds and returns the corresponding list of 
	//				prime integers (using threaded tasks).
	//
	// Parameters:  int: first input from client
	//				int: second input from client
	//
	// Returns: Integer ArrayList: array of prime integers to use in 
	//			calculations
	//
	// **************************************************************
	public ArrayList<Integer> GenerateList(int first, int second)
	{
		ArrayList<Integer> numList = new ArrayList<Integer>();
		
		// first check if any integer is less than or equal to0
		if (first <= 0 || second <= 0)
		{
			// output error to client
			SendData("xAll the integers must be greater than zero.");
		}
		// next check if the second input is not greater than the first input
		else if (second <= first)
		{
			// output error to client
			SendData("xThe first integer must be less than the second.");
		}
		else
		{
			// everything checks out, build the list starting at the first 
			// number and ending at the second
			// first, check the list for primes
			ArrayList<Future<Boolean>> primes = new ArrayList<Future<Boolean>>();
			ArrayList<Boolean> resultChecked = new ArrayList<Boolean>();
			for (int i = (int)first ; i <= second; i++)
			{
				// check if the number is prime
				// ask the server to queue this check
				synchronized(server) 
				{
					primes.add(server.QueueTaskExecution(new PrimeTestCallable(i)));
					resultChecked.add(false);
		       }
			}
			
			// add all found primes to numList
			// This logic allows the thread to never be blocked waiting on a result
			int resultsCount = 0;
			//int primeIndex = 0;
			boolean noError = true;
			while (resultsCount < primes.size())
			{
				try
				{
					if (!resultChecked.get(resultsCount) && primes.get(resultsCount).isDone())
					{
						// check if int at index is prime
						if (primes.get(resultsCount).get())
						{
							// int is prime add i to first int given by client to get the true value
							numList.add(resultsCount + first);
						}
						// task complete, got a result
						resultChecked.set(resultsCount, true);
						resultsCount++;
					}
				}
				catch(Exception e)
				{
					if (e instanceof InterruptedException)
					{
						System.err.printf("%s while trying to retrieve prime number for client on port %s.\n",e.toString(),clientSocket.getPort());
						e.printStackTrace();
					}
					else if (e instanceof CancellationException)
					{
						// future was cancelled before we got a result
						System.err.printf("Thread cancellation while trying to retrieve prime number for client on port %s.\n",clientSocket.getPort());
						e.printStackTrace();
					}
					else if (e instanceof ExecutionException)
					{
						// future's thread throw an exception
						// print it
						System.err.printf("%s while trying to retrieve prime number for client on port %s.\n",e.getCause().toString(),clientSocket.getPort());
						e.getCause().printStackTrace();
					}
					// clear the bad prime list
					primes.clear();
					
					// stop looking for primes due to error
					resultsCount = primes.size();
					noError = false;
					// send error to client
					SendData("xError while gathering primes, please try again.");
				}
				
				// increment primeIndex to check next task
				/*primeIndex++;
				if (primeIndex >= primes.size())
					primeIndex = 0;*/
			}
			
			// check if we still have an empty list
			if (numList.size() == 0 && noError)
			{
				// send message to client to try different numbers
				SendData(String.format(
						"xNo prime integers between %d and %d. Please try different integers.\n",
						first,
						second
						));
			}
		}
		return numList;
	}
	

	// ***************************************************************
	//
	// Method: CalculateOutput
	//
	// Description: calculates the sum, mean, and standard deviation of a 
	// 				passed list of integers.
	//
	// Parameters:  Integer ArrayList: array of integers to use in 
	//				calculations
	//
	// Returns: N/A
	//
	// **************************************************************
	public void CalculateOutput(ArrayList<Integer> numList) 
	{		
		try
		{
			double sum = 0;
			double mean = 0;
			double stdev = 0;
			boolean Done = false;
			
			// setup calculation tasks
			Future<Double> result;

			// calculate sum
			synchronized(server) 
			{
				result = server.QueueTaskExecution(new CalculationCallable(numList));
			}

			while (!Done)
			{
				if (result.isDone())
				{
					sum = result.get();
					Done = true;
				}
			}

			// calculate mean
			synchronized(server) 
			{
				result = server.QueueTaskExecution(new CalculationCallable(sum,numList.size()));
			}
			
			Done = false;
			while (!Done)
			{
				if (result.isDone())
				{
					mean = result.get();
					Done = true;
				}
			}

			// calculate standard deviation
			synchronized(server) 
			{
				result = server.QueueTaskExecution(new CalculationCallable(mean,numList));
			}
			
			Done = false;
			while (!Done)
			{
				if (result.isDone())
				{
					stdev = result.get();
					Done = true;
				}
			}

			// send data to client
			PackageData(sum, mean, stdev, numList);
		}
		catch(Exception e)
		{
			if (e instanceof InterruptedException)
			{
				System.err.printf("%s while trying to process prime integer calculatoins for client on port %s.\n", 
						e.toString(),
						clientSocket.getPort());
				e.printStackTrace();
				
			}
			else if (e instanceof CancellationException)
			{
				// future was cancelled before we got a result
				System.err.printf("Thread cancellation while trying to process prime integer calculatoins for client on port %s.\n",
						clientSocket.getPort());
				e.printStackTrace();
			}
			else if (e instanceof ExecutionException)
			{
				// future's thread throw an exception
				// print it
				System.err.printf("%s while trying to process prime integer calculatoins for client on port %s", 
						e.getCause().toString(),
						clientSocket.getPort());
				e.getCause().printStackTrace();
			}
			else
			{
				// future's thread throw an exception
				// print it
				System.err.printf("%s while trying to process prime integer calculatoins for client on port %s", 
						e.toString(),
						clientSocket.getPort());
				e.printStackTrace();
			}
			// Send an error to the user and ask for new input
			SendData("xError while calculating answers. Please try again.");
		}		
	}
	
	
	// ***************************************************************
	//
	// Method: CloseConnection
	//
	// Description: Closes all client streams and the connection to 
	//				the client
	//
	// Parameters: None
	//
	// Returns: N/A
	//
	// **************************************************************
	public void CloseConnection() 
	{
		// if the socket is still open
		if (!clientSocket.isClosed())
		{
			try
			{		
				// print client socket closing
				System.out.printf("\nClient disconnected:\nIP:%s\nPort:%d\n",
						clientSocket.getInetAddress().toString(),
						clientSocket.getPort());
				
				// close client socket data streams
				dataOutStream.close();
				dataInStream.close();
				
				// close connection to client
				clientSocket.close();			
			} 
			catch (Exception e)
			{
				if (e instanceof IOException)
				{
					System.err.printf("IO Error while closing connection to %s on port %d.\n", clientSocket.getInetAddress().toString(), clientSocket.getPort());
					e.printStackTrace();
					// not a critical error, no need to quit
					// don't shut down server, just wait for new client
				}
				else if (e instanceof NullPointerException)
				{
					System.err.printf("Connection not fully closed properly for client at %s on port %d.\n", clientSocket.getInetAddress().toString(), clientSocket.getPort());
					//e.printStackTrace();
					// not a critical error, no need to quit
					// don't shut down server, just wait for new client
				}
				else
				{
					System.err.printf("Unhandled exception while closing connection to %s on port %d.\n", clientSocket.getInetAddress().toString(), clientSocket.getPort());
					e.printStackTrace();
					// not a critical error, no need to quit
					// don't shut down server, just wait for new client
				}
			}
		}
		
		// if this was a "fully connected" client
		if (!disconnect)
		{
			// notify server of client disconnect
			synchronized(server) {
				server.ClientDisconnected();
	       }
		}
	}

}
