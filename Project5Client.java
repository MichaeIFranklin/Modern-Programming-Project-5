//******************************************************************************
//
//  Developer:     Michael Franklin
//
//  Project #:     Project 5
//
//  File Name:     Project5Client.java
//
//  Course:        COSC 4301 - Modern Programming
//
//  Due Date:      03/13/2022
//
//  Instructor:    Fred Kumi 
//
//  Description:   Client side of Project 5.
//
//
//******************************************************************************

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Project5Client {
	private final String serverAddress = "127.0.0.1";
	private final int serverPort = 4301;
	private final String closeCommand = "Bye";
	private final String packageDelimiter = "~"; // delimiter string to show there is no more data from the server
	private final String connectionToken = "y"; // token to look for to check for full connection to the server
	private final int primesPerLine = 10; // number of primes to output per line
    
	private DataOutputStream dataOutStream;
	private DataInputStream dataInStream;
	private Scanner inputStream;
	private Socket socket;
	private boolean connected;
	
	// ***************************************************************
	//
	// Method: main
	//
	// Description: The main method of the client
	//
	// Parameters: String array
	//
	// Returns: N/A
	//
	// **************************************************************
	public static void main(String argvs[])
	{
		// create class object
		Project5Client client = new Project5Client();
		client.developerInfo();
		
		// setup client and connect to server
		client.Setup();
		
		// if we successfully connected to the server
		if (client.connected)
		{
			// main loop of client
			String input = "";
			while (!input.equals(client.closeCommand)) 
			{
				// get user input
				input = client.GetUserInput();
				
				// send input to server
				client.SendData(input);
				
				// wait for and output response from server
				client.GetResponse();
			}
		}
		
		// close connection to server
		client.CloseConnection();
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
			// not fully connected
			connected = false;
			
			// try to connect to the server
			socket = new Socket(serverAddress, serverPort);
			
			// connection successful	
			// open input stream from the server
			dataInStream = new DataInputStream(socket.getInputStream());
			
			// await connection response from server
			String connectResponse = dataInStream.readUTF();
			
			// check for connection successful response
			if (connectResponse.equals(connectionToken))
			{
				// fully connected
				connected = true;
				// open output stream to the server
				dataOutStream = new DataOutputStream(socket.getOutputStream());
				
				// setup scanner to get user input
				inputStream = new Scanner(System.in);
				
				// print setup and connection successful
				System.out.println("Connected to Server");
			}
			else
			{
				// print out the message from the server
				System.out.println(connectResponse.substring(1));
			}
		} 
		catch (Exception e) 
		{
			if (e instanceof UnknownHostException) 
			{
				System.err.printf("Host Not Found at %s on port %d.\nPlease check that the server is running and try again.\n", serverAddress,
						serverPort);
				System.exit(0);
			} 
			else if (e instanceof ConnectException) 
			{
				System.err.printf("Could not connect to %s on port %d.\nPlease check that the server is running and try again.\n", serverAddress, serverPort);
				//e.printStackTrace();
				System.exit(0);
			}
			else if (e instanceof IOException) 
			{
				System.err.printf("IO Error while connecting to %s on port %d.\n", serverAddress, serverPort);
				e.printStackTrace();
				System.exit(0);
			}
			else
			{
				System.err.printf("Unhandled exception while connecting to %s on port %d.\n", serverAddress, serverPort);
				e.printStackTrace();
				System.exit(0);
			}
		}
	}

	
	// ***************************************************************
	//
	// Method: GetUserInput
	//
	// Description: prompts the user for input and returns user input
	//
	// Parameters: None
	//
	// Returns: String: input from user
	//
	// **************************************************************
	public String GetUserInput()
	{
		System.out.println(
				"To calcuate the sum, mean and standard deviation of the prime numbers in an integer list,\nplease enter 2 seperate integers following these rules:\n"
				+ " * Integers must be seperated by a comma.\n"
				+ " * Integers must be greater than 0.\n"
				+ " * The second integer must be larger than the first.\n");
		
		String input = "";
		while(input.equals(""))
		{
			input = inputStream.nextLine();
		}
		
		return input;
	}
	
	
	// ***************************************************************
	//
	// Method: SendData
	//
	// Description: Sends the passed string to the server.
	//
	// Parameters: String: data to be sent to the server
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
		catch (Exception e)
		{
			if (e instanceof SocketException) 
			{
				System.err.printf("Server on port %d closed the connection unexpectedly.\nPlease check the server and try again.", serverPort);
				System.exit(0);
			}
			else if (e instanceof IOException) 
			{
				System.err.printf("IO Error while sending data to %s on port %d.", serverAddress, serverPort);
				e.printStackTrace();
				System.exit(0);
			}
		}
	}

	
	// ***************************************************************
	//
	// Method: GetResponse
	//
	// Description: Gets the response from the server and outputs it 
	//			    to the user
	//
	// Parameters: None
	//
	// Returns: N/A
	//
	// **************************************************************
	public void GetResponse() 
	{
		// get data chunks from server
		boolean Done = false;
		String data = "";
		
		// persistent number data between response data chunks
		double sum = 0;
		double mean = 0;
		double stdev = 0; 
		int slot = 0; // persists current output column of prime int
		
		// while there is still data to read
		while (!Done)
		{
			try
			{
				data = dataInStream.readUTF();
			}
			catch (Exception e) 
			{
				if (e instanceof SocketException) 
				{
					System.err.printf("Server on port %d closed the connection unexpectedly.\nPlease check the server and try again.", serverPort);
					System.exit(0);
				}
				else if (e instanceof IOException) 
				{
					System.err.printf("IO Error while reading data from server at %s on port %d.\n", serverAddress, serverPort);
					e.printStackTrace();
					System.exit(0);
				}
				else if (e instanceof EOFException) 
				{
					System.err.printf("End of Field Error while reading data from server at %s on port %d.\n", serverAddress, serverPort);
					e.printStackTrace();
					System.exit(0);
				}
				else
				{
					System.err.printf("Unhandled exception while reading data from server at %s on port %d.\n", serverAddress, serverPort);
					e.printStackTrace();
					System.exit(0);
				}
			}
			
			// output data to user
			// check if data is a message or integer data
			if (data.startsWith("x"))
			{
				// server message, trim leading x and print
				System.out.println(data.substring(1));
				
				// no more data
				Done = true;
			}
			else
			{
				// number data chunk (calculation answers), parse and print
				// check for data chunk position
				String outputStrings[] = data.split(",");
				// if we are at the end of the data chunk
				if (data.contains(packageDelimiter))
				{
					// parse final chunk and there is no more data to read
					Done = true;
				}
				
				// check if we are at the beginning
				if (outputStrings.length == 3 && !data.contains(packageDelimiter))
				{
					// parse calculated answers
					sum = Double.parseDouble(outputStrings[0]);
					mean = Double.parseDouble(outputStrings[1]);
					stdev = Double.parseDouble(outputStrings[2]);
					
					// output header for user output
					System.out.println("\nPrimes Found:");
				}
				else
				{
					int i = 0;
					// print 10 primes per line
					while(slot < primesPerLine - 1 && i < outputStrings.length)
					{
						if (i < outputStrings.length)
						{
							if (!outputStrings[i].equals(packageDelimiter))
								
								System.out.printf("%-5s ",outputStrings[i]);
							i++;
							slot++;
							if (slot >= primesPerLine - 1)
							{
								System.out.println();
								slot = 0;
							}
								
						}			
					}
				}
				
				// if that was the last chunk show the final results
				if (Done)
				{
					// output answers
					System.out.printf(
							"\n\nCalculated Results:\n\n"
							+ "Sum: %.0f\n"
							+ "Mean: %.3f\n"
							+ "Standard Deviation: %.3f\n\n",
							sum, mean, stdev);
				}
			}
		}
	}

	
	// ***************************************************************
	//
	// Method: CloseConnection
	//
	// Description: Closes all streams and the connection to the 
	//			    server
	//
	// Parameters: None
	//
	// Returns: N/A
	//
	// **************************************************************
	public void CloseConnection() 
	{
		try
		{
			// close user input stream
			if (inputStream != null)
				inputStream.close();
			
			// close socket data streams
			if (dataOutStream != null)
				dataOutStream.close();
			if (dataInStream != null)
				dataInStream.close();
			
			// close connection to server
			if (socket != null)
				socket.close();
		} 
		catch (Exception e)
		{
			if (e instanceof IOException)
			{
				System.err.printf("IO Error while closing connection to %s on port %d.\n", serverAddress, serverPort);
				e.printStackTrace();
				System.exit(0);
			}
			else if (e instanceof NullPointerException)
			{
				System.err.printf("Connection not fully closed properly to %s on port %d.\n", serverAddress, serverPort);
				//e.printStackTrace();
				System.exit(0);
			}
			else
			{
				System.err.printf("Unhandled exception while closing connection to %s on port %d.\n", serverAddress, serverPort);
				e.printStackTrace();
				System.exit(0);
			}
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